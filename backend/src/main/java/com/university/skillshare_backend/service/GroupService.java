package com.university.skillshare_backend.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import com.university.skillshare_backend.model.Group;
import com.university.skillshare_backend.model.GroupMembership;
import com.university.skillshare_backend.model.Notification.NotificationType;
import com.university.skillshare_backend.model.User;
import com.university.skillshare_backend.repository.GroupRepository;
import com.university.skillshare_backend.repository.GroupMembershipRepository;
import com.university.skillshare_backend.repository.UserRepository;
import com.university.skillshare_backend.exception.ResourceNotFoundException;
import java.util.List;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;
import com.university.skillshare_backend.model.GroupPost;
import com.university.skillshare_backend.repository.GroupPostRepository;
import com.university.skillshare_backend.repository.PostRepository;
import com.university.skillshare_backend.model.Post;

@Service
public class GroupService {
    
    private final GroupRepository groupRepository;
    private final GroupMembershipRepository membershipRepository;
    private final NotificationService notificationService;
    private final GroupPostRepository groupPostRepository;
    private final PostRepository postRepository;
    private final UserRepository userRepository;

    @Autowired
    public GroupService(GroupRepository groupRepository, 
                       GroupMembershipRepository membershipRepository,
                       NotificationService notificationService,
                       GroupPostRepository groupPostRepository,
                       PostRepository postRepository,
                       UserRepository userRepository) {
        this.groupRepository = groupRepository;
        this.membershipRepository = membershipRepository;
        this.notificationService = notificationService;
        this.groupPostRepository = groupPostRepository;
        this.postRepository = postRepository;
        this.userRepository = userRepository;
    }

    public List<Group> getAllGroups() {
        return groupRepository.findAll();
    }

    public Group createGroup(Group group) {
        Group savedGroup = groupRepository.save(group);
        
        // Send notification to system about new group creation
        notificationService.createSystemNotification(
            group.getOwnerId(),
            "Group Created",
            "You have successfully created the group: " + group.getName()
        );
        
        return savedGroup;
    }

    public Group getGroupById(String groupId) {
        return groupRepository.findById(groupId)
            .orElseThrow(() -> new ResourceNotFoundException("Group", "id", groupId));
    }

    public List<Group> getGroupsByOwnerId(String ownerId) {
        return groupRepository.findByOwnerId(ownerId);
    }

    public GroupMembership joinGroup(String groupId, String userId) {
        Group group = getGroupById(groupId);
        
        if (membershipRepository.existsByGroupIdAndUserId(groupId, userId)) {
            throw new IllegalStateException("User is already a member of this group");
        }

        GroupMembership membership = new GroupMembership();
        membership.setGroupId(groupId);
        membership.setUserId(userId);
        membership.setRole(GroupMembership.Role.MEMBER);
        
        GroupMembership savedMembership = membershipRepository.save(membership);

        // Notify group owner
        notificationService.createNotification(
            group.getOwnerId(),
            "New Group Member",
            "A new user has joined your group: " + group.getName(),
            NotificationType.GROUP_JOIN_REQUEST,
            groupId
        );

        return savedMembership;
    }

    public List<GroupMembership> getUserMemberships(String userId) {
        return membershipRepository.findByUserId(userId);
    }

    public GroupPost sharePost(String groupId, String postId, String userId) {
        Group group = getGroupById(groupId);
        Post post = postRepository.findById(postId)
            .orElseThrow(() -> new ResourceNotFoundException("Post", "id", postId));

        // Allow both group owners and members to share posts
        boolean isMember = membershipRepository.existsByGroupIdAndUserId(groupId, userId);
        boolean isOwner = group.getOwnerId().equals(userId);
        
        if (!isMember && !isOwner) {
            throw new IllegalStateException("User must be a member or owner of the group to share posts");
        }

        // Check if post is already shared in the group
        if (groupPostRepository.existsByGroupIdAndPostId(groupId, postId)) {
            throw new IllegalStateException("This post is already shared in the group");
        }

        GroupPost groupPost = new GroupPost();
        groupPost.setGroupId(groupId);
        groupPost.setPostId(postId);
        groupPost.setSharedById(userId);

        GroupPost savedPost = groupPostRepository.save(groupPost);

        // Notify group members
        notificationService.createNotification(
            group.getOwnerId(),
            "New Post in Group",
            "A new post has been shared in your group: " + group.getName(),
            NotificationType.GROUP_POST,
            groupId
        );

        return savedPost;
    }

    public List<Group> getGroupsByMemberId(String userId) {
        List<GroupMembership> memberships = membershipRepository.findByUserId(userId);
        List<Group> groups = new ArrayList<>();
        
        for (GroupMembership membership : memberships) {
            groupRepository.findById(membership.getGroupId())
                .ifPresent(groups::add);
        }
        
        return groups;
    }
    
