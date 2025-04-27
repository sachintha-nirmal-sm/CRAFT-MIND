package com.university.skillshare_backend.model;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import lombok.Data;
import java.util.Date;

@Data
@Document(collection = "likes")
public class Like {
    @Id
    private String id;
    private String postId;
    private String userId;
    private Date createdAt;

    public Like(String postId, String userId) {
        this.postId = postId;
        this.userId = userId;
        this.createdAt = new Date();
    }
}
