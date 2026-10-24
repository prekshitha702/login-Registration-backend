package com.microservices.registration.service;

import com.microservices.registration.dto.RegisterRequest;
import com.microservices.registration.entity.User;
import com.microservices.registration.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.Map;

@Service
public class RegistrationService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Autowired
    public RegistrationService(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional
    public Map<String, Object> registerUser(RegisterRequest request) {
        if (userRepository.existsByUsername(request.getUsername().trim())) {
            throw new IllegalArgumentException("Username '" + request.getUsername().trim() + "' is already registered.");
        }

        if (userRepository.existsByEmail(request.getEmail().trim().toLowerCase())) {
            throw new IllegalArgumentException("Email '" + request.getEmail().trim().toLowerCase() + "' is already in use.");
        }

        String hashedPassword = passwordEncoder.encode(request.getPassword());

        User user = new User(
                request.getUsername().trim(),
                request.getEmail().trim().toLowerCase(),
                hashedPassword,
                request.getFullName().trim()
        );

        User savedUser = userRepository.save(user);

        Map<String, Object> responseData = new HashMap<>();
        responseData.put("id", savedUser.getId());
        responseData.put("username", savedUser.getUsername());
        responseData.put("email", savedUser.getEmail());
        responseData.put("fullName", savedUser.getFullName());
        responseData.put("createdAt", savedUser.getCreatedAt());

        return responseData;
    }
}
