package com.reddit.clone.advice;

import com.reddit.clone.service.MessageService;
import com.reddit.clone.service.auth.AuthService;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

@ControllerAdvice
public class GlobalModelAttributes {

    private final MessageService messageService;
    private final AuthService authService;

    public GlobalModelAttributes(MessageService messageService, AuthService authService) {
        this.messageService = messageService;
        this.authService = authService;
    }

    @ModelAttribute
    public void addUnreadCount(Model model) {
        if (authService.getCurrentUser() != null) {
            int unreadCount = messageService.countUnreadMessages(authService.getCurrentUser().getId());
            model.addAttribute("unreadCount", unreadCount);
        }
    }
}
