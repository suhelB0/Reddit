package com.reddit.clone.controller;

import com.reddit.clone.entity.User;
import com.reddit.clone.service.NotificationService;
import com.reddit.clone.service.auth.AuthService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/notifications")
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationService notificationService;
    private final AuthService authService;

    @PostMapping("/mark-read")
    public ResponseEntity<?> markAllRead() {
        User currentUser = authService.getCurrentUser();
        if (currentUser == null) {
            return ResponseEntity.status(401).body("User not authenticated");
        }
        notificationService.markAllRead(currentUser);
        return ResponseEntity.ok().build();
    }
}
