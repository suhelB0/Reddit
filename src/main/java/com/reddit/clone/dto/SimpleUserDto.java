package com.reddit.clone.dto;

import lombok.*;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SimpleUserDto {
    private Long userId;
    private String username;
    private String displayName;
    private String profileImageUrl;
}