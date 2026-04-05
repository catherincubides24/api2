package com.petshop.repository;

import com.petshop.entity.PetOrder;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PetOrderRepository extends JpaRepository<PetOrder, Long> {

    List<PetOrder> findByUserId(Long userId);
}
