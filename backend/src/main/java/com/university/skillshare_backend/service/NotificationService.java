package com.university.skillshare_backend.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import com.university.skillshare_backend.model.Notification;
import com.university.skillshare_backend.model.Notification.NotificationType;
import com.university.skillshare_backend.repository.NotificationRepository;

import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.Map;
import java.util.HashMap;

@Service
public class NotificationService {
    
    private final NotificationRepository notificationRepository;
    private final SimpMessagingTemplate messagingTemplate;
    
    @Autowired
    public NotificationService(
            NotificationRepository notificationRepository,
            SimpMessagingTemplate messagingTemplate) {
        this.notificationRepository = notificationRepository;
        this.messagingTemplate = messagingTemplate;
    }
    
    /**
     * Get all notifications for a user
     */
    public List<Notification> getUserNotifications(String userId) {
        return notificationRepository.findByUserId(userId);
    }
    
    /**
     * Get unread notifications for a user
     */
    public List<Notification> getUnreadNotifications(String userId) {
        return notificationRepository.findByUserIdAndReadIsFalse(userId);
    }
    
    /**
     * Count unread notifications
     */
    public long countUnreadNotifications(String userId) {
        return notificationRepository.countByUserIdAndReadIsFalse(userId);
    }
    
    /**
     * Mark a notification as read
     */
    public Notification markAsRead(String notificationId) {
        Optional<Notification> optNotification = notificationRepository.findById(notificationId);
        if (optNotification.isPresent()) {
            Notification notification = optNotification.get();
            notification.setRead(true);
            return notificationRepository.save(notification);
        }
        return null;
    }
    
    /**
     * Mark a notification as unread
     */
    public Notification markAsUnread(String notificationId) {
        Optional<Notification> optNotification = notificationRepository.findById(notificationId);
        if (optNotification.isPresent()) {
            Notification notification = optNotification.get();
            notification.setRead(false);
            return notificationRepository.save(notification);
        }
        return null;
    }
    
    /**
     * Mark all user notifications as read
     */
    public void markAllAsRead(String userId) {
        List<Notification> notifications = notificationRepository.findByUserIdAndReadIsFalse(userId);
        for (Notification notification : notifications) {
            notification.setRead(true);
        }
        notificationRepository.saveAll(notifications);
    }
    
    /**
     * Delete a notification
     */
    public void deleteNotification(String notificationId) {
        notificationRepository.deleteById(notificationId);
    }
    
    /**
     * Create a new notification
     */
    public Notification createNotification(String userId, String title, String message, 
                                          NotificationType type, String relatedItemId) {
        Notification notification = new Notification(userId, title, message, type, relatedItemId);
        notification = notificationRepository.save(notification);
        
        // Send real-time notification via WebSocket
        messagingTemplate.convertAndSendToUser(
            userId, 
            "/topic/notifications", 
            notification
        );
        
        return notification;
    }
    
    /**
     * Create a system notification
     */
    public Notification createSystemNotification(String userId, String title, String message) {
        return createNotification(userId, title, message, NotificationType.SYSTEM, null);
    }
    
    /**
     * Create a mention notification
     */
    public Notification createMentionNotification(String userId, String mentionerName, String postId) {
        String title = "New Mention";
        String message = mentionerName + " mentioned you in a comment";
        return createNotification(userId, title, message, NotificationType.MENTION, postId);
    }
    
    /**
     * Create a comment notification
     */
    public Notification createCommentNotification(String userId, String commenterName, String postId) {
        String title = "New Comment on Your Post";
        String message = commenterName + " commented on your post";
        return createNotification(userId, title, message, NotificationType.COMMENT, postId);
    }
    
    /**
     * Create a like notification
     */
    public Notification createLikeNotification(String userId, String likerName, String postId) {
        String title = "New Like";
        String message = likerName + " liked your post";
        Notification notification = createNotification(userId, title, message, NotificationType.LIKE, postId);
        notification.setMetadata(new HashMap<>());
        return notification;
    }

    public Notification createCommentNotification(String userId, String commenterName, String postId, int commentPosition) {
        String title = "New Comment on Your Post";
        String message = commenterName + " commented on your post";
        Notification notification = createNotification(userId, title, message, NotificationType.COMMENT, postId);
        
        Map<String, String> metadata = new HashMap<>();
        metadata.put("commentPosition", String.valueOf(commentPosition));
        notification.setMetadata(metadata);
        
        return notification;
    }

    public Notification createMentionNotification(String userId, String mentionerName, String postId, int commentPosition) {
        String title = "New Mention";
        String message = mentionerName + " mentioned you in a comment";
        Notification notification = createNotification(userId, title, message, NotificationType.MENTION, postId);
        notification.setMetadata(Map.of("commentPosition", String.valueOf(commentPosition)));
        return notification;
    }
}
