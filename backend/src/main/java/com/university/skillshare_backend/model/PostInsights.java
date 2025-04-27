package com.university.skillshare_backend.model;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import lombok.Data;

@Data
@Document(collection = "post_insights")
public class PostInsights {
    @Id
    private String id;
    private String postId;
    private int views;
    private int uniqueViewers;
    private int likeCount;
    private int commentCount;
    private int shareCount;
    private double engagementRate;

    public PostInsights(String postId) {
        this.postId = postId;
        this.views = 0;
        this.uniqueViewers = 0;
        this.likeCount = 0;
        this.commentCount = 0;
        this.shareCount = 0;
        this.engagementRate = 0.0;
    }
}
