package com.university.skillshare_backend.model;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import lombok.Data;
import java.util.Date;

@Data
@Document(collection = "groups")
public class Group {
    @Id
    private String id;
    private String name;
    private String description;
    private String ownerId;
    private Date createdAt;
    private String coverImage;
    private boolean isPublic;  // Ensure this property name is correct
    
    public Group() {
        this.createdAt = new Date();
        this.isPublic = true;  // Default to public
    }
}
