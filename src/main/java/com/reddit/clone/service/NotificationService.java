package com.reddit.clone.service;

import com.reddit.clone.entity.Notification;
import com.reddit.clone.entity.User;
import com.reddit.clone.repository.NotificationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class NotificationService {

    private final NotificationRepository notificationRepository;

    // Get all notifications for a user
    public List<Notification> getNotifications(User user) {
        return notificationRepository.findByUserOrderByCreatedAtDesc(user);
    }

    // Get only unread notifications
    public List<Notification> getUnreadNotifications(User user) {
        return notificationRepository.findByUserAndIsReadFalse(user);
    }

    // Mark all notifications as read for a user - THIS IS THE CRITICAL FIX
    @Transactional
    public void markAllRead(User user) {
        notificationRepository.markAllAsRead(user);
    }

    // Create a new notification
    @Transactional
    public void createNotification(User user, String message) {
        Notification notification = new Notification();
        notification.setUser(user);
        notification.setMessage(message);
        notification.setIsRead(false);
        notificationRepository.save(notification);
    }
}
