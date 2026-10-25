package com.codominator.angusmasail.backend.config;

import com.codominator.angusmasail.backend.entity.ChatRequest;
import com.codominator.angusmasail.backend.entity.ChatRequestStatus;
import com.codominator.angusmasail.backend.repository.ChatRequestRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.UUID;

@Component
public class WebSocketSecurityInterceptor implements ChannelInterceptor {

    @Autowired
    private JwtUtils jwtUtils;

    @Autowired
    private ChatRequestRepository chatRequestRepository;

    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {
        StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);

        if (accessor != null && StompCommand.CONNECT.equals(accessor.getCommand())) {
            String token = accessor.getFirstNativeHeader("Authorization");
            if (token != null && token.startsWith("Bearer ")) {
                token = token.substring(7);
                if (jwtUtils.validateJwtToken(token)) {
                    String userId = jwtUtils.getUserIdFromToken(token);
                    accessor.setUser(new UsernamePasswordAuthenticationToken(userId, null, null));
                }
            }
        }

        if (accessor != null && (StompCommand.SUBSCRIBE.equals(accessor.getCommand()) || StompCommand.SEND.equals(accessor.getCommand()))) {
            String destination = accessor.getDestination();
            if (destination != null && destination.startsWith("/topic/chat/")) {
                try {
                    String[] parts = destination.split("/");
                    UUID chatRequestId = UUID.fromString(parts[parts.length - 1]);

                    Optional<ChatRequest> req = chatRequestRepository.findById(chatRequestId);
                    if (req.isEmpty() || req.get().getStatus() != ChatRequestStatus.ACCEPTED) {
                        throw new IllegalArgumentException("Chat request is not in ACCEPTED state.");
                    }
                } catch (Exception e) {
                    throw new IllegalArgumentException("Unauthorized access to chat room.");
                }
            }
        }

        return message;
    }
}
