package com.example.sakeec.controller;

import com.example.sakeec.dto.AdminLoginRequest;
import com.example.sakeec.dto.AdminLoginResponse;
import com.example.sakeec.service.AdminService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/admin")
public class AdminAuthController {

    private final AdminService adminService;

    public AdminAuthController(AdminService adminService) {
        this.adminService = adminService;
    }

    @PostMapping("/login")
    public ResponseEntity<AdminLoginResponse> login(@RequestBody @Valid AdminLoginRequest request) {
        return ResponseEntity.ok(adminService.login(request));
    }

    @PostMapping("/logout")
    public ResponseEntity<Map<String, String>> logout() {
        return ResponseEntity.ok(Map.of("message", "logged out"));
    }
}
