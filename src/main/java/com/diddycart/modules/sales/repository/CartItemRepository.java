package com.diddycart.modules.sales.repository;

import com.diddycart.modules.sales.models.CartItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface CartItemRepository extends JpaRepository<CartItem, Long> {

    // Delete a cart item based on cart ID and product ID
    void deleteByCartIdAndProductId(Long cartId, Long productId);
}