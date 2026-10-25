package com.codominator.angusmasail.backend.repository;

import com.codominator.angusmasail.backend.entity.ChatMessage;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface ChatMessageRepository extends JpaRepository<ChatMessage, UUID> {
    List<ChatMessage> findByChatRequestIdOrderBySentAtAsc(UUID chatRequestId);
    Page<ChatMessage> findByChatRequestIdOrderBySentAtDesc(UUID chatRequestId, Pageable pageable);

    @Modifying
    @Query("DELETE FROM ChatMessage cm WHERE cm.chatRequest.id = :chatRequestId")
    int deleteByChatRequestId(@Param("chatRequestId") UUID chatRequestId);

    @Modifying
    @Query("DELETE FROM ChatMessage cm WHERE cm.chatRequest.id IN (SELECT cr.id FROM ChatRequest cr WHERE cr.imamProfile.id = :imamProfileId)")
    int deleteByImamProfileId(@Param("imamProfileId") UUID imamProfileId);
}
