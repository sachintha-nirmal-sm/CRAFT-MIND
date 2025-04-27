package com.university.skillshare_backend.controller;

import com.university.skillshare_backend.model.Achievement;
import com.university.skillshare_backend.repository.AchievementRepository;
import com.university.skillshare_backend.repository.UserRepository;
import com.university.skillshare_backend.exception.ResourceNotFoundException;
import com.university.skillshare_backend.exception.UnauthorizedException;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.Date;
import java.util.Set;

@RestController
@RequestMapping("/api")
@CrossOrigin(origins = "*")
public class AchievementController {

    @Autowired
    private AchievementRepository achievementRepository;

    @Autowired
    private UserRepository userRepository;

    @PostMapping("/achievements")
    public ResponseEntity<Achievement> createAchievement(@RequestBody Achievement achievement) {
        userRepository.findById(achievement.getUserId())
            .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        Achievement savedAchievement = achievementRepository.save(achievement);
        return new ResponseEntity<>(savedAchievement, HttpStatus.CREATED);
    }

    @GetMapping("/achievements")
    public ResponseEntity<List<Achievement>> getAllAchievements() {
        try {
            List<Achievement> achievements = achievementRepository.findAllByOrderByCreatedAtDesc();
            return ResponseEntity.ok(achievements);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @GetMapping("/achievements/recent")
    public ResponseEntity<List<Achievement>> getRecentAchievements() {
        try {
            List<Achievement> achievements = achievementRepository.findAllByOrderByCreatedAtDesc();
            return ResponseEntity.ok(achievements);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @GetMapping("/users/{userId}/achievements")
    public ResponseEntity<List<Achievement>> getUserAchievements(@PathVariable String userId) {
        try {
            List<Achievement> achievements = achievementRepository.findByUserIdOrderByCreatedAtDesc(userId);
            return ResponseEntity.ok(achievements);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @GetMapping("/users/{userId}/achievements/category/{category}")
    public ResponseEntity<List<Achievement>> getUserAchievementsByCategory(
            @PathVariable String userId,
            @PathVariable String category) {
        try {
            List<Achievement> achievements = achievementRepository.findByUserIdAndCategoryOrderByCreatedAtDesc(userId, category);
            return ResponseEntity.ok(achievements);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @GetMapping("/users/{userId}/achievements/categories")
    public ResponseEntity<Set<String>> getUserAchievementCategories(@PathVariable String userId) {
        try {
            Set<String> categories = achievementRepository.findDistinctCategoriesByUserId(userId);
            return ResponseEntity.ok(categories);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @GetMapping("/users/{userId}/achievements/latest")
    public ResponseEntity<List<Achievement>> getLatestUserAchievements(@PathVariable String userId) {
        try {
            List<Achievement> achievements = achievementRepository.findByUserIdOrderByCreatedAtDesc(userId);
            return ResponseEntity.ok(achievements);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @GetMapping("/users/{userId}/my-achievements")
    public ResponseEntity<List<Achievement>> getUserOwnAchievements(@PathVariable String userId) {
        try {
            List<Achievement> achievements = achievementRepository.findByUserIdOrderByCreatedAtDesc(userId);
            return ResponseEntity.ok(achievements);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @PutMapping("/achievements/{achievementId}")
    public ResponseEntity<?> updateAchievement(
            @PathVariable String achievementId,
            @RequestParam String userId,
            @RequestBody Achievement updatedAchievement) {
        Achievement achievement = achievementRepository.findById(achievementId)
                .orElseThrow(() -> new ResourceNotFoundException("Achievement not found"));
        
        if (!achievement.getUserId().equals(userId)) {
            throw new UnauthorizedException("Not authorized to edit this achievement");
        }

        achievement.setTitle(updatedAchievement.getTitle());
        achievement.setDescription(updatedAchievement.getDescription());
        achievement.setImageUrl(updatedAchievement.getImageUrl());
        achievement.setVideoUrl(updatedAchievement.getVideoUrl());
        achievement.setUpdatedAt(new Date());

        Achievement saved = achievementRepository.save(achievement);
        return ResponseEntity.ok(saved);
    }

    @DeleteMapping("/achievements/{achievementId}")
    public ResponseEntity<?> deleteAchievement(
            @PathVariable String achievementId,
            @RequestParam String userId) {
        Achievement achievement = achievementRepository.findById(achievementId)
                .orElseThrow(() -> new ResourceNotFoundException("Achievement not found"));

        if (!achievement.getUserId().equals(userId)) {
            throw new UnauthorizedException("Not authorized to delete this achievement");
        }

        achievementRepository.delete(achievement);
        return ResponseEntity.ok().build();
    }
}
