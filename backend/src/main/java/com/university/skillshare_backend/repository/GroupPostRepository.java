package com.university.skillshare_backend.repository;

import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;
import com.university.skillshare_backend.model.GroupPost;
import java.util.List;

@Repository
public interface GroupPostRepository extends MongoRepository<GroupPost, String> {
    List<GroupPost> findByGroupId(String groupId);
    List<GroupPost> findByPostId(String postId);
    List<GroupPost> findByGroupIdAndPostId(String groupId, String postId);
    boolean existsByGroupIdAndPostId(String groupId, String postId);
}
