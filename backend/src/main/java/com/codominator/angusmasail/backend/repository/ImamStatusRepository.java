package com.codominator.angusmasail.backend.repository;

import com.codominator.angusmasail.backend.entity.ImamStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Repository
public interface ImamStatusRepository extends JpaRepository<ImamStatus, UUID> {
    List<ImamStatus> findByImamProfileIdAndExpiresAtAfterOrderByCreatedAtDesc(UUID imamProfileId, LocalDateTime now);
    List<ImamStatus> findByExpiresAtBefore(LocalDateTime now);
    List<ImamStatus> findByImamProfileId(UUID imamProfileId);

    @Modifying
    @Query("DELETE FROM ImamStatus s WHERE s.imamProfile.id = :imamProfileId")
    int deleteByImamProfileId(@Param("imamProfileId") UUID imamProfileId);
}
