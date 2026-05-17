package com.OnRoot.onroot.domain.user.service;

import java.time.LocalDateTime;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.OnRoot.onroot.domain.user.dto.AuthResponse;
import com.OnRoot.onroot.domain.user.dto.LoginRequest;
import com.OnRoot.onroot.domain.user.dto.SignupRequest;
import com.OnRoot.onroot.domain.user.dto.UserResponse;
import com.OnRoot.onroot.domain.user.dto.UserUpdateRequest;
import com.OnRoot.onroot.domain.user.entity.User;
import com.OnRoot.onroot.domain.user.repository.UserRepository;
import com.OnRoot.onroot.global.exception.NotFoundException;
import com.OnRoot.onroot.global.security.JwtTokenProvider;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;

    @Transactional
    public void signup(SignupRequest request) {
        userRepository.findByEmail(request.email()).ifPresent(u -> {
            throw new IllegalArgumentException("이미 사용중인 이메일입니다.");
        });
        User user = User.builder()
                .email(request.email())
                .passwordHash(passwordEncoder.encode(request.password()))
                .nickname(request.nickname())
                .provider("local")
                .createdAt(LocalDateTime.now())
                .build();
        userRepository.save(user);
    }

    @Transactional(readOnly = true)
    public AuthResponse login(LoginRequest request) {
        User user = userRepository.findByEmail(request.email())
                .orElseThrow(() -> new NotFoundException("사용자를 찾을 수 없습니다."));
        if (!passwordEncoder.matches(request.password(), user.getPasswordHash())) {
            throw new IllegalArgumentException("이메일 또는 비밀번호가 올바르지 않습니다.");
        }
        String token = jwtTokenProvider.createToken(user.getId(), user.getEmail());
        return AuthResponse.of(token);
    }

    @Transactional(readOnly = true)
    public UserResponse getMe(User user) {
        return UserResponse.from(user);
    }

    @Transactional
    public UserResponse updateMe(User user, UserUpdateRequest req) {
        if (req.nickname() != null && !req.nickname().isBlank()) {
            user.updateNickname(req.nickname());
        }
        return UserResponse.from(user);
    }

    @Transactional
    public void deleteMe(User user) {
        userRepository.delete(user);
    }
}
