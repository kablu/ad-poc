package com.company.ra.repository;

import com.company.ra.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * Repository interface for User entity
 */
@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    /**
     * Find user by username
     *
     * @param username Username
     * @return Optional User
     */
    Optional<User> findByUsername(String username);

    /**
     * Check if user exists by username
     *
     * @param username Username
     * @return true if exists
     */
    boolean existsByUsername(String username);

    /**
     * Find user by email
     *
     * @param email Email address
     * @return Optional User
     */
    Optional<User> findByEmail(String email);
}
