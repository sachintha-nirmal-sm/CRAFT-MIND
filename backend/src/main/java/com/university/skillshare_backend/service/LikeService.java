package com.university.skillshare_backend.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.university.skillshare_backend.exception.ResourceNotFoundException;
import com.university.skillshare_backend.model.Like;
import com.university.skillshare_backend.model.Post;
import com.university.skillshare_backend.repository.LikeRepository;
import com.university.skillshare_backend.repository.PostRepository;
import com.university.skillshare_backend.repository.UserRepository;

@Service
public class LikeService {

    private static final Logger logger = LoggerFactory.getLogger(LikeService.class);

    private final LikeRepository likeRepository;
    private final PostRepository postRepository;
    private final UserRepository userRepository;
    private final NotificationService notificationService;
    private final WebSocketService webSocketService;
    
    @Autowired
    private PostInsightsService postInsightsService;
    
    @Autowired
    public LikeService(
            LikeRepository likeRepository, 
            PostRepository postRepository,
            UserRepository userRepository,
            NotificationService notificationService,
            WebSocketService webSocketService) {
        this.likeRepository = likeRepository;
        this.postRepository = postRepository;
        this.userRepository = userRepository;
        this.notificationService = notificationService;
        this.webSocketService = webSocketService;
    }
    
    /**
     * Add a like to a post
     * 
     * @param postId The post ID
     * @param userId The user ID
     * @return The updated like count
     */
    public long likePost(String postId, String userId) {
        try {
            // Verify post exists and get post owner
            Post post = postRepository.findById(postId)
                .orElseThrow(() -> new ResourceNotFoundException("Post", "id", postId));
            
            // Get user details for notification
            String likerName = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", userId))
                .getUsername();

            // Check if user already liked the post
            Like existingLike = likeRepository.findByPostIdAndUserId(postId, userId);
            if (existingLike == null) {
                Like like = new Like(postId, userId);
                likeRepository.save(like);
                
                // Send notification to post owner if they're not the one liking
                if (!userId.equals(post.getUserId())) {
                    notificationService.createLikeNotification(post.getUserId(), likerName, postId);
                }
            }
            
            // Get updated like count
            long likeCount = likeRepository.countByPostId(postId);
            
            try {
                // Update insights with new like count
                postInsightsService.updateLikes(postId, (int) likeCount);
                
                // Broadcast the update via WebSocket
                webSocketService.broadcastLikeCount(postId, likeCount);
            } catch (Exception e) {
                logger.error("Error updating insights/broadcasting for post {}: {}", postId, e.getMessage());
                // Continue execution since the like was still recorded
            }
            
            return likeCount;
        } catch (ResourceNotFoundException e) {
            logger.error("Attempted to like non-existent post/user: {}", e.getMessage());
            throw e;
        }
    }
    
    /**
     * Remove a like from a post
     * 
     * @param postId The post ID
     * @param userId The user ID
     * @return The updated like count
     */
    public long unlikePost(String postId, String userId) {
        // Verify post exists
        postRepository.findById(postId)
            .orElseThrow(() -> new ResourceNotFoundException("Post", "id", postId));
        
        // Delete the like if it exists
        likeRepository.deleteByPostIdAndUserId(postId, userId);
        
        // Get updated like count
        long likeCount = likeRepository.countByPostId(postId);
        
        try {
            // Update insights with new like count
            postInsightsService.updateLikes(postId, (int) likeCount);
            
            // Broadcast the update via WebSocket
            webSocketService.broadcastLikeCount(postId, likeCount);
        } catch (Exception e) {
            logger.error("Error updating insights/broadcasting for post {}: {}", postId, e.getMessage());
            // Continue execution since the unlike was still recorded
        }
        
        return likeCount;
    }
    
    /**
     * Get the like count for a post
     * 
     * @param postId The post ID
     * @return The like count
     */
    public long getLikeCount(String postId) {
        try {
            // Verify post exists
            postRepository.findById(postId)
                .orElseThrow(() -> new ResourceNotFoundException("Post", "id", postId));
            
            return likeRepository.countByPostId(postId);
        } catch (ResourceNotFoundException e) {
            // Return 0 likes for non-existent posts instead of throwing error
            logger.warn("Attempted to get likes for non-existent post {}", postId);
            return 0;
        }
    }
    
    /**
     * Check if a user has liked a post
     * 
     * @param postId The post ID
     * @param userId The user ID
     * @return True if the user liked the post, false otherwise
     */
    public boolean hasUserLiked(String postId, String userId) {
        try {
            // Verify post exists
            postRepository.findById(postId)
                .orElseThrow(() -> new ResourceNotFoundException("Post", "id", postId));
            
            return likeRepository.findByPostIdAndUserId(postId, userId) != null;
        } catch (ResourceNotFoundException e) {
            // Return false for non-existent posts instead of throwing error
            logger.warn("Attempted to check likes for non-existent post {}", postId);
            return false;
        }
    }
}
