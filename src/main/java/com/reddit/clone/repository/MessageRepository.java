package com.reddit.clone.repository;

import com.reddit.clone.entity.Message;
import com.reddit.clone.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface MessageRepository extends JpaRepository<Message, Long> {
    List<Message> findBySenderAndReceiverOrReceiverAndSenderOrderBySentAtAsc(
            User sender1, User receiver1, User sender2, User receiver2
    );

    @Query("SELECT m FROM Message m " +
            "WHERE (m.sender.id = :userId AND m.receiver.id = :receiverId) " +
            "   OR (m.sender.id = :receiverId AND m.receiver.id = :userId) " +
            "ORDER BY m.sentAt ASC")
    List<Message> findConversationBetweenUsers(@Param("userId") Long userId,
                                               @Param("receiverId") Long receiverId);

    @Query("SELECT m FROM Message m " +
            "WHERE m.receiver.id = :userId " +
            "ORDER BY m.sentAt DESC")
    List<Message> findAllReceivedMessages(@Param("userId") Long userId);

    @Query("""
    SELECT m FROM Message m
    WHERE m.id IN (
        SELECT MAX(m2.id) FROM Message m2
        WHERE m2.receiver.id = :userId
        GROUP BY m2.sender.id
    )
    ORDER BY m.sentAt DESC""")
    List<Message> findLatestMessagesFromEachSender(@Param("userId") Long userId);

    int countByReceiverIdAndIsReadFalse(Long receiverId);
}
