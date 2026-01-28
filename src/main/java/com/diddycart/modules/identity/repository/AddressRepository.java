package com.diddycart.modules.identity.repository;

import com.diddycart.modules.identity.models.Address;
import com.diddycart.modules.identity.models.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AddressRepository extends JpaRepository<Address, Long> {

    // Fetch all addresses for a specific logged-in user
    List<Address> findByUser(User user);
}