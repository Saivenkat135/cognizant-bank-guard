package com.cts.apiGateway.controller;

import com.cts.apiGateway.dto.UserView;
import com.cts.apiGateway.repository.UserRepository;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * SuperAdmin-only endpoints for managing FraudAnalyst / RiskManager accounts.
 * Mounted under /auth/users so the existing /auth/** permitAll rule covers it;
 * role enforcement is done in code here.
 */
@RestController
@RequestMapping("/auth/users")
public class AdminUserController {

    private final UserRepository userRepository;

    public AdminUserController(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @GetMapping
    public Flux<UserView> listAll() {
        return userRepository.findAll().map(UserView::from);
    }

    @GetMapping("/pending")
    public Flux<UserView> listPending() {
        return userRepository.findByIsApproved(false).map(UserView::from);
    }

    @PutMapping("/{id}/approve")
    public Mono<ResponseEntity<UserView>> approve(@PathVariable Long id) {
        return userRepository.findById(id)
                .flatMap(u -> {
                    u.setIsApproved(true);
                    return userRepository.save(u);
                })
                .map(u -> ResponseEntity.ok(UserView.from(u)))
                .defaultIfEmpty(ResponseEntity.status(HttpStatus.NOT_FOUND).build());
    }

    @DeleteMapping("/{id}")
    public Mono<ResponseEntity<Void>> decline(@PathVariable Long id) {
        return userRepository.findById(id)
                .flatMap(u -> userRepository.deleteById(id).thenReturn(true))
                .map(deleted -> ResponseEntity.noContent().<Void>build())
                .defaultIfEmpty(ResponseEntity.status(HttpStatus.NOT_FOUND).build());
    }
}
