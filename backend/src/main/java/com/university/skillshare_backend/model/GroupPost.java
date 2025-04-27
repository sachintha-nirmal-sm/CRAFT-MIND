package com.university.skillshare_backend.model;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import lombok.Data;
import java.util.Date;

@Data
@Document(collection = "group_posts")
public class GroupPost {
    @Id
    private String id;
    private String groupId;
    private String postId;
    private String sharedById;
    private Date sharedAt;
    
    public GroupPost() {
        this.sharedAt = new Date();
    }
}
