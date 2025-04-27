package com.university.skillshare_backend.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.university.skillshare_backend.dto.UserUpdateRequest;
import com.university.skillshare_backend.exception.ResourceNotFoundException;
import com.university.skillshare_backend.model.User;
import com.university.skillshare_backend.repository.UserRepository;
import com.university.skillshare_backend.repository.FollowRepository;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api")
@CrossOrigin(origins = "*") // Consider restricting this in production
public class UserController {

    private final UserRepository userRepository;
    private final FollowRepository followRepository;
    
    @Autowired
    public UserController(UserRepository userRepository, FollowRepository followRepository) {
        this.userRepository = userRepository;
        this.followRepository = followRepository;
    }
    
    /**
     * Create a new user
     * 
     * @param user User object from request body
     * @return The created user
     */
    @PostMapping("/users")
    public ResponseEntity<User> createUser(@RequestBody User user) {
        User savedUser = userRepository.save(user);
        return new ResponseEntity<>(savedUser, HttpStatus.CREATED);
    }
    
    /**
     * Search for users by username fragment (for @mentions)
     * 
     * @param query Username fragment to search
     * @return List of matching users
     */
    @GetMapping("/users/search")
    public ResponseEntity<List<User>> searchUsers(@RequestParam String query) {
        List<User> users = userRepository.findByUsernameContainingIgnoreCase(query);
        // Only return necessary fields
        users.forEach(user -> {
            user.setPassword(null);
            user.setEmail(null);
        });
        return ResponseEntity.ok(users);
    }
    
    /**
     * Get all users
     * 
     * @return List of all users
     */
    @GetMapping("/users")
    public ResponseEntity<List<User>> getAllUsers() {
        List<User> users = userRepository.findAll();
        return ResponseEntity.ok(users);
    }
    
    /**
     * Get user by ID
     * 
     * @param userId User ID
     * @return User details
     */
    @GetMapping("/users/{userId}")
    public ResponseEntity<User> getUserById(@PathVariable String userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", userId));
        return ResponseEntity.ok(user);
    }
    
    /**
     * Get user by username
     * 
     * @param username Username
     * @return User details
     */
    @GetMapping("/users/username/{username}")
    public ResponseEntity<User> getUserByUsername(@PathVariable String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("User", "username", username));
        return ResponseEntity.ok(user);
    }
    
    /**
     * Update user profile
     * 
     * @param userId User ID
     * @param userUpdateRequest Fields to update
     * @return Updated user details
     */
    @PutMapping("/users/{userId}")
    public ResponseEntity<?> updateUser(@PathVariable String userId, @RequestBody UserUpdateRequest userUpdateRequest) {
        User existingUser = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", userId));
        
        // Check if username is changed and not already taken
        if (userUpdateRequest.getUsername() != null && 
            !userUpdateRequest.getUsername().equals(existingUser.getUsername())) {
            if (userRepository.findByUsername(userUpdateRequest.getUsername()).isPresent()) {
                Map<String, String> response = new HashMap<>();
                response.put("error", "Username is already taken");
                return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
            }
            existingUser.setUsername(userUpdateRequest.getUsername());
        }
        
        // Check if email is changed and not already taken
        if (userUpdateRequest.getEmail() != null && 
            !userUpdateRequest.getEmail().equals(existingUser.getEmail())) {
            if (userRepository.findByEmail(userUpdateRequest.getEmail()).isPresent()) {
                Map<String, String> response = new HashMap<>();
                response.put("error", "Email is already registered");
                return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
            }
            existingUser.setEmail(userUpdateRequest.getEmail());
        }
        
        // Update bio if provided
        if (userUpdateRequest.getBio() != null) {
            existingUser.setBio(userUpdateRequest.getBio());
        }

        // Update role if provided (and user is authorized)
        if (userUpdateRequest.getRole() != null) {
            existingUser.setRole(userUpdateRequest.getRole());
        }

        // Update other fields
        if (userUpdateRequest.getFullName() != null) {
            existingUser.setFullName(userUpdateRequest.getFullName());
        }

        if (userUpdateRequest.getSpecializations() != null) {
            existingUser.setSpecializations(userUpdateRequest.getSpecializations());
        }
        
        User updatedUser = userRepository.save(existingUser);
        updatedUser.setPassword(null); // Don't return password
        
        return ResponseEntity.ok(updatedUser);
    }

    @GetMapping("/users/{userId}/counts")
    public ResponseEntity<Map<String, Long>> getUserCounts(@PathVariable String userId) {
        Map<String, Long> counts = new HashMap<>();
        counts.put("followers", followRepository.countByFollowedId(userId));
        counts.put("following", followRepository.countByFollowerId(userId));
        return ResponseEntity.ok(counts);
    }
}
