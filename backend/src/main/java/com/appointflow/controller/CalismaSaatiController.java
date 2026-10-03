package com.appointflow.controller;

import com.appointflow.dto.CalismaSaatiRequest;
import com.appointflow.entity.CalismaSaati;
import com.appointflow.service.CalismaSaatiService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/calisma-saatleri")
@RequiredArgsConstructor
public class CalismaSaatiController {

    private final CalismaSaatiService calismaSaatiService;

    @GetMapping("/{uzmanId}")
    public ResponseEntity<List<CalismaSaati>> saatleriGetir(@PathVariable Long uzmanId) {
        return ResponseEntity.ok(calismaSaatiService.uzmanSaatleriniGetir(uzmanId));
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('OWNER', 'ADMIN', 'BRANCH_MANAGER', 'STAFF')")
    public ResponseEntity<List<CalismaSaati>> saatleriKaydet(
            @RequestBody List<CalismaSaatiRequest> request,
            @AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity.ok(calismaSaatiService.saatleriKaydet(userDetails.getUsername(), request));
    }
}