    public List<Post> getGroupPosts(String groupId) {
        // First, verify the group exists
        getGroupById(groupId);
        
        // Find all GroupPost entries for this group
        List<GroupPost> groupPosts = groupPostRepository.findByGroupId(groupId);
        
        // Collect all the posts
        List<Post> posts = new ArrayList<>();
        for (GroupPost groupPost : groupPosts) {
            postRepository.findById(groupPost.getPostId())
                .ifPresent(posts::add);
        }
        
        return posts;
    }
    
    public List<Group> getAllUserGroups(String userId) {
        // Get groups owned by the user
        List<Group> ownedGroups = groupRepository.findByOwnerId(userId);
        
        // Get groups where user is a member
        List<Group> memberGroups = getGroupsByMemberId(userId);
        
        // Combine both lists and remove duplicates
        Set<Group> allGroups = new HashSet<>();
        allGroups.addAll(ownedGroups);
        allGroups.addAll(memberGroups);
        
        return new ArrayList<>(allGroups);
    }
    
    public List<User> getGroupMembers(String groupId) {
        // Verify group exists
        getGroupById(groupId);
        
        // Get all memberships for this group
        List<GroupMembership> memberships = membershipRepository.findByGroupId(groupId);
        
        // Get user details for each member
        List<User> members = new ArrayList<>();
        for (GroupMembership membership : memberships) {
            userRepository.findById(membership.getUserId())
                .ifPresent(members::add);
        }
        
        return members;
    }
    
    public Group updateGroup(String groupId, Group updatedGroup, String userId) {
        Group existingGroup = getGroupById(groupId);
        
        // Verify the user is the owner
        if (!existingGroup.getOwnerId().equals(userId)) {
            throw new IllegalStateException("Only the group owner can update the group");
        }
        
        // Update fields
        existingGroup.setName(updatedGroup.getName());
        existingGroup.setDescription(updatedGroup.getDescription());
        existingGroup.setPublic(updatedGroup.isPublic());  // Changed setIsPublic to setPublic to match actual setter method
        if (updatedGroup.getCoverImage() != null) {
            existingGroup.setCoverImage(updatedGroup.getCoverImage());
        }
        
        return groupRepository.save(existingGroup);
    }
    
    public void deleteGroup(String groupId, String userId) {
        Group group = getGroupById(groupId);
        
        // Verify the user is the owner
        if (!group.getOwnerId().equals(userId)) {
            throw new IllegalStateException("Only the group owner can delete the group");
        }
        
        // Delete all memberships
        List<GroupMembership> memberships = membershipRepository.findByGroupId(groupId);
        membershipRepository.deleteAll(memberships);
        
        // Delete all group posts
        List<GroupPost> groupPosts = groupPostRepository.findByGroupId(groupId);
        groupPostRepository.deleteAll(groupPosts);
        
        // Delete the group
        groupRepository.delete(group);
    }
    
    public void removeGroupPost(String groupId, String postId, String userId) {
        Group group = getGroupById(groupId);
        
        // Verify the user is the owner
        if (!group.getOwnerId().equals(userId)) {
            throw new IllegalStateException("Only the group owner can remove posts");
        }
        
        // Find the group post
        List<GroupPost> posts = groupPostRepository.findByGroupIdAndPostId(groupId, postId);
        if (posts.isEmpty()) {
            throw new ResourceNotFoundException("GroupPost", "postId", postId);
        }
        
        // Delete the group post
        groupPostRepository.deleteAll(posts);
    }
    
    public void removeGroupMember(String groupId, String memberId, String requesterId) {
        Group group = getGroupById(groupId);
        
        // Verify the requester is the owner
        if (!group.getOwnerId().equals(requesterId)) {
            throw new IllegalStateException("Only the group owner can remove members");
        }
        
        // Find the membership
        List<GroupMembership> memberships = membershipRepository.findByGroupIdAndUserId(groupId, memberId);
        if (memberships.isEmpty()) {
            throw new ResourceNotFoundException("GroupMembership", "userId", memberId);
        }
        
        // Delete the membership
        membershipRepository.deleteAll(memberships);
    }

    public void leaveGroup(String groupId, String userId) {
        Group group = getGroupById(groupId);
        
        // Prevent group owner from leaving their own group
        if (group.getOwnerId().equals(userId)) {
            throw new IllegalStateException("Group owner cannot leave the group. Delete the group instead.");
        }
        
        // Check if user is a member
        if (!membershipRepository.existsByGroupIdAndUserId(groupId, userId)) {
            throw new IllegalStateException("User is not a member of this group");
        }

        // Remove membership
        membershipRepository.deleteByGroupIdAndUserId(groupId, userId);

        // Optional: Notify group owner
        notificationService.createNotification(
            group.getOwnerId(),
            "Member Left Group",
            "A user has left your group: " + group.getName(),
            NotificationType.GROUP_JOIN_REQUEST,  // Using existing notification type for membership changes
            groupId
        );
    }
}
