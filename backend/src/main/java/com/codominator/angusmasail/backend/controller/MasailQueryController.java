package com.codominator.angusmasail.backend.controller;

import com.codominator.angusmasail.backend.dto.MasailQueryDto;
import com.codominator.angusmasail.backend.entity.QueryTag;
import com.codominator.angusmasail.backend.entity.QueryStatus;
import com.codominator.angusmasail.backend.config.UserDetailsImpl;
import com.codominator.angusmasail.backend.service.LocalFileStorageService;
import com.codominator.angusmasail.backend.service.MasailQueryService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1")
public class MasailQueryController {

    @Autowired
    private MasailQueryService masailQueryService;

    @Autowired
    private LocalFileStorageService fileStorageService;

    @PostMapping("/queries/check-duplicate")
    public ResponseEntity<List<MasailQueryDto>> checkDuplicates(
            @RequestParam UUID imamProfileId,
            @RequestParam String questionText) {
        return ResponseEntity.ok(masailQueryService.checkDuplicates(imamProfileId, questionText));
    }

    @PostMapping(value = "/queries", consumes = {
            MediaType.MULTIPART_FORM_DATA_VALUE,
            MediaType.APPLICATION_JSON_VALUE,
            MediaType.APPLICATION_FORM_URLENCODED_VALUE
    })
    public ResponseEntity<MasailQueryDto> submitQuery(
            @RequestParam(required = false) QueryTag tag,
            @RequestParam String questionText,
            @RequestParam(required = false) String audioUrl,
            @RequestParam(required = false) UUID imamProfileId,
            @RequestPart(value = "audioFile", required = false) MultipartFile audioFile,
            @AuthenticationPrincipal UserDetailsImpl userDetails) {

        String finalAudioUrl = audioUrl;
        if (audioFile != null && !audioFile.isEmpty()) {
            finalAudioUrl = fileStorageService.storeFile(audioFile, "audio");
        }

        UUID userId = userDetails != null ? userDetails.getId() : null;
        return ResponseEntity.ok(masailQueryService.submitQuery(userId, imamProfileId, tag, questionText, finalAudioUrl));
    }

    @GetMapping("/queries/my")
    public ResponseEntity<List<MasailQueryDto>> getMyQueries(@AuthenticationPrincipal UserDetailsImpl userDetails) {
        UUID userId = userDetails != null ? userDetails.getId() : null;
        return ResponseEntity.ok(masailQueryService.getUserQueries(userId));
    }

    @GetMapping("/imam/queries")
    @PreAuthorize("hasAnyRole('IMAM', 'ADMIN')")
    public ResponseEntity<List<MasailQueryDto>> getImamQueries(
            @RequestParam(required = false) QueryTag tag,
            @RequestParam(required = false) QueryStatus status,
            @AuthenticationPrincipal UserDetailsImpl userDetails) {
        UUID imamUserId = userDetails != null ? userDetails.getId() : null;
        return ResponseEntity.ok(masailQueryService.getImamQueries(imamUserId, tag, status));
    }

    @PostMapping("/imam/queries/{queryId}/accept")
    @PreAuthorize("hasAnyRole('IMAM', 'ADMIN')")
    public ResponseEntity<MasailQueryDto> acceptQuery(
            @PathVariable UUID queryId,
            @AuthenticationPrincipal UserDetailsImpl userDetails) {
        UUID imamUserId = userDetails != null ? userDetails.getId() : null;
        return ResponseEntity.ok(masailQueryService.acceptQuery(imamUserId, queryId));
    }

    @PostMapping("/imam/queries/{queryId}/resolve")
    @PreAuthorize("hasAnyRole('IMAM', 'ADMIN')")
    public ResponseEntity<MasailQueryDto> resolveQuery(
            @PathVariable UUID queryId,
            @RequestParam String replyText,
            @RequestParam(required = false) String replyAudioUrl,
            @RequestParam(required = false, defaultValue = "false") boolean isPublicFaq,
            @RequestPart(value = "replyAudioFile", required = false) MultipartFile replyAudioFile,
            @AuthenticationPrincipal UserDetailsImpl userDetails) {

        String finalReplyAudio = replyAudioUrl;
        if (replyAudioFile != null && !replyAudioFile.isEmpty()) {
            finalReplyAudio = fileStorageService.storeFile(replyAudioFile, "audio");
        }

        UUID imamUserId = userDetails != null ? userDetails.getId() : null;
        return ResponseEntity.ok(masailQueryService.resolveQuery(imamUserId, queryId, replyText, finalReplyAudio, isPublicFaq));
    }
}
