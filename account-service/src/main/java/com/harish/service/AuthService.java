package com.harish.service;


import com.harish.dto.auth.AuthResponse;
import com.harish.dto.auth.LoginRequest;
import com.harish.dto.auth.SignupRequest;

public interface AuthService {

    AuthResponse signup(SignupRequest request);

    AuthResponse login(LoginRequest request);

}
