package com.codominator.angusmasail.backend.repository;

import com.codominator.angusmasail.backend.entity.ImamProfile;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ImamProfileRepository extends JpaRepository<ImamProfile, UUID> {
    Optional<ImamProfile> findByUserId(UUID userId);
    List<ImamProfile> findByMasjidCityContainingIgnoreCaseOrMasjidNameContainingIgnoreCase(String city, String name);
    List<ImamProfile> findByFiqhSchoolContainingIgnoreCase(String fiqhSchool);
    List<ImamProfile> findByVerifiedFalse();
    List<ImamProfile> findByVerifiedTrue();
}
