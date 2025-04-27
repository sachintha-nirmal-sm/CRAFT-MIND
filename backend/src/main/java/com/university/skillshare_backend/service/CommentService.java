package com.university.skillshare_backend.service;

import java.util.List;
import java.util.Map;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Collections;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.university.skillshare_backend.exception.ResourceNotFoundException;
import com.university.skillshare_backend.model.Comment;
import com.university.skillshare_backend.model.User;
import com.university.skillshare_backend.repository.CommentRepository;
import com.university.skillshare_backend.repository.PostRepository;
import com.university.skillshare_backend.repository.UserRepository;
import com.university.skillshare_backend.util.MentionParser;

@Service
public class CommentService {

    private static final Logger logger = LoggerFactory.getLogger(CommentService.class);

    private final CommentRepository commentRepository;
    private final PostRepository postRepository;
    private final UserRepository userRepository;
    private final MentionParser mentionParser;
    private final WebSocketService webSocketService;
    private final NotificationService notificationService;
    
    @Autowired
    public CommentService(
            CommentRepository commentRepository,
            PostRepository postRepository,
            UserRepository userRepository,
            MentionParser mentionParser,
            WebSocketService webSocketService,
            NotificationService notificationService) {
        this.commentRepository = commentRepository;
        this.postRepository = postRepository;
        this.userRepository = userRepository;
        this.mentionParser = mentionParser;
        this.webSocketService = webSocketService;
        this.notificationService = notificationService;
    }
    
    /**
     * Add a comment to a post
     * 
     * @param postId The post ID
     * @param userId The user ID
     * @param text The comment text
     * @return The saved comment
     */
    public Comment addComment(String postId, String userId, String text) {
        try {
            // Verify post exists
            postRepository.findById(postId)
                .orElseThrow(() -> new ResourceNotFoundException("Post", "id", postId));
            
            // Verify user exists
            userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", userId));
            
            // Parse mentions
            List<String> mentions = mentionParser.parseMentions(text);
            
            // Create and save the comment
            Comment comment = new Comment(postId, userId, text);
            Comment savedComment = commentRepository.save(comment);
            
            // Get post owner and commenter details
            String postOwnerId = postRepository.findById(postId)
                .orElseThrow(() -> new ResourceNotFoundException("Post", "id", postId))
                .getUserId();
            String commenterName = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", userId))
                .getUsername();

            // Get comment position
            long commentPosition = commentRepository.countByPostId(postId);

            // Send notification to post owner with position
            if (!userId.equals(postOwnerId)) {
                notificationService.createCommentNotification(postOwnerId, commenterName, postId, (int)commentPosition);
            }

            // Process mentions with position
            for (String mentionedUsername : mentions) {
                User mentionedUser = userRepository.findByUsername(mentionedUsername)
                    .orElse(null);
                if (mentionedUser != null && !mentionedUser.getId().equals(userId)) {
                    notificationService.createMentionNotification(
                        mentionedUser.getId(), 
                        commenterName, 
                        postId,
                        (int)commentPosition
                    );
                }
            }

            // Broadcast the new comment via WebSocket
            webSocketService.broadcastNewComment(postId, savedComment);
            
            return savedComment;
        } catch (ResourceNotFoundException e) {
            logger.error("Attempted to add comment to non-existent post {}", postId);
            throw e;
        }
    }
    
    /**
     * Edit a comment
     * 
     * @param commentId The comment ID
     * @param userId The user ID (to verify ownership)
     * @param text The updated comment text
     * @return The updated comment
     */
    public Comment editComment(String commentId, String userId, String text) {
        // Fetch and verify comment exists
        Comment comment = commentRepository.findById(commentId)
                .orElseThrow(() -> new ResourceNotFoundException("Comment", "id", commentId));
        
        // Verify user owns the comment
        if (!comment.getUserId().equals(userId)) {
            throw new RuntimeException("You don't have permission to edit this comment");
        }
        
        // Get commenter's name
        String commenterName = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", userId))
                .getUsername();
        
        // Parse mentions from the updated text
        List<String> mentions = mentionParser.parseMentions(text);
        
        // Update the comment
        comment.setText(text);
        Comment updatedComment = commentRepository.save(comment);
        
        // Process mentions and send notifications
        processMentions(mentions, updatedComment, commenterName);
        
        // Broadcast the update via WebSocket
        webSocketService.broadcastCommentUpdate(comment.getPostId(), updatedComment);
        
        return updatedComment;
    }
    
    /**
     * Delete a comment
     * 
     * @param commentId The comment ID
     * @param userId The user ID (to verify ownership)
     */
    public void deleteComment(String commentId, String userId) {
        // Fetch and verify comment exists
        Comment comment = commentRepository.findById(commentId)
                .orElseThrow(() -> new ResourceNotFoundException("Comment", "id", commentId));
        
        // Verify user owns the comment
        if (!comment.getUserId().equals(userId)) {
            throw new RuntimeException("You don't have permission to delete this comment");
        }
        
        // Delete the comment
        commentRepository.delete(comment);
        
        // Broadcast the deletion via WebSocket
        webSocketService.broadcastCommentUpdate(comment.getPostId(), 
                java.util.Collections.singletonMap("deleted", commentId));
    }
    
    /**
     * Get all comments for a post
     * 
     * @param postId The post ID
     * @return List of comments
     */
    public List<Comment> getCommentsByPostId(String postId) {
        try {
            // Verify post exists
            postRepository.findById(postId)
                .orElseThrow(() -> new ResourceNotFoundException("Post", "id", postId));
            
            return commentRepository.findByPostId(postId);
        } catch (ResourceNotFoundException e) {
            logger.warn("Attempted to get comments for non-existent post {}", postId);
            return Collections.emptyList(); // Return empty list instead of throwing error
        }
    }
    
    /**
     * Get all comments for a post with user details
     * 
     * @param postId The post ID
     * @return List of comments with user details
     */
    public List<Map<String, Object>> getCommentsByPostIdWithUserDetails(String postId) {
        // Verify post exists
        postRepository.findById(postId)
            .orElseThrow(() -> new ResourceNotFoundException("Post", "id", postId));
        
        List<Comment> comments = commentRepository.findByPostId(postId);
        List<Map<String, Object>> enrichedComments = new ArrayList<>();
        
        for (Comment comment : comments) {
            Map<String, Object> enrichedComment = new HashMap<>();
            enrichedComment.put("id", comment.getId());
            enrichedComment.put("postId", comment.getPostId());
            enrichedComment.put("userId", comment.getUserId());
            enrichedComment.put("text", comment.getText());
            enrichedComment.put("createdAt", comment.getCreatedAt());
            
            // Add user details
            userRepository.findById(comment.getUserId()).ifPresent(user -> {
                enrichedComment.put("username", user.getUsername());
                enrichedComment.put("fullName", user.getFullName());
            });
            
            enrichedComments.add(enrichedComment);
        }
        
        return enrichedComments;
    }
    
    /**
     * Process mentions in a comment
     * 
     * @param mentions List of usernames mentioned
     * @param comment The comment containing mentions
     * @param commenterName The name of the commenter
     */
    private void processMentions(List<String> mentions, Comment comment, String commenterName) {
        for (String username : mentions) {
            userRepository.findByUsername(username).ifPresent(mentionedUser -> {
                // Don't notify if user mentions themselves
                if (!mentionedUser.getId().equals(comment.getUserId())) {
                    notificationService.createMentionNotification(
                        mentionedUser.getId(), 
                        commenterName,
                        comment.getPostId()
                    );
                }
            });
        }
    }
}
