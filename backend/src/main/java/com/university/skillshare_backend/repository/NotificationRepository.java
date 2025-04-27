package com.university.skillshare_backend.repository;

import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import com.university.skillshare_backend.model.Notification;

import java.util.List;

@Repository
public interface NotificationRepository extends MongoRepository<Notification, String> {
    
    // Find notifications for a specific user
    List<Notification> findByUserId(String userId);
    
    // Find unread notifications for a specific user
    List<Notification> findByUserIdAndReadIsFalse(String userId);
    
    // Count unread notifications for a specific user
    long countByUserIdAndReadIsFalse(String userId);
}
