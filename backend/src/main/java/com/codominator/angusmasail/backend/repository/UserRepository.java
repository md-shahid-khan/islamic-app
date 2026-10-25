package com.codominator.angusmasail.backend.repository;

import com.codominator.angusmasail.backend.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface UserRepository extends JpaRepository<User, UUID> {
    Optional<User> findByPhone(String phone);
    Optional<User> findByEmail(String email);
    Optional<User> findByUsername(String username);
    
    boolean existsByPhone(String phone);
    boolean existsByEmail(String email);
    boolean existsByUsername(String username);

    List<User> findBySelectedImamId(UUID selectedImamId);

    @Modifying
    @Query("UPDATE User u SET u.selectedImam = null WHERE u.selectedImam.id = :imamProfileId")
    int unlinkFollowersByImamProfileId(@Param("imamProfileId") UUID imamProfileId);
}
