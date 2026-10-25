package com.codominator.angusmasail.backend.service;

import com.codominator.angusmasail.backend.dto.BannerDto;
import com.codominator.angusmasail.backend.entity.Banner;
import com.codominator.angusmasail.backend.repository.BannerRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class BannerService {

    private final BannerRepository bannerRepository;

    public List<BannerDto> getBanners() {
        return bannerRepository.findAllByOrderByDisplayOrderAscCreatedAtDesc()
                .stream()
                .limit(4)
                .map(this::mapToDto)
                .collect(Collectors.toList());
    }

    @Transactional
    public BannerDto createBanner(String imageUrl, String title, String actionUrl) {
        long currentCount = bannerRepository.count();
        if (currentCount >= 4) {
            throw new IllegalStateException("Maximum of 4 banners allowed. Please delete an existing banner before adding a new one.");
        }

        Banner banner = Banner.builder()
                .imageUrl(imageUrl)
                .title(title)
                .actionUrl(actionUrl)
                .displayOrder((int) currentCount + 1)
                .build();

        Banner saved = bannerRepository.save(banner);
        log.info("Created new home banner: {}", saved.getId());
        return mapToDto(saved);
    }

    @Transactional
    public void deleteBanner(UUID id) {
        bannerRepository.deleteById(id);
        log.info("Deleted banner: {}", id);
    }

    public BannerDto mapToDto(Banner banner) {
        return BannerDto.builder()
                .id(banner.getId())
                .imageUrl(banner.getImageUrl())
                .title(banner.getTitle())
                .actionUrl(banner.getActionUrl())
                .displayOrder(banner.getDisplayOrder())
                .createdAt(banner.getCreatedAt())
                .build();
    }
}
