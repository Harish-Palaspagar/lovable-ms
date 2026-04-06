package com.harish.impl;

import com.harish.entity.User;
import com.harish.repository.UserRepository;
import com.harish.security.JwtUserPrincipal;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.ArrayList;

@RequiredArgsConstructor
@Service
public class UserService implements UserDetailsService {

    private final UserRepository userRepository;

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {

        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("User not found: " + username));
        return new JwtUserPrincipal(
                user.getId(),
                user.getName(),
                user.getUsername(),
                user.getPassword(),
                new ArrayList<>()
        );

    }

}
