package com.university.skillshare_backend.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.util.Assert;
import com.university.skillshare_backend.model.PostInsights;
import com.university.skillshare_backend.repository.PostInsightsRepository;
import com.university.skillshare_backend.exception.ResourceNotFoundException;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import com.university.skillshare_backend.repository.LikeRepository;
import com.university.skillshare_backend.repository.CommentRepository;

@Service
public class PostInsightsService {
    private static final Logger logger = LoggerFactory.getLogger(PostInsightsService.class);

    @Autowired
    private PostInsightsRepository insightsRepository;
    
    @Autowired
    private SimpMessagingTemplate messagingTemplate;

    @Autowired
    private LikeRepository likeRepository;
    
    @Autowired
    private CommentRepository commentRepository;

    private final ConcurrentHashMap<String, Set<String>> uniqueViewers = new ConcurrentHashMap<>();

    @Transactional(readOnly = true) 
    public PostInsights getInsights(String postId) {
        Assert.hasText(postId, "PostId must not be empty");
        try {
            return insightsRepository.findByPostId(postId)
                .orElseGet(() -> {
                    // Create new insights if they don't exist
                    PostInsights insights = new PostInsights(postId);
                    return insightsRepository.save(insights);
                });
        } catch (Exception e) {
            logger.error("Error fetching insights for post {}: {}", postId, e.getMessage());
            // Return empty insights rather than throwing error
            return new PostInsights(postId);
        }
    }

    @Transactional
    public synchronized void incrementViews(String postId, String viewerId) {
        Assert.hasText(postId, "PostId must not be empty");
        Assert.hasText(viewerId, "ViewerId must not be empty");

        try {
            PostInsights insights = getInsights(postId);
            Set<String> viewers = uniqueViewers.computeIfAbsent(postId, k -> ConcurrentHashMap.newKeySet());
            
            insights.setViews(insights.getViews() + 1);
            if (viewers.add(viewerId)) {
                insights.setUniqueViewers(viewers.size());
            }
            updateEngagementRate(insights);
            
            PostInsights savedInsights = insightsRepository.save(insights);
            broadcastInsights(postId, savedInsights);
            
        } catch (Exception e) {
            logger.error("Error incrementing views for post {}: {}", postId, e.getMessage());
            // Don't throw exception to prevent 500 errors
        }
    }

    @Transactional
    public void syncInsights(String postId) {
        try {
            PostInsights insights = getInsights(postId);
            
            // Get actual counts from repositories
            long actualLikeCount = likeRepository.countByPostId(postId);
            long actualCommentCount = commentRepository.countByPostId(postId);
            
            // Update counts
            insights.setLikeCount((int) actualLikeCount);
            insights.setCommentCount((int) actualCommentCount);
            
            // Update engagement rate
            updateEngagementRate(insights);
            
            // Save and broadcast
            PostInsights savedInsights = insightsRepository.save(insights);
            broadcastInsights(postId, savedInsights);
            
            logger.debug("Synced insights for post {}: likes={}, comments={}", 
                postId, actualLikeCount, actualCommentCount);
        } catch (Exception e) {
            logger.error("Error syncing insights for post {}: {}", postId, e.getMessage());
        }
    }

    @Transactional
    public void updateLikes(String postId, int likeCount) {
        try {
            PostInsights insights = getInsights(postId);
            insights.setLikeCount(likeCount);
            updateEngagementRate(insights);
            PostInsights savedInsights = insightsRepository.save(insights);
            broadcastInsights(postId, savedInsights);
            
            logger.debug("Updated likes for post {}: {}", postId, likeCount);
        } catch (Exception e) {
            logger.error("Error updating likes for post {}: {}", postId, e.getMessage());
            syncInsights(postId); // Fallback to sync if update fails
        }
    }

    @Transactional
    public void updateComments(String postId, int commentCount) {
        try {
            PostInsights insights = getInsights(postId);
            insights.setCommentCount(commentCount);
            updateEngagementRate(insights);
            PostInsights savedInsights = insightsRepository.save(insights);
            broadcastInsights(postId, savedInsights);
            
            logger.debug("Updated comments for post {}: {}", postId, commentCount);
        } catch (Exception e) {
            logger.error("Error updating comments for post {}: {}", postId, e.getMessage());
            syncInsights(postId); // Fallback to sync if update fails
        }
    }

    private void updateEngagementRate(PostInsights insights) {
        try {
            double totalEngagements = Math.max(0, insights.getLikeCount() + 
                                                insights.getCommentCount() + 
                                                insights.getShareCount());
            double viewCount = Math.max(1, insights.getViews());
            double rate = (totalEngagements / viewCount) * 100;
            insights.setEngagementRate(Math.min(100, rate)); // Cap at 100%
        } catch (Exception e) {
            logger.error("Error calculating engagement rate: {}", e.getMessage());
            insights.setEngagementRate(0.0); // Fallback to 0 on error
        }
    }

    private void broadcastInsights(String postId, PostInsights insights) {
        try {
            messagingTemplate.convertAndSend("/topic/insights/" + postId, insights);
        } catch (Exception e) {
            logger.error("Error broadcasting insights for post {}: {}", postId, e.getMessage());
            // Don't throw exception here to prevent transaction rollback
        }
    }

    private PostInsights createNewInsights(String postId) {
        PostInsights insights = new PostInsights(postId);
        return insightsRepository.save(insights);
    }
}
