package com.harish.impl;

import com.harish.dto.auth.AuthResponse;
import com.harish.dto.auth.LoginRequest;
import com.harish.dto.auth.SignupRequest;
import com.harish.entity.User;
import com.harish.error.BadRequestException;
import com.harish.mapper.UserMapper;
import com.harish.repository.UserRepository;
import com.harish.security.AuthUtil;
import com.harish.security.JwtUserPrincipal;
import com.harish.service.AuthService;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.ArrayList;

@Service
@RequiredArgsConstructor
@FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
public class AuthServiceImpl implements AuthService {

    UserRepository userRepository;
    UserMapper userMapper;
    PasswordEncoder passwordEncoder;
    AuthUtil authUtil;
    AuthenticationManager authenticationManager;

    @Override
    public AuthResponse signup(SignupRequest request) {

        userRepository.findByUsername(request.username()).ifPresent(user -> {
            throw new BadRequestException("User already exists with username: "+request.username());
        });
        User user = userMapper.toEntity(request);
        user.setPassword(passwordEncoder.encode(request.password()));
        user = userRepository.save(user);
        JwtUserPrincipal jwtUserPrincipal = new JwtUserPrincipal(user.getId(), user.getName(),
                user.getUsername(), null,  new ArrayList<>());
        String token = authUtil.generateAccessToken(jwtUserPrincipal);
        return new AuthResponse(token, userMapper.toUserProfileResponse(jwtUserPrincipal));

    }

    @Override
    public AuthResponse login(LoginRequest request) {

        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.username(), request.password())
        );
        JwtUserPrincipal user = (JwtUserPrincipal) authentication.getPrincipal();
        String token = authUtil.generateAccessToken(user);
        return new AuthResponse(token, userMapper.toUserProfileResponse(user));

    }

}