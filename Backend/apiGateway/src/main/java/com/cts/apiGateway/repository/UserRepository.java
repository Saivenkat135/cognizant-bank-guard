package com.cts.apiGateway.repository;

import com.cts.apiGateway.model.User;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Repository
public interface UserRepository extends ReactiveCrudRepository<User, Long> {

    Mono<User> findByUsername(String username);

    Flux<User> findByRole(String role);

    Flux<User> findByIsApproved(Boolean isApproved);
}
