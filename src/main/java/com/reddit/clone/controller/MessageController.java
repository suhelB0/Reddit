package com.reddit.clone.controller;

import com.reddit.clone.entity.Message;
import com.reddit.clone.entity.User;
import com.reddit.clone.service.MessageService;
import com.reddit.clone.service.auth.AuthService;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Controller
@RequestMapping("/messages")
@RequiredArgsConstructor
public class MessageController {

    private final MessageService messageService;
    private final AuthService authService;

    @GetMapping("/{receiverId}")
    public String getConversation(@PathVariable Long receiverId,
                                  Model model) {
        List<Message> messages = messageService.getConversation(authService.getCurrentUser().getId(), receiverId);
        model.addAttribute("messages", messages);
        model.addAttribute("receiverId", receiverId);
        return "messages/conversation";
    }

    @PostMapping("/send")
    public String sendMessage(@RequestParam Long receiverId,
                              @RequestParam String content) {
        messageService.sendMessage(authService.getCurrentUser().getId(), receiverId, content);
        return "redirect:/messages/" + receiverId;
    }

    @GetMapping("/inbox")
    public String getReceivedMessages(Model model){
        User user = authService.getCurrentUser();
        List<Message> messages = messageService.receivedMessages(user.getId());
        model.addAttribute("receivedMessages", messages);
        model.addAttribute("unreadCount",messageService.countUnreadMessages(user.getId()));
        return "messages/inbox";
    }
}

