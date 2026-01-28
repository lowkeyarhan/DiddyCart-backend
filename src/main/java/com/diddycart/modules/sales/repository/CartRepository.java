package com.diddycart.modules.sales.repository;

import com.diddycart.modules.sales.models.Cart;
import com.diddycart.modules.identity.models.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface CartRepository extends JpaRepository<Cart, Long> {

    // Find the cart belonging to a specific user
    Optional<Cart> findByUser(User user);
}