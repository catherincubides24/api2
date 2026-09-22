package com.petshop.repository;

import com.petshop.entity.PetOrder;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PetOrderRepository extends JpaRepository<PetOrder, Long> {

    Page<PetOrder> findByUserId(Long userId, Pageable pageable);

    Optional<PetOrder> findByTicketNumber(String ticketNumber);
}
