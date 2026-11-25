package com.reddit.clone.service;

import com.reddit.clone.entity.UserProfile;
import com.reddit.clone.repository.UserProfileRepository;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

@Service
public class UserProfileService {

    private final UserProfileRepository userProfileRepository;

    public UserProfileService(UserProfileRepository userProfileRepository) {
        this.userProfileRepository = userProfileRepository;
    }

    @CacheEvict(value = {"communityCache", "allCommunitiesCache", "searchCommunitiesCache",
            "postCache", "postFilesCache", "communityPostsCache", "hotPostsCache", "newPostsCache", "userCache", "userViewCache"
    }, allEntries = true)
    public void saveUserProfile(UserProfile userProfile) {
        userProfileRepository.save(userProfile);
    }

}
