package com.university.skillshare_backend.repository;

import java.util.Optional;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;
import com.university.skillshare_backend.model.PostInsights;

@Repository
public interface PostInsightsRepository extends MongoRepository<PostInsights, String> {
    Optional<PostInsights> findByPostId(String postId);
    void deleteByPostId(String postId);
}
