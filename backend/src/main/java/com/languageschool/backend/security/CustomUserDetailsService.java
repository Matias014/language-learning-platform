package com.languageschool.backend.security;

import com.languageschool.backend.entity.User;
import com.languageschool.backend.repository.UserRepository;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
public class CustomUserDetailsService implements UserDetailsService {

    private final UserRepository repo;

    public CustomUserDetailsService(UserRepository repo) {
        this.repo = repo;
    }

    @Override
    public UserDetails loadUserByUsername(String usernameOrEmail) throws UsernameNotFoundException {
        String v = usernameOrEmail == null ? "" : usernameOrEmail.trim();
        User u = v.contains("@")
                ? repo.findByEmail(v).orElseThrow(() -> new UsernameNotFoundException("User not found"))
                : repo.findByLogin(v).orElseThrow(() -> new UsernameNotFoundException("User not found"));
        return org.springframework.security.core.userdetails.User
                .withUsername(u.getLogin())
                .password(u.getPasswordHash())
                .roles(u.getRole().name().toUpperCase())
                .build();
    }
}
