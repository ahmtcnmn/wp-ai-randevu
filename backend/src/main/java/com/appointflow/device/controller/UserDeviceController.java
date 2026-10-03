package com.appointflow.device.controller;

import com.appointflow.common.ApiResponse;
import com.appointflow.device.dto.UserDeviceRequest;
import com.appointflow.device.dto.UserDeviceResponse;
import com.appointflow.device.service.UserDeviceService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/user-devices")
@RequiredArgsConstructor
public class UserDeviceController {

    private final UserDeviceService userDeviceService;

    @PostMapping
    public ResponseEntity<ApiResponse<UserDeviceResponse>> register(
            Authentication auth, @Valid @RequestBody UserDeviceRequest request) {
        return ResponseEntity.ok(ApiResponse.success(userDeviceService.register(auth.getName(), request)));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<UserDeviceResponse>>> myDevices(Authentication auth) {
        return ResponseEntity.ok(ApiResponse.success(userDeviceService.myDevices(auth.getName())));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> delete(Authentication auth, @PathVariable Long id) {
        userDeviceService.delete(auth.getName(), id);
        return ResponseEntity.ok(ApiResponse.success(null));
    }
}
