package com.reddit.clone.service;

import com.reddit.clone.dto.UserViewDto;
import com.reddit.clone.entity.User;
import com.reddit.clone.entity.UserProfile;
import com.reddit.clone.exception.RedditCloneException;
import com.reddit.clone.mapper.UserMapper;
import com.reddit.clone.repository.UserRepository;
import com.reddit.clone.service.auth.AuthService;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.time.Instant;

@Service
public class UserService {

    // We can wrap this in a separate class, for now keep it simple
    private static final String BASE_NAME = "reddit_user_";
    private static final String ALPHANUM = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789";
    private static final int RANDOM_LENGTH = 6;
    private final SecureRandom random = new SecureRandom();

    public String generateUniqueUsername() {
        String username;
        int attempts = 0;
        do {
            username = BASE_NAME + generateRandomString();
            if (attempts > 10) {
                throw new RuntimeException("Failed to generate unique username");
            }
            attempts++;
        } while (userRepository.existsByUsername(username));
        return username;
    }

    private String generateRandomString() {
        StringBuilder sb = new StringBuilder(UserService.RANDOM_LENGTH);
        for (int i = 0; i < UserService.RANDOM_LENGTH; i++) {
            int index = random.nextInt(ALPHANUM.length());
            sb.append(ALPHANUM.charAt(index));
        }
        return sb.toString();
    }

    // Let's Keep it simple '
    //

    private final UserMapper userMapper;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthService authService;

    public UserService(UserMapper userMapper, UserRepository repo, PasswordEncoder passwordEncoder, AuthService authService) {
        this.passwordEncoder = passwordEncoder;
        this.userMapper = userMapper;
        this.userRepository = repo;
        this.authService = authService;
    }

    public User register(User user) {
        if (userRepository.existsByEmail(user.getEmail())) {
            throw new IllegalArgumentException("Email already in use");
        }

        String encodedPassword = passwordEncoder.encode(user.getPassword());
        user.setPassword(encodedPassword);

        String generatedUsername = generateUniqueUsername();
        user.setUsername(generatedUsername);

        UserProfile userProfile = new UserProfile();
        userProfile.setUser(user);
        user.setProfile(userProfile);

        user.setCreatedDate(Instant.now());
        return userRepository.save(user);
    }

    // We have to this in the mappper , Modify Later
    @Cacheable(value = "userViewCache", key = "#username")
    public UserViewDto getUserView(String username) {
        User user = findByUsername(username);
        return userMapper.mapToUserView(user);
    }

    @Cacheable(value = "userCache", key = "#username")
    public User findByUsername(String username) {
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("User not found"));
    }

    @CacheEvict(value = {"userViewCache", "userCache"},  key = "#user.username")
    public User saveUser(User user) {
        return userRepository.save(user);
    }


//    @Caching(evict = {
//            @CacheEvict(value = "userViewCache", key = "#userToUnfollow != null ? #userToUnfollow.username : null"),
//            @CacheEvict(value = "userViewCache", key = "#follower != null ? #follower.username : null"),
//            @CacheEvict(value = "userCache", key = "#userToUnfollow != null ? #userToUnfollow.username : null"),
//            @CacheEvict(value = "userCache", key = "#follower != null ? #follower.username : null")
//    })
    @CacheEvict(value = {"userViewCache", "userCache"}, allEntries = true)
    public void followUser(User follower, User userToFollow) {
        if (follower.getId().equals(userToFollow.getId())) {
            throw new IllegalArgumentException("Cannot follow yourself");
        }

        userToFollow.getFollowers().add(follower);
        userRepository.save(userToFollow);
    }

//    @Caching(evict = {
//            @CacheEvict(value = "userViewCache", key = "#userToUnfollow != null ? #userToUnfollow.username : null"),
//            @CacheEvict(value = "userViewCache", key = "#follower != null ? #follower.username : null"),
//            @CacheEvict(value = "userCache", key = "#userToUnfollow != null ? #userToUnfollow.username : null"),
//            @CacheEvict(value = "userCache", key = "#follower != null ? #follower.username : null")
//    })
    @CacheEvict(value = {"userViewCache", "userCache"}, allEntries = true)
    public void unfollowUser(User follower, User userToUnfollow) {
        if (follower.getId().equals(userToUnfollow.getId())) {
            throw new IllegalArgumentException("Cannot unfollow yourself");
        }

        userToUnfollow.getFollowers().remove(follower);
        userRepository.save(userToUnfollow);
    }

    public boolean isOwner(String username) {
        var currentUser = authService.getCurrentUser();
        if (currentUser == null) return false;
        return currentUser.getUsername().equals(username);
    }

}
