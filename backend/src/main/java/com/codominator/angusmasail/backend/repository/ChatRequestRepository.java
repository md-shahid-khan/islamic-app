package com.codominator.angusmasail.backend.repository;

import com.codominator.angusmasail.backend.entity.ChatRequest;
import com.codominator.angusmasail.backend.entity.ChatRequestStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface ChatRequestRepository extends JpaRepository<ChatRequest, UUID> {
    List<ChatRequest> findByUserIdOrderByCreatedAtDesc(UUID userId);
    List<ChatRequest> findByImamProfileIdAndStatusOrderByCreatedAtDesc(UUID imamProfileId, ChatRequestStatus status);
    List<ChatRequest> findByImamProfileIdAndStatusOrderByCreatedAtAsc(UUID imamProfileId, ChatRequestStatus status);
    List<ChatRequest> findByImamProfileIdOrderByCreatedAtDesc(UUID imamProfileId);

    @Modifying
    @Query("DELETE FROM ChatRequest cr WHERE cr.imamProfile.id = :imamProfileId")
    int deleteByImamProfileId(@Param("imamProfileId") UUID imamProfileId);
}
