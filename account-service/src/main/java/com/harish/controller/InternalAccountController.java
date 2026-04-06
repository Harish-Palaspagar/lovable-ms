package com.harish.controller;

import com.harish.dto.PlanDto;
import com.harish.dto.UserDto;
import com.harish.error.ResourceNotFoundException;
import com.harish.mapper.UserMapper;
import com.harish.repository.UserRepository;
import com.harish.service.SubscriptionService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.Optional;

@RestController
@RequiredArgsConstructor
@RequestMapping("/internal/v1")
public class InternalAccountController {

    private final UserRepository userRepository;
    private final UserMapper userMapper;
    private final SubscriptionService subscriptionService;

    @GetMapping("/users/{id}")
    public UserDto getUserById(@PathVariable Long id) {

        return userRepository.findById(id)
                .map(userMapper::toUserDto)
                .orElseThrow(() -> new ResourceNotFoundException("User", id.toString()));

    }

    @GetMapping("/users/by-email")
    public Optional<UserDto> getUserByEmail(@RequestParam String email) {

        return userRepository.findByUsernameIgnoreCase(email)
                .map(userMapper::toUserDto);

    }

    @GetMapping("/billing/current-plan")
    public PlanDto getCurrentSubscribedPlan() {

        return subscriptionService.getCurrentSubscribedPlanByUser();

    }

}
