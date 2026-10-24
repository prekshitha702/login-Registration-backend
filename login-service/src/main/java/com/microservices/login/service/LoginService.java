package com.microservices.login.service;

import com.microservices.login.dto.LoginRequest;
import com.microservices.login.dto.LoginResponse;
import com.microservices.login.entity.JwtToken;
import com.microservices.login.entity.User;
import com.microservices.login.repository.JwtRepository;
import com.microservices.login.repository.UserRepository;
import io.jsonwebtoken.Claims;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@Service
public class LoginService {

    private final UserRepository userRepository;
    private final JwtRepository jwtRepository;
    private final JwtTokenProvider tokenProvider;
    private final PasswordEncoder passwordEncoder;

    @Autowired
    public LoginService(UserRepository userRepository,
                        JwtRepository jwtRepository,
                        JwtTokenProvider tokenProvider,
                        PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.jwtRepository = jwtRepository;
        this.tokenProvider = tokenProvider;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional
    public LoginResponse authenticate(LoginRequest request) {
        String inputIdentifier = request.getUsername().trim();

        // Check in usertable by username or email
        Optional<User> userOptional = userRepository.findByUsername(inputIdentifier);
        if (userOptional.isEmpty()) {
            userOptional = userRepository.findByEmail(inputIdentifier.toLowerCase());
        }

        if (userOptional.isEmpty()) {
            throw new IllegalArgumentException("Invalid credentials: user not found.");
        }

        User user = userOptional.get();

        // Verify password hash
        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            throw new IllegalArgumentException("Invalid credentials: password does not match.");
        }

        // Generate JWT token
        String token = tokenProvider.generateToken(user.getUsername(), user.getId(), user.getEmail(), user.getFullName());
        Date expiryDate = tokenProvider.getExpirationDateFromToken(token);

        LocalDateTime issuedAt = LocalDateTime.now();
        LocalDateTime expiresAt = expiryDate.toInstant().atZone(ZoneId.systemDefault()).toLocalDateTime();

        // Persist issued token in jwttable
        JwtToken jwtEntity = new JwtToken(user.getUsername(), token, issuedAt, expiresAt);
        jwtRepository.save(jwtEntity);

        return new LoginResponse(
                token,
                user.getId(),
                user.getUsername(),
                user.getEmail(),
                user.getFullName(),
                issuedAt,
                expiresAt
        );
    }

    @Transactional
    public void logout(String token) {
        if (token != null && token.startsWith("Bearer ")) {
            token = token.substring(7);
        }

        if (token != null && !token.isBlank()) {
            jwtRepository.revokeToken(token);
        }
    }

    @Transactional(readOnly = true)
    public Map<String, Object> validateToken(String token) {
        if (token != null && token.startsWith("Bearer ")) {
            token = token.substring(7);
        }

        if (token == null || !tokenProvider.validateToken(token)) {
            throw new IllegalArgumentException("Token is invalid or expired.");
        }

        // Check if token has been revoked in jwttable
        Optional<JwtToken> recordedToken = jwtRepository.findByToken(token);
        if (recordedToken.isPresent() && recordedToken.get().isRevoked()) {
            throw new IllegalArgumentException("Token has been revoked/logged out.");
        }

        Claims claims = tokenProvider.getClaimsFromToken(token);
        String username = claims.getSubject();
        Long userId = claims.get("userId", Long.class);
        String email = claims.get("email", String.class);
        String fullName = claims.get("fullName", String.class);

        Map<String, Object> tokenInfo = new HashMap<>();
        tokenInfo.put("valid", true);
        tokenInfo.put("username", username);
        tokenInfo.put("userId", userId);
        tokenInfo.put("email", email);
        tokenInfo.put("fullName", fullName);
        tokenInfo.put("expiresAt", claims.getExpiration());

        return tokenInfo;
    }
}
