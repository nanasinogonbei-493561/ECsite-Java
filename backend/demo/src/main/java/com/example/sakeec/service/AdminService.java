package com.example.sakeec.service;

import com.example.sakeec.dto.AdminLoginRequest;
import com.example.sakeec.dto.AdminLoginResponse;

public interface AdminService {
    AdminLoginResponse login(AdminLoginRequest request);
}
