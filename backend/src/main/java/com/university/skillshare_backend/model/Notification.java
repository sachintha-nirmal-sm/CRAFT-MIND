package com.university.skillshare_backend.model;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import java.time.LocalDateTime;
import java.util.Map;
import lombok.Data;

import java.util.Date;

@Data
@Document(collection = "notifications")
public class Notification {
    
    public enum NotificationType {
        MENTION, 
        COMMENT, 
        LIKE, 
        FOLLOW, 
        SYSTEM,
        GROUP_INVITE,
        GROUP_JOIN_REQUEST,
        GROUP_POST,
        GROUP_APPROVED
    }
    
    @Id
    private String id;
    
    private String userId;
    
    private String title;
    
    private String message;
    
    private NotificationType type;
    
    private String relatedItemId; // Id of related post, comment, etc.
    
    private boolean read;
    
    private Date createdAt;
    
    private Map<String, String> metadata;
    
    // Constructors
    public Notification() {
        this.read = false;
        this.createdAt = new Date();
    }
    
    public Notification(String userId, String title, String message, NotificationType type) {
        this();
        this.userId = userId;
        this.title = title;
        this.message = message;
        this.type = type;
    }
    
    public Notification(String userId, String title, String message, NotificationType type, String relatedItemId) {
        this(userId, title, message, type);
        this.relatedItemId = relatedItemId;
    }
    
    // Getters and setters
    public String getId() {
        return id;
    }
    
    public void setId(String id) {
        this.id = id;
    }
    
    public String getUserId() {
        return userId;
    }
    
    public void setUserId(String userId) {
        this.userId = userId;
    }
    
    public String getTitle() {
        return title;
    }
    
    public void setTitle(String title) {
        this.title = title;
    }
    
    public String getMessage() {
        return message;
    }
    
    public void setMessage(String message) {
        this.message = message;
    }
    
    public NotificationType getType() {
        return type;
    }
    
    public void setType(NotificationType type) {
        this.type = type;
    }
    
    public String getRelatedItemId() {
        return relatedItemId;
    }
    
    public void setRelatedItemId(String relatedItemId) {
        this.relatedItemId = relatedItemId;
    }
    
    public boolean isRead() {
        return read;
    }
    
    public void setRead(boolean read) {
        this.read = read;
    }
    
    public Date getCreatedAt() {
        return createdAt;
    }
    
    public void setCreatedAt(Date createdAt) {
        this.createdAt = createdAt;
    }
    
    public Map<String, String> getMetadata() {
        return metadata;
    }
    
    public void setMetadata(Map<String, String> metadata) {
        this.metadata = metadata;
    }
}
