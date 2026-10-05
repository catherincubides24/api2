package com.petshop.repository;

import com.petshop.entity.UserSession;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserSessionRepository extends JpaRepository<UserSession, Long> {

    Optional<UserSession> findByUserEmail(String email);
}