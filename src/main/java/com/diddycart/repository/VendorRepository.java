package com.diddycart.repository;

import com.diddycart.models.Vendor;
import com.diddycart.models.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface VendorRepository extends JpaRepository<Vendor, Long> {

    // Find the vendor profile associated with a user
    Optional<Vendor> findByUser(User user);

    // find vendor by user id
    Optional<Vendor> findByUserId(Long id);

    // Check if GSTIN already exists
    boolean existsByGstin(String gstin);
}