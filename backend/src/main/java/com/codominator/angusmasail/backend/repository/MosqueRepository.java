package com.codominator.angusmasail.backend.repository;

import com.codominator.angusmasail.backend.entity.Mosque;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface MosqueRepository extends JpaRepository<Mosque, UUID> {

    List<Mosque> findAllByOrderByCreatedAtDesc();

    List<Mosque> findByCityContainingIgnoreCaseOrNameContainingIgnoreCase(String city, String name);

    List<Mosque> findByCurrentImamIsNull();

    Optional<Mosque> findByNameIgnoreCaseAndCityIgnoreCase(String name, String city);

    @Query("SELECT m FROM Mosque m LEFT JOIN m.currentImam i WHERE i IS NULL OR i.verified = false ORDER BY m.name ASC")
    List<Mosque> findAvailableMosques();
}
