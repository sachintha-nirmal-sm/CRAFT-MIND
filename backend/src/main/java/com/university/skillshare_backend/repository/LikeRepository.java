package com.university.skillshare_backend.repository;

import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import com.university.skillshare_backend.model.Like;

@Repository
public interface LikeRepository extends MongoRepository<Like, String> {
    Like findByPostIdAndUserId(String postId, String userId);
    void deleteByPostIdAndUserId(String postId, String userId);
    long countByPostId(String postId);
}
