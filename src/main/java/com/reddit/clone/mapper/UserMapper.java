package com.reddit.clone.mapper;

import com.reddit.clone.dto.SimpleUserDto;
import com.reddit.clone.dto.UserDto;
import com.reddit.clone.dto.UserViewDto;
import com.reddit.clone.entity.User;
import com.reddit.clone.entity.UserProfile;
import com.reddit.clone.entity.VoteType;
import com.reddit.clone.service.auth.AuthService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
@Component
public class UserMapper {

    private final PostMapper postMapper;
    private final VoteMapper voteMapper;
    private final CommentMapper commentMapper;
    private final AuthService authService;

    public UserMapper(PostMapper postMapper, VoteMapper voteMapper, CommentMapper commentMapper, AuthService authService) {
        this.postMapper = postMapper;
        this.voteMapper = voteMapper;
        this.commentMapper = commentMapper;
        this.authService = authService;
    }

    public UserDto mapEntityToDto(User user) {
        return UserDto.builder()
                .id(user.getId())
                .username(user.getUsername())
                .email(user.getEmail())
//                .createdDate(user.getCreatedDate())
//                .enabled(user.getEnabled())
//                .postCount(user.getPosts() != null ?
//                        user.getPosts().size() : 0)
//                .commentCount(user.getComments() != null ?
//                        user.getComments().size() : 0)
//                .communityCount(user.getCreatedCommunities() != null ?
//                        user.getCreatedCommunities().size() : 0)
                .build();
    }

    public User mapDtoToEntity(UserDto userDto) {
        return User.builder()
                .id(userDto.getId())
//                .username(userDto.getUsername())
                .email(userDto.getEmail())
//                .createdDate(userDto.getCreatedDate())
//                .enabled(userDto.getEnabled())
                .password(userDto.getPassword() != null ?
                                userDto.getPassword() : null)
                .build();
    }

    private static final String DEFAULT_AVATAR =
            "https://www.redditstatic.com/avatars/defaults/v2/avatar_default_1.png";

    public UserViewDto mapToUserView(User user) {
        if (user == null) {
            return null;
        }

        UserProfile profile = user.getProfile();

        User loggedInUser = authService.getCurrentUser();

        System.out.println(user.equals(loggedInUser));

        boolean isFollowing = false;
        if (loggedInUser != null && !user.equals(loggedInUser)) {
            isFollowing = user.getFollowers().contains(loggedInUser);
        }

        return UserViewDto.builder()
                .userId(user.getId())
                .username(user.getUsername())
                .createdDate(user.getCreatedDate())
                .displayName(profile != null ? profile.getDisplayName() : user.getUsername())
                .bio(profile != null ? profile.getBio() : null)
                .profileImageUrl(profile != null && profile.getProfileImageUrl() != null
                        ? profile.getProfileImageUrl() : DEFAULT_AVATAR)
                .bannerImageUrl(profile != null ? profile.getBannerImageUrl() : null)
                .location(profile != null ? profile.getLocation() : null)
                .website(profile != null ? profile.getWebsite() : null)
                .postKarma(calculatePostKarma(user))
                .commentKarma(calculateCommentKarma(user))
                .totalPosts(user.getPosts() != null ? user.getPosts().size() : 0)
                .totalComments(user.getComments() != null ? user.getComments().size() : 0)
                .totalCommunities(user.getCreatedCommunities() != null ? user.getCreatedCommunities().size() : 0)
                .posts(postMapper.mapEntityToDto(user.getPosts()))
                .comments(commentMapper.mapEntityToDto(user.getComments()))
                .upvotedPosts(voteMapper.mapVoteEntityToDtoByType(user.getVotes(), VoteType.UPVOTE))
                .downvotedPosts(voteMapper.mapVoteEntityToDtoByType(user.getVotes(),VoteType.DOWNVOTE))
                .isOwnProfile(user.equals(loggedInUser))
                .isFollowing(isFollowing)
                .followers(mapUsersToSimpleDto(user.getFollowers()))
                .following(mapUsersToSimpleDto(user.getFollowing()))
                .totalFollowers(user.getFollowers() != null ? user.getFollowers().size() : 0)
                .totalFollowing(user.getFollowing() != null ? user.getFollowing().size() : 0)
                .build();
    }

    private List<SimpleUserDto> mapUsersToSimpleDto(Set<User> users) {
        if (users == null || users.isEmpty()) {
            return new ArrayList<>();
        }

        return users.stream()
                .map(user -> {
                    UserProfile profile = user.getProfile();
                    return SimpleUserDto.builder()
                            .userId(user.getId())
                            .username(user.getUsername())
                            .displayName(profile != null ? profile.getDisplayName() : user.getUsername())
                            .profileImageUrl(profile != null && profile.getProfileImageUrl() != null
                                    ? profile.getProfileImageUrl() : DEFAULT_AVATAR)
                            .build();
                })
                .collect(Collectors.toList());
    }


    private Integer calculateCommentKarma(User user) {
        if (user.getComments() == null || user.getComments().isEmpty()) {
            return 0;
        }
     return 0;
    }

    private Integer calculatePostKarma(User user) {
        if (user.getPosts() == null || user.getPosts().isEmpty()) {
            return 0;
        }
        return user.getPosts().stream()
                .mapToInt(post -> post.getVoteCount() != null ? post.getVoteCount() : 0)
                .sum();
    }
}
