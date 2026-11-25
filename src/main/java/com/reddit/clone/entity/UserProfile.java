package com.reddit.clone.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

@Entity
@Table(name = "user_profiles")
@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class UserProfile {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne
    @JoinColumn(name = "user_id")
//    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    private String displayName;

    @Column(length = 500)
    private String bio;

    private String profileImageUrl;
    private String bannerImageUrl;

    private String location;
    private String website;

    private Instant updatedDate;

    @PrePersist
    @PreUpdate
    public void preUpdate() {
        if (displayName == null || displayName.isBlank()) {
            displayName = "New User";
        }

        if (profileImageUrl == null || profileImageUrl.isBlank()) {
            profileImageUrl = "https://www.redditstatic.com/avatars/avatar_default_02_46D160.png";
        }

        if (bannerImageUrl == null || bannerImageUrl.isBlank()) {
            bannerImageUrl = "https://40687240.fs1.hubspotusercontent-na1.net/hubfs/40687240/Reddit%20Inc/Blog/Imported_Blog_Media/sxbofyls_2021-08-25-021317_fbly.png";
        }

        updatedDate = Instant.now();
    }
}
