package com.ventureai.service;

import com.ventureai.dto.auth.AuthResponse;
import com.ventureai.dto.auth.ForgotPasswordRequest;
import com.ventureai.dto.auth.LoginRequest;
import com.ventureai.dto.auth.RegisterRequest;
import com.ventureai.dto.auth.ResetPasswordRequest;
import com.ventureai.dto.common.MessageResponse;
import com.ventureai.entity.User;
import com.ventureai.entity.enums.Role;
import com.ventureai.exception.BadRequestException;
import com.ventureai.repository.UserRepository;
import com.ventureai.security.AuthenticatedUser;
import com.ventureai.security.JwtService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final AuthenticationManager authenticationManager;

    @Transactional
    public AuthResponse register(RegisterRequest request) {
        if (!request.password().equals(request.confirmPassword())) {
            throw new BadRequestException("Password and confirm password do not match.");
        }
        if (userRepository.existsByEmail(request.email().toLowerCase())) {
            throw new BadRequestException("An account with this email already exists.");
        }

        User user = User.builder()
                .name(request.fullName())
                .email(request.email().toLowerCase())
                .password(passwordEncoder.encode(request.password()))
                .userType(request.resolvedUserType())
                .role(Role.ROLE_USER)
                .build();

        User saved = userRepository.save(user);
        return buildAuthResponse(saved);
    }

    public AuthResponse login(LoginRequest request) {
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.email().toLowerCase(), request.password())
        );
        User user = userRepository.findByEmail(request.email().toLowerCase())
                .orElseThrow(() -> new BadRequestException("Invalid email or password."));
        return buildAuthResponse(user);
    }

    /**
     * Academic-project note: there is no email/SMTP provider wired up, so
     * instead of silently pretending to send an email, we generate the
     * reset token and return it directly in the response (clearly marked as
     * dev-mode). In a production deployment this would be emailed to the
     * user and never returned via the API.
     */
    @Transactional
    public MessageResponse forgotPassword(ForgotPasswordRequest request) {
        return userRepository.findByEmail(request.email().toLowerCase())
                .map(user -> {
                    String token = UUID.randomUUID().toString();
                    user.setResetToken(token);
                    user.setResetTokenExpiry(LocalDateTime.now().plusMinutes(30));
                    userRepository.save(user);
                    log.info("Password reset requested for {} — dev-mode token: {}", user.getEmail(), token);
                    return new MessageResponse(
                            "DEV MODE (no email service configured): reset token = " + token +
                                    ". In production this token is emailed, never returned by the API."
                    );
                })
                .orElse(new MessageResponse("If an account exists for that email, a reset link has been sent."));
    }

    @Transactional
    public MessageResponse resetPassword(ResetPasswordRequest request) {
        User user = userRepository.findAll().stream()
                .filter(u -> request.token().equals(u.getResetToken()))
                .findFirst()
                .orElseThrow(() -> new BadRequestException("Invalid or expired reset token."));

        if (user.getResetTokenExpiry() == null || user.getResetTokenExpiry().isBefore(LocalDateTime.now())) {
            throw new BadRequestException("Reset token has expired. Please request a new one.");
        }

        user.setPassword(passwordEncoder.encode(request.newPassword()));
        user.setResetToken(null);
        user.setResetTokenExpiry(null);
        userRepository.save(user);
        return new MessageResponse("Password has been reset successfully. You can now log in.");
    }

    private AuthResponse buildAuthResponse(User user) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("uid", user.getId());
        claims.put("name", user.getName());
        claims.put("userType", user.getUserType().name());
        String token = jwtService.generateToken(new AuthenticatedUser(user), claims);
        return new AuthResponse(token, user.getId(), user.getName(), user.getEmail(), user.getUserType().name());
    }
}
