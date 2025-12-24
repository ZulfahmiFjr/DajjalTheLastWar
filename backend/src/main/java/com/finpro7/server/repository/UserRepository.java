package com.finpro7.server.repository;

import com.finpro7.server.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByUsername(String username);

    List<User> findTop100ByOrderByCoinsDesc();
}
