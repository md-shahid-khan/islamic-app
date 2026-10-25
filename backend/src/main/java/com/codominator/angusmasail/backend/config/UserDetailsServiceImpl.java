package com.codominator.angusmasail.backend.config;

import com.codominator.angusmasail.backend.entity.User;
import com.codominator.angusmasail.backend.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
public class UserDetailsServiceImpl implements UserDetailsService {

    @Autowired
    private UserRepository userRepository;

    @Override
    @Transactional
    public UserDetails loadUserByUsername(String idOrPhone) throws UsernameNotFoundException {
        User user;
        try {
            UUID userId = UUID.fromString(idOrPhone);
            user = userRepository.findById(userId)
                    .orElseThrow(() -> new UsernameNotFoundException("User not found with id: " + idOrPhone));
        } catch (IllegalArgumentException e) {
            user = userRepository.findByPhone(idOrPhone)
                    .orElseThrow(() -> new UsernameNotFoundException("User not found with phone: " + idOrPhone));
        }

        return UserDetailsImpl.build(user);
    }
}
