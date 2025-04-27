package com.university.skillshare_backend.model;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import lombok.Data;
import java.util.Date;

@Data
@Document(collection = "group_memberships")
public class GroupMembership {
    @Id
    private String id;
    private String groupId;
    private String userId;
    private Role role;
    private Date joinedAt;
    
    public enum Role {
        MEMBER,
        MODERATOR,
        ADMIN
    }
    
    public GroupMembership() {
        this.joinedAt = new Date();
        this.role = Role.MEMBER;
    }
}
