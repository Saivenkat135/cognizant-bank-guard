package com.cts.apiGateway.controller;

import com.cts.apiGateway.dto.AuthRequest;
import com.cts.apiGateway.dto.AuthResponse;
import com.cts.apiGateway.dto.RegisterRequest;
import com.cts.apiGateway.model.Role;
import com.cts.apiGateway.model.User;
import com.cts.apiGateway.repository.UserRepository;
import com.cts.apiGateway.security.JwtUtil;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/auth")
public class AuthController {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;

    public AuthController(UserRepository userRepository,
                          PasswordEncoder passwordEncoder,
                          JwtUtil jwtUtil) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtUtil = jwtUtil;
    }

    /**
     * Register a new user.
     * SuperAdmin signup is not allowed via this endpoint (seeded in DB).
     * FraudAnalyst / RiskManager are created with isApproved=false until SuperAdmin approves.
     */
    @PostMapping("/register")
    public Mono<ResponseEntity<AuthResponse>> register(@RequestBody RegisterRequest request) {

        Role roleEnum;
        try {
            roleEnum = Role.valueOf(request.getRole());
        } catch (IllegalArgumentException e) {
            return Mono.just(ResponseEntity.badRequest().body(
                    AuthResponse.builder()
                            .message("Invalid role. Allowed: FRAUD_ANALYST, RISK_MANAGER")
                            .build()
            ));
        }

        if (roleEnum == Role.SUPER_ADMIN) {
            return Mono.just(ResponseEntity.status(HttpStatus.FORBIDDEN).body(
                    AuthResponse.builder()
                            .message("SUPER_ADMIN signup is not allowed.")
                            .build()
            ));
        }

        return userRepository.findByUsername(request.getUsername())
                .flatMap(existing -> Mono.just(ResponseEntity.status(HttpStatus.CONFLICT).body(
                        AuthResponse.builder()
                                .message("Username already exists")
                                .build()
                )))
                .switchIfEmpty(
                        userRepository.save(
                                User.builder()
                                        .username(request.getUsername())
                                        .password(passwordEncoder.encode(request.getPassword()))
                                        .role(request.getRole())
                                        .isApproved(false)
                                        .build()
                        ).map(saved -> ResponseEntity.status(HttpStatus.CREATED).body(
                                AuthResponse.builder()
                                        .username(saved.getUsername())
                                        .role(saved.getRole())
                                        .message("Signup submitted. Wait for SuperAdmin approval.")
                                        .build()
                        ))
                );
    }

    /**
     * Login. Blocks users that are not yet approved.
     */
    @PostMapping("/login")
    public Mono<ResponseEntity<AuthResponse>> login(@RequestBody AuthRequest request) {
        return userRepository.findByUsername(request.getUsername())
                .flatMap(user -> {
                    if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
                        return Mono.just(ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(
                                AuthResponse.builder().message("Invalid username or password").build()
                        ));
                    }

                    boolean approved = Boolean.TRUE.equals(user.getIsApproved())
                            || "SUPER_ADMIN".equals(user.getRole());

                    if (!approved) {
                        return Mono.just(ResponseEntity.status(HttpStatus.FORBIDDEN).body(
                                AuthResponse.builder()
                                        .message("Account pending SuperAdmin approval.")
                                        .build()
                        ));
                    }

                    String token = jwtUtil.generateToken(user.getUsername(), user.getRole());
                    return Mono.just(ResponseEntity.ok(
                            AuthResponse.builder()
                                    .token(token)
                                    .username(user.getUsername())
                                    .role(user.getRole())
                                    .message("Login successful")
                                    .build()
                    ));
                })
                .switchIfEmpty(Mono.just(ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(
                        AuthResponse.builder()
                                .message("Invalid username or password")
                                .build()
                )));
    }
}
