package com.diddycart.modules.identity.repository;

import com.diddycart.modules.identity.models.User;
import com.diddycart.modules.identity.models.UserRole;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.List;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    // Find user by email
    Optional<User> findByEmail(String email);

    // Check if a user exists by email
    boolean existsByEmail(String email);

    // ADMIN FEATURE: Find all Vendors
    List<User> findByRole(UserRole role);

    // ADMIN FEATURE: Search user by name (Partial match)
    List<User> findByNameContainingIgnoreCase(String name);
}