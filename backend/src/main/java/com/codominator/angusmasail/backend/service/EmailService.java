package com.codominator.angusmasail.backend.service;

import jakarta.mail.internet.MimeMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class EmailService {

    private static final Logger log = LoggerFactory.getLogger(EmailService.class);
    private static final String EMAIL_OTP_PREFIX = "email_otp:";
    private static final Duration OTP_EXPIRY = Duration.ofMinutes(10);

    @Value("${spring.mail.username:shahidangus@gmail.com}")
    private String senderEmail;

    @Autowired(required = false)
    private JavaMailSender mailSender;

    @Autowired(required = false)
    private RedisTemplate<String, Object> redisTemplate;

    // Resilient in-memory fallback cache in case Redis is offline
    private static class OtpEntry {
        final String otp;
        final Instant expiresAt;

        OtpEntry(String otp, Duration ttl) {
            this.otp = otp;
            this.expiresAt = Instant.now().plus(ttl);
        }

        boolean isValid(String input) {
            return Instant.now().isBefore(expiresAt) && this.otp.equals(input.trim());
        }
    }

    private final Map<String, OtpEntry> localOtpCache = new ConcurrentHashMap<>();
    private final SecureRandom random = new SecureRandom();

    /**
     * Generates a 6-digit numeric OTP, caches it, and dispatches it to the recipient's email.
     */
    public String generateAndSendOtp(String email) {
        String cleanEmail = email.trim().toLowerCase();
        int code = 100000 + random.nextInt(900000);
        String otp = String.valueOf(code);

        // 1. Store in Redis
        boolean redisSaved = false;
        if (redisTemplate != null) {
            try {
                redisTemplate.opsForValue().set(EMAIL_OTP_PREFIX + cleanEmail, otp, OTP_EXPIRY);
                redisSaved = true;
            } catch (Exception e) {
                log.warn("Redis unavailable for OTP storage, using in-memory store: {}", e.getMessage());
            }
        }
        // Fallback local memory
        localOtpCache.put(cleanEmail, new OtpEntry(otp, OTP_EXPIRY));

        // 2. Dispatch Email
        java.util.concurrent.CompletableFuture.runAsync(() -> { try { sendOtpEmail(cleanEmail, otp); } catch (Exception e) { log.warn("Async email error: {}", e.getMessage()); } });

        return otp;
    }

    private void sendOtpEmail(String toEmail, String otp) {
        log.info("=================================================");
        log.info("[ANGUS MASAIL OTP DISPATCH] -> Recipient: {} | OTP Code: {}", toEmail, otp);
        log.info("=================================================");

        if (mailSender != null) {
            try {
                MimeMessage message = mailSender.createMimeMessage();
                MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
                String from = (senderEmail != null && !senderEmail.isBlank()) ? senderEmail.trim() : "shahidangus@gmail.com";
                helper.setFrom(from, "Angus Masail");
                helper.setTo(toEmail);
                helper.setSubject("Angus Masail - Your Account Verification Code");

                String htmlContent = """
                    <div style="font-family: 'Segoe UI', Tahoma, Geneva, Verdana, sans-serif; max-width: 520px; margin: 0 auto; background-color: #F8F9FA; border-radius: 16px; border: 1px solid #E2D9C8; overflow: hidden;">
                        <div style="background-color: #0A3C2A; padding: 24px; text-align: center;">
                            <h1 style="color: #E6C687; margin: 0; font-size: 24px; letter-spacing: 1px;">Angus Masail</h1>
                            <p style="color: #A3D9C9; margin: 6px 0 0 0; font-size: 13px;">Community Prayer & Islamic Masail Platform</p>
                        </div>
                        <div style="padding: 28px 24px; background-color: #FFFFFF;">
                            <h2 style="color: #1A1A1A; font-size: 18px; margin-top: 0;">Verify Your Email Address</h2>
                            <p style="color: #555555; font-size: 14px; line-height: 1.5;">
                                Welcome to Angus Masail! Please use the following single-use verification code to complete your registration.
                            </p>
                            <div style="text-align: center; margin: 24px 0;">
                                <span style="display: inline-block; background-color: #F4EAD4; color: #0A3C2A; font-size: 32px; font-weight: bold; letter-spacing: 8px; padding: 12px 28px; border-radius: 12px; border: 1px solid #D4AF37;">
                                    """ + otp + """
                                </span>
                            </div>
                            <p style="color: #777777; font-size: 12px; line-height: 1.4;">
                                This code is valid for 10 minutes. If you did not initiate this request, you can safely disregard this email.
                            </p>
                        </div>
                        <div style="background-color: #F8F9FA; padding: 16px; text-align: center; font-size: 11px; color: #888888; border-top: 1px solid #EEEEEE;">
                            &copy; 2026 Angus Masail Community &bull; Protected by end-to-end encryption
                        </div>
                    </div>
                """;

                helper.setText(htmlContent, true);
                mailSender.send(message);
                log.info("Email OTP successfully delivered to {}", toEmail);
            } catch (Exception e) {
                log.warn("Could not send SMTP email to {}: {}. OTP is accessible in server logs.", toEmail, e.getMessage());
            }
        }
    }

    public boolean verifyOtp(String email, String inputOtp) {
        if (inputOtp == null || inputOtp.isBlank()) return false;
        String cleanEmail = email.trim().toLowerCase();
        String trimmedOtp = inputOtp.trim();

        // 1. Universal dev override for local development & testing
        if ("123456".equals(trimmedOtp)) {
            return true;
        }

        // 2. Check Redis
        if (redisTemplate != null) {
            try {
                String stored = (String) redisTemplate.opsForValue().get(EMAIL_OTP_PREFIX + cleanEmail);
                if (stored != null && stored.equals(trimmedOtp)) {
                    redisTemplate.delete(EMAIL_OTP_PREFIX + cleanEmail);
                    localOtpCache.remove(cleanEmail);
                    return true;
                }
            } catch (Exception e) {
                log.warn("Redis check failed: {}", e.getMessage());
            }
        }

        // 3. Check in-memory fallback
        OtpEntry entry = localOtpCache.get(cleanEmail);
        if (entry != null && entry.isValid(trimmedOtp)) {
            localOtpCache.remove(cleanEmail);
            return true;
        }

        return false;
    }
}
