package com.university.skillshare_backend.repository;

import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;
import com.university.skillshare_backend.model.GroupMembership;
import java.util.List;

@Repository
public interface GroupMembershipRepository extends MongoRepository<GroupMembership, String> {
    List<GroupMembership> findByUserId(String userId);
    List<GroupMembership> findByGroupId(String groupId);
    List<GroupMembership> findByGroupIdAndUserId(String groupId, String userId);
    boolean existsByGroupIdAndUserId(String groupId, String userId);
    void deleteByGroupIdAndUserId(String groupId, String userId);
}
