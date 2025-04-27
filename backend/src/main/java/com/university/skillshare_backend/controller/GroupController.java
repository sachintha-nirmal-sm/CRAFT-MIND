package com.university.skillshare_backend.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import com.university.skillshare_backend.model.Group;
import com.university.skillshare_backend.model.GroupMembership;
import com.university.skillshare_backend.model.GroupPost;
import com.university.skillshare_backend.model.Post;
import com.university.skillshare_backend.model.User;
import com.university.skillshare_backend.service.GroupService;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/groups")
@CrossOrigin(origins = "*")
public class GroupController {
    
    private final GroupService groupService;
    
    @Autowired
    public GroupController(GroupService groupService) {
        this.groupService = groupService;
    }
    
    @GetMapping
    public ResponseEntity<List<Group>> getAllGroups() {
        return ResponseEntity.ok(groupService.getAllGroups());
    }
    
    @PostMapping
    public ResponseEntity<Group> createGroup(@RequestBody Group group) {
        return ResponseEntity.ok(groupService.createGroup(group));
    }
    
    @GetMapping("/{groupId}")
    public ResponseEntity<Group> getGroupById(@PathVariable String groupId) {
        return ResponseEntity.ok(groupService.getGroupById(groupId));
    }
    
    @GetMapping("/user/{userId}")
    public ResponseEntity<List<Group>> getGroupsByOwnerId(@PathVariable String userId) {
        return ResponseEntity.ok(groupService.getGroupsByOwnerId(userId));
    }

    @GetMapping("/user/{userId}/joined")
    public ResponseEntity<List<Group>> getUserJoinedGroups(@PathVariable String userId) {
        return ResponseEntity.ok(groupService.getGroupsByMemberId(userId));
    }
    
    @GetMapping("/user/{userId}/all")
    public ResponseEntity<List<Group>> getAllUserGroups(@PathVariable String userId) {
        return ResponseEntity.ok(groupService.getAllUserGroups(userId));
    }

    @PostMapping("/{groupId}/join")
    public ResponseEntity<?> joinGroup(@PathVariable String groupId, @RequestParam String userId) {
        try {
            GroupMembership membership = groupService.joinGroup(groupId, userId);
            return ResponseEntity.ok(membership);
        } catch (IllegalStateException e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                               .body(Map.of("message", "Failed to join group"));
        }
    }

    @DeleteMapping("/{groupId}/leave")
    public ResponseEntity<?> leaveGroup(@PathVariable String groupId, @RequestParam String userId) {
        try {
            groupService.leaveGroup(groupId, userId);
            return ResponseEntity.ok(Map.of("message", "Successfully left the group"));
        } catch (IllegalStateException e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                               .body(Map.of("message", "Failed to leave group: " + e.getMessage()));
        }
    }

    @GetMapping("/user/{userId}/memberships")
    public ResponseEntity<List<GroupMembership>> getUserMemberships(@PathVariable String userId) {
        return ResponseEntity.ok(groupService.getUserMemberships(userId));
    }

    @PostMapping("/{groupId}/posts")
    public ResponseEntity<?> sharePost(
            @PathVariable String groupId,
            @RequestBody Map<String, String> body,
            @RequestParam String userId) {
        try {
            String postId = body.get("postId");
            GroupPost groupPost = groupService.sharePost(groupId, postId, userId);
            return ResponseEntity.ok(groupPost);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                               .body(Map.of("message", "Failed to share post: " + e.getMessage()));
        }
    }

    @GetMapping("/{groupId}/posts")
    public ResponseEntity<List<Post>> getGroupPosts(@PathVariable String groupId) {
        try {
            List<Post> posts = groupService.getGroupPosts(groupId);
            return ResponseEntity.ok(posts);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                   .body(null);
        }
    }
    
    @PutMapping("/{groupId}")
    public ResponseEntity<?> updateGroup(
            @PathVariable String groupId,
            @RequestBody Group updatedGroup,
            @RequestParam String userId) {
        try {
            Group group = groupService.updateGroup(groupId, updatedGroup, userId);
            return ResponseEntity.ok(group);
        } catch (IllegalStateException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                   .body(Map.of("message", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                   .body(Map.of("message", "Failed to update group: " + e.getMessage()));
        }
    }
    
    @DeleteMapping("/{groupId}")
    public ResponseEntity<?> deleteGroup(
            @PathVariable String groupId,
            @RequestParam String userId) {
        try {
            groupService.deleteGroup(groupId, userId);
            return ResponseEntity.ok(Map.of("message", "Group deleted successfully"));
        } catch (IllegalStateException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                   .body(Map.of("message", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                   .body(Map.of("message", "Failed to delete group: " + e.getMessage()));
        }
    }
    
    @DeleteMapping("/{groupId}/posts/{postId}")
    public ResponseEntity<?> removeGroupPost(
            @PathVariable String groupId,
            @PathVariable String postId,
            @RequestParam String userId) {
        try {
            groupService.removeGroupPost(groupId, postId, userId);
            return ResponseEntity.ok(Map.of("message", "Post removed from group"));
        } catch (IllegalStateException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                   .body(Map.of("message", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                   .body(Map.of("message", "Failed to remove post: " + e.getMessage()));
        }
    }
    
    @DeleteMapping("/{groupId}/members/{memberId}")
    public ResponseEntity<?> removeGroupMember(
            @PathVariable String groupId,
            @PathVariable String memberId,
            @RequestParam String userId) {
        try {
            groupService.removeGroupMember(groupId, memberId, userId);
            return ResponseEntity.ok(Map.of("message", "Member removed from group"));
        } catch (IllegalStateException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                   .body(Map.of("message", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                   .body(Map.of("message", "Failed to remove member: " + e.getMessage()));
        }
    }

    @GetMapping("/{groupId}/members")
    public ResponseEntity<List<User>> getGroupMembers(@PathVariable String groupId) {
        List<User> members = groupService.getGroupMembers(groupId);
        return ResponseEntity.ok(members);
    }
}
