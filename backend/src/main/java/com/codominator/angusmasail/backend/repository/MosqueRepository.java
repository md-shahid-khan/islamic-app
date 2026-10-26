package com.codominator.angusmasail.backend.repository;

import com.codominator.angusmasail.backend.entity.Mosque;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface MosqueRepository extends JpaRepository<Mosque, UUID> {

    List<Mosque> findByVerifiedTrueOrderByCreatedAtDesc();

    List<Mosque> findByVerifiedFalseOrderByCreatedAtDesc();

    @Query("SELECT m FROM Mosque m WHERE m.verified = true AND (LOWER(m.city) LIKE LOWER(CONCAT('%', :search, '%')) OR LOWER(m.name) LIKE LOWER(CONCAT('%', :search, '%'))) ORDER BY m.createdAt DESC")
    List<Mosque> searchVerifiedMosques(@Param("search") String search);

    Optional<Mosque> findByNameIgnoreCaseAndCityIgnoreCase(String name, String city);

    @Query("SELECT DISTINCT m FROM Mosque m WHERE m.verified = true OR (m.registeredBy IS NOT NULL AND m.registeredBy.id = :userId) ORDER BY m.name ASC")
    List<Mosque> findAvailableMosquesForUser(@Param("userId") UUID userId);

    @Query("SELECT m FROM Mosque m WHERE m.verified = true ORDER BY m.name ASC")
    List<Mosque> findAvailableMosques();
}
