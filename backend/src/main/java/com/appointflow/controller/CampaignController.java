package com.appointflow.controller;

import com.appointflow.common.ApiResponse;
import com.appointflow.dto.*;
import com.appointflow.service.CampaignService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/campaigns")
@RequiredArgsConstructor
public class CampaignController {

    private final CampaignService campaignService;

    // ─── Slot Campaigns ────────────────────────────────────────────────────────

    @GetMapping("/slot")
    @PreAuthorize("hasAnyRole('OWNER', 'ADMIN', 'BRANCH_MANAGER')")
    public ResponseEntity<ApiResponse<List<SlotCampaignResponse>>> getSlotCampaigns() {
        return ResponseEntity.ok(ApiResponse.success(campaignService.getSlotCampaigns()));
    }

    @PostMapping("/slot/{id}/cancel")
    @PreAuthorize("hasAnyRole('OWNER', 'ADMIN', 'BRANCH_MANAGER')")
    public ResponseEntity<ApiResponse<SlotCampaignResponse>> cancelSlotCampaign(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success(campaignService.cancelSlotCampaign(id)));
    }

    // ─── Segment Campaigns ─────────────────────────────────────────────────────

    @GetMapping("/segment")
    @PreAuthorize("hasAnyRole('OWNER', 'ADMIN', 'BRANCH_MANAGER')")
    public ResponseEntity<ApiResponse<List<SegmentCampaignResponse>>> getSegmentCampaigns() {
        return ResponseEntity.ok(ApiResponse.success(campaignService.getSegmentCampaigns()));
    }

    @PostMapping("/segment")
    @PreAuthorize("hasAnyRole('OWNER', 'ADMIN', 'BRANCH_MANAGER')")
    public ResponseEntity<ApiResponse<SegmentCampaignResponse>> sendSegmentCampaign(
            @Valid @RequestBody SegmentCampaignRequest request,
            Authentication authentication) {
        return ResponseEntity.ok(ApiResponse.success(
                campaignService.sendSegmentCampaign(request, authentication.getName())));
    }
}
