package com.traceusage.traceusage.auth.service;

import com.traceusage.traceusage.auth.dto.LoginRequest;
import com.traceusage.traceusage.auth.dto.LoginResponse;
import com.traceusage.traceusage.auth.dto.RegisterRequest;
import com.traceusage.traceusage.auth.dto.RegisterResponse;

public interface AuthService {

    RegisterResponse register(RegisterRequest request);

    LoginResponse login(LoginRequest request);
}
