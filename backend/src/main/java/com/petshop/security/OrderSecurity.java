package com.petshop.security;

import com.petshop.repository.PetOrderRepository;
import com.petshop.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;

@Component("orderSecurity")
@RequiredArgsConstructor
public class OrderSecurity {

    private final UserRepository userRepository;
    private final PetOrderRepository petOrderRepository;

    /** El usuario autenticado es el dueño del userId indicado. */
    public boolean isSelf(Long userId, Authentication auth) {
        return userId != null && userRepository.findByEmail(auth.getName())
                .map(user -> user.getId().equals(userId))
                .orElse(false);
    }

    /** El pedido pertenece al usuario autenticado. */
    public boolean ownsOrder(Long orderId, Authentication auth) {
        return orderId != null && petOrderRepository.existsByIdAndUserEmail(orderId, auth.getName());
    }
}