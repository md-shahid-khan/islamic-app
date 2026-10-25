package com.codominator.angusmasail.backend.repository;

import com.codominator.angusmasail.backend.entity.ImamResource;
import com.codominator.angusmasail.backend.entity.ResourceType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface ImamResourceRepository extends JpaRepository<ImamResource, UUID> {
    List<ImamResource> findByImamProfileIdOrderByCreatedAtDesc(UUID imamProfileId);
    List<ImamResource> findByImamProfileIdAndTypeOrderByCreatedAtDesc(UUID imamProfileId, ResourceType type);
    List<ImamResource> findAllByOrderByCreatedAtDesc();
}
