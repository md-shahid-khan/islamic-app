package com.codominator.angusmasail.backend.repository;

import com.codominator.angusmasail.backend.entity.MasailQuery;
import com.codominator.angusmasail.backend.entity.QueryStatus;
import com.codominator.angusmasail.backend.entity.QueryTag;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface MasailQueryRepository extends JpaRepository<MasailQuery, UUID> {

    List<MasailQuery> findByUserIdOrderByCreatedAtDesc(UUID userId);

    List<MasailQuery> findByImamProfileIdOrderByIsUrgentDescCreatedAtAsc(UUID imamProfileId);

    List<MasailQuery> findByImamProfileIdAndStatusOrderByIsUrgentDescCreatedAtAsc(UUID imamProfileId, QueryStatus status);

    List<MasailQuery> findByImamProfileIdAndTagAndStatusOrderByIsUrgentDescCreatedAtAsc(UUID imamProfileId, QueryTag tag, QueryStatus status);

    @Query("SELECT m FROM MasailQuery m WHERE m.imamProfile.id = :imamId AND m.isPublicFaq = true AND LOWER(m.questionText) LIKE LOWER(CONCAT('%', :keyword, '%'))")
    List<MasailQuery> searchPublicFaqs(@Param("imamId") UUID imamId, @Param("keyword") String keyword, Pageable pageable);

    @Modifying
    @Query("DELETE FROM MasailQuery q WHERE q.imamProfile.id = :imamProfileId")
    int deleteByImamProfileId(@Param("imamProfileId") UUID imamProfileId);
}
