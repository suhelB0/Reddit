package com.reddit.clone.service;

import com.reddit.clone.dto.CommunityDto;
import com.reddit.clone.entity.Community;
import com.reddit.clone.entity.User;
import com.reddit.clone.exception.CommunityNotFoundException;
import com.reddit.clone.exception.RedditCloneException;
import com.reddit.clone.mapper.CommunityMapper;
import com.reddit.clone.repository.CommunityRepository;
import com.reddit.clone.repository.UserRepository;
import com.reddit.clone.service.auth.AuthService;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;
import java.util.stream.Collectors;

@Service
@AllArgsConstructor
@Slf4j
@Transactional
public class CommunityService {

    private final CommunityRepository communityRepository;
    private final CommunityMapper communityMapper;
    private final AuthService authService;
    private final UserRepository userRepository;

    @CacheEvict(value = {"communityCache", "allCommunitiesCache", "searchCommunitiesCache",
            "postCache", "postFilesCache", "communityPostsCache", "hotPostsCache", "newPostsCache", "userCache", "userViewCache"
    }, allEntries = true)
    public CommunityDto save(CommunityDto communityDto) {
        if (communityRepository.existsByName(communityDto.getName())) {
            throw new RedditCloneException("Community with name " + communityDto.getName() + " already exists");
        }

        Community community = communityMapper.mapDtoToEntity(communityDto);
         community.setCreator(authService.getCurrentUser()); // Will be added later
        Community savedCommunity = communityRepository.save(community);
        communityDto.setId(savedCommunity.getId());

        joinCommunity(savedCommunity.getId(), authService.getCurrentUser());
        log.info("Community created: {}", savedCommunity.getName());
        return communityDto;
    }

    @Cacheable(value = "allCommunitiesCache")
    @Transactional(readOnly = true)
    public List<CommunityDto> getAllCommunities() {
        return communityRepository.findAll()
                .stream()
                .map(communityMapper::mapEntityToDto)
                .toList();
    }

    @Cacheable(value = "allCommunitiesCache", key = "#pageable.pageNumber + '-' + #pageable.pageSize")
    @Transactional(readOnly = true)
    public Page<CommunityDto> getAllCommunities(Pageable pageable) {
        User currentUser = authService.getCurrentUser();
        Page<Community> communities = communityRepository.findAll(pageable);

        return communities.map(community -> {
            CommunityDto dto = communityMapper.mapEntityToDto(community);
            return enrichWithUserInfo(dto, currentUser);
        });
    }

    @Cacheable(value = "communityCache", key = "#id")
    @Transactional(readOnly = true)
    public CommunityDto getCommunityById(Long id) {
        Community community = communityRepository.findById(id)
                .orElseThrow(() -> new CommunityNotFoundException("Community not found with ID: " + id));
        return communityMapper.mapEntityToDto(community);
    }

    @CacheEvict(value = {"communityCache", "allCommunitiesCache", "searchCommunitiesCache",
            "postCache", "postFilesCache", "communityPostsCache", "hotPostsCache", "newPostsCache", "userCache", "userViewCache"
    }, allEntries = true)
    public CommunityDto updateCommunity(Long id, CommunityDto communityDto) {
        Community community = communityRepository.findById(id)
                .orElseThrow(() -> new CommunityNotFoundException("Community not found with ID: " + id));

        community.setDescription(communityDto.getDescription());
        community.setType(communityDto.getType());

        Community updatedCommunity = communityRepository.save(community);
        return communityMapper.mapEntityToDto(updatedCommunity);
    }

    @CacheEvict(value = {"communityCache", "allCommunitiesCache", "searchCommunitiesCache",
            "postCache", "postFilesCache", "communityPostsCache", "hotPostsCache", "newPostsCache", "userCache", "userViewCache"
    }, allEntries = true)
    public void deleteCommunity(Long id) {
        if (!communityRepository.existsById(id)) {
            throw new CommunityNotFoundException("Community not found with ID: " + id);
        }
        communityRepository.deleteById(id);
        log.info("Community deleted with ID: {}", id);
    }

    @Cacheable(value = "searchCommunitiesCache", key = "#keyword + '-' + #pageable.pageNumber + '-' + #pageable.pageSize")
    @Transactional(readOnly = true)
    public Page<CommunityDto> searchCommunities(String keyword, Pageable pageable) {
        User currentUser = authService.getCurrentUser();
        Page<Community> communities = communityRepository.findByKeyword(keyword, pageable);

        return communities.map(community -> {
            CommunityDto dto = communityMapper.mapEntityToDto(community);
            return enrichWithUserInfo(dto, currentUser);
        });
    }

    @CacheEvict(value = {"communityCache", "allCommunitiesCache", "searchCommunitiesCache",
            "postCache", "postFilesCache", "communityPostsCache", "hotPostsCache", "newPostsCache", "userCache", "userViewCache"
    }, allEntries = true)
    public void joinCommunity(Long communityId, User user) {
        Community community = communityRepository.findById(communityId)
                .orElseThrow(() -> new CommunityNotFoundException("Community not found"));

        // Check if already joined
        if (user.getJoinedCommunities().contains(community)) {
            throw new RedditCloneException("Already joined this community");
        }

        user.getJoinedCommunities().add(community);
        userRepository.save(user);
       // log.info("User {} joined community {}", user.getUsername(), community.getName());
    }

    @CacheEvict(value = {"communityCache", "allCommunitiesCache", "searchCommunitiesCache",
            "postCache", "postFilesCache", "communityPostsCache", "hotPostsCache", "newPostsCache", "userCache", "userViewCache"
    }, allEntries = true)
    public void leaveCommunity(Long communityId, User user) {
        Community community = communityRepository.findById(communityId)
                .orElseThrow(() -> new CommunityNotFoundException("Community not found"));

        user.getJoinedCommunities().remove(community);
        userRepository.save(user);
        log.info("User {} left community {}", user.getUsername(), community.getName());
    }

    @Transactional(readOnly = true)
    public List<CommunityDto> getCommunitiesJoinedByUser(User user) {

        if (user == null) {
            return List.of();
        }

        User userWithCommunities = userRepository.findByIdWithJoinedCommunities(user.getId())
                .orElseThrow(() -> new RuntimeException("User not found"));

        return userWithCommunities.getJoinedCommunities().stream()
                .map(communityMapper::mapEntityToDto)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public CommunityDto enrichWithUserInfo(CommunityDto dto, User user) {
        Community community = communityRepository.findById(dto.getId())
                .orElseThrow(() -> new CommunityNotFoundException("Community not found"));

        // Check if user has joined
        if (user != null) dto.setJoined(user.getJoinedCommunities().contains(community));

        // SET CREATOR INFO WITH NULL CHECKS
        if (community.getCreator() != null) {
            dto.setCreatorUsername(community.getCreator().getUsername());
            dto.setCreatorId(community.getCreator().getId());
        } else {
            // Handle communities without creators
            dto.setCreatorUsername("Unknown");
            dto.setCreatorId(null);
        }

        // Set member count
        dto.setMemberCount(communityRepository.countMembersByCommunityId(dto.getId()));

        return dto;
    }


    public boolean isOwner(Long communityId) {
        User currentUser = authService.getCurrentUser();
        Community community = communityRepository.findById(communityId)
                .orElseThrow(() -> new CommunityNotFoundException("Community not found with ID: " + communityId));

        if (community == null) return false;
        return community.getCreator().getId().equals(currentUser.getId());
    }

}
