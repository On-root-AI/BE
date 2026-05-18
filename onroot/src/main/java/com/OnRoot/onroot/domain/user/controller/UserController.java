package com.OnRoot.onroot.domain.user.controller;

import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.OnRoot.onroot.domain.user.dto.AuthResponse;
import com.OnRoot.onroot.domain.user.dto.LoginRequest;
import com.OnRoot.onroot.domain.user.dto.SignupRequest;
import com.OnRoot.onroot.domain.user.dto.UserResponse;
import com.OnRoot.onroot.domain.user.dto.UserUpdateRequest;
import com.OnRoot.onroot.domain.user.entity.User;
import com.OnRoot.onroot.domain.user.service.UserService;
import com.OnRoot.onroot.global.exception.UnauthorizedException;
import com.OnRoot.onroot.global.response.ErrorResponse;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@Tag(name = "User", description = "회원가입/로그인 API")
@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @Operation(summary = "회원가입", description = "이메일, 비밀번호, 닉네임으로 로컬 회원가입을 진행합니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "회원가입 성공"),
            @ApiResponse(responseCode = "400", description = "잘못된 요청",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "500", description = "서버 오류",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @PostMapping("/signup")
    @ResponseStatus(HttpStatus.CREATED)
    public void signup(@Valid @RequestBody SignupRequest request) {
        userService.signup(request);
    }

    @Operation(summary = "로그인", description = "이메일+비밀번호로 로그인하고 액세스 토큰을 반환합니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "로그인 성공"),
            @ApiResponse(responseCode = "400", description = "잘못된 요청",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "401", description = "인증 실패",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @PostMapping("/login")
    public AuthResponse login(@Valid @RequestBody LoginRequest request) {
        return userService.login(request);
    }

        @Operation(summary = "내 정보 조회", description = "로그인한 사용자의 정보를 반환합니다.")
        @GetMapping("/me")
        public UserResponse getMe(Authentication auth) {
                return userService.getMe(resolveAuthenticatedUser(auth));
        }

        @Operation(summary = "내 정보 수정", description = "로그인한 사용자의 닉네임을 수정합니다.")
        @PatchMapping("/me")
        public UserResponse updateMe(Authentication auth, @RequestBody UserUpdateRequest req) {
                return userService.updateMe(resolveAuthenticatedUser(auth), req);
        }

        @Operation(summary = "회원 탈퇴", description = "로그인한 사용자를 삭제합니다.")
        @DeleteMapping("/me")
        @ResponseStatus(HttpStatus.NO_CONTENT)
        public void deleteMe(Authentication auth) {
                userService.deleteMe(resolveAuthenticatedUser(auth));
        }

        private User resolveAuthenticatedUser(Authentication auth) {
                if (auth == null || !(auth.getPrincipal() instanceof User user)) {
                        throw new UnauthorizedException("로그인이 필요합니다.");
                }
                return user;
        }
}
