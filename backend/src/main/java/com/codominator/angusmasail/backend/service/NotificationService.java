package com.codominator.angusmasail.backend.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
public class NotificationService {
    private static final Logger logger = LoggerFactory.getLogger(NotificationService.class);

    public void sendPushNotification(String fcmToken, String title, String body) {
        if (fcmToken == null || fcmToken.isBlank()) {
            return;
        }
        logger.info("FCM Notification -> Token: {}, Title: {}, Body: {}", fcmToken, title, body);
    }

    public void notifyImamFollowers(UUID imamProfileId, String title, String body) {
        logger.info("FCM Broadcast to followers of ImamProfile {}: [{}] {}", imamProfileId, title, body);
    }

    public void notifyUser(UUID userId, String title, String body) {
        logger.info("Notification to User [{}]: [{}] {}", userId, title, body);
    }
}
