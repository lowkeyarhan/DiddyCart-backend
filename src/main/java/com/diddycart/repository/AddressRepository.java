package com.diddycart.repository;

import com.diddycart.models.Address;
import com.diddycart.models.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AddressRepository extends JpaRepository<Address, Long> {

    // Fetch all addresses for a specific logged-in user
    List<Address> findByUser(User user);
}