package com.service.user;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {

    /**
     * Looks up a user by their unique username. Returns an empty {@link Optional}
     * when no such user exists — used during login/authentication.
     */
    Optional<User> findByUsername(String username);

    /**
     * Checks whether a username is already taken, without loading the row.
     * Used to reject duplicate registrations (409) before insert.
     */
    boolean existsByUsername(String username);
}
