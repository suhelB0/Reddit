package com.reddit.clone.service;

import com.reddit.clone.entity.Message;
import com.reddit.clone.entity.User;
import com.reddit.clone.repository.MessageRepository;
import com.reddit.clone.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;

@Service
@RequiredArgsConstructor
public class MessageService {

    private final MessageRepository messageRepository;
    private final UserRepository userRepository;

    public List<Message> getConversation(Long userId1, Long userId2) {
        User u1 = userRepository.findById(userId1).orElseThrow();
        User u2 = userRepository.findById(userId2).orElseThrow();
        List<Message> messages = messageRepository.findConversationBetweenUsers(u1.getId(), u2.getId());
        messages.stream()
                .filter(m -> m.getReceiver().getId().equals(userId1) && !m.isRead())
                .forEach(m -> m.setRead(true));
        return  messageRepository.saveAll(messages);
    }

    public void sendMessage(Long senderId, Long receiverId, String content) {
        User sender = userRepository.findById(senderId).orElseThrow();
        User receiver = userRepository.findById(receiverId).orElseThrow();

        Message message = new Message();
        message.setSender(sender);
        message.setReceiver(receiver);
        message.setContent(content);
        message.setRead(false);
        message.setSentAt(Instant.now());
        messageRepository.save(message);
    }

    public List<Message> receivedMessages(Long userId){
        return messageRepository.findLatestMessagesFromEachSender(userId);
    }

    public int countUnreadMessages(Long id){
        return messageRepository.countByReceiverIdAndIsReadFalse(id);
    }
}
