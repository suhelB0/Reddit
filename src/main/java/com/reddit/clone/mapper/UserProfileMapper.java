package com.reddit.clone.mapper;

import com.reddit.clone.dto.UserProfileDto;
import com.reddit.clone.entity.UserProfile;
import com.reddit.clone.service.CloudinaryService;
import org.springframework.stereotype.Component;

@Component
public class UserProfileMapper {

    private final CloudinaryService cloudinaryService;

    public UserProfileMapper(CloudinaryService cloudinaryService) {
        this.cloudinaryService = cloudinaryService;
    }

    // Convert Entity → DTO
    public UserProfileDto mapEntityToDto(UserProfile userProfile) {
        if (userProfile == null) {
            return new UserProfileDto();
        }

        UserProfileDto dto = new UserProfileDto();
        dto.setDisplayName(userProfile.getDisplayName());
        dto.setBio(userProfile.getBio());
        dto.setLocation(userProfile.getLocation());
        dto.setWebsite(userProfile.getWebsite());
        dto.setBannerImageUrl(userProfile.getBannerImageUrl());
        dto.setProfileImageUrl(userProfile.getProfileImageUrl());
        return dto;
    }

    // Convert DTO → Entity
    public UserProfile mapDtoToEntity(UserProfileDto dto, UserProfile userProfile) {
        if (userProfile == null) userProfile = new UserProfile();

        userProfile.setDisplayName(dto.getDisplayName());
        userProfile.setBio(dto.getBio());
        userProfile.setLocation(dto.getLocation());
        userProfile.setWebsite(dto.getWebsite());

        if (dto.getProfileImage() != null && !dto.getProfileImage().isEmpty()) {
            String uploadedUrl = cloudinaryService.uploadFile(dto.getProfileImage());
            userProfile.setProfileImageUrl(uploadedUrl);
        } else if (userProfile.getProfileImageUrl() == null) {
            userProfile.setProfileImageUrl("https://www.redditstatic.com/avatars/avatar_default_02_46D160.png");
        }

        if (dto.getBannerImage() != null && !dto.getBannerImage().isEmpty()) {
            String uploadedUrl = cloudinaryService.uploadFile(dto.getBannerImage());
            userProfile.setBannerImageUrl(uploadedUrl);
        } else if (userProfile.getBannerImageUrl() == null) {
            userProfile.setBannerImageUrl("https://styles.redditmedia.com/t5_2qh33/styles/bannerBackgroundImage_2j3dfp4g3xv91.png");
        }


        return userProfile;
    }
}
