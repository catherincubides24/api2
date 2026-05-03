package com.petshop.service;

import com.petshop.dto.auth.AuthRequest;
import com.petshop.dto.auth.AuthResponse;
import com.petshop.dto.auth.RegisterRequest;
import com.petshop.entity.Role;
import com.petshop.entity.User;
import com.petshop.exception.BadRequestException;
import com.petshop.repository.UserRepository;
import com.petshop.security.JwtService;
import java.util.Objects;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;

    @Transactional
    public AuthResponse register(RegisterRequest request) {
        log.info("Intento de registro para email: {}", request.email());

        if (userRepository.existsByEmail(request.email())) {
            log.warn("Registro fallido: ya existe un usuario con email {}", request.email());
            throw new BadRequestException("Ya existe un usuario con ese email");
        }

        User user = User.builder()
                .fullName(request.fullName())
                .email(request.email())
                .password(passwordEncoder.encode(request.password()))
                .role(request.role() == null ? Role.CUSTOMER : request.role())
                .build();

        User savedUser = userRepository.save(Objects.requireNonNull(user));
        log.info("Usuario registrado exitosamente: id={}, email={}, rol={}",
                savedUser.getId(), savedUser.getEmail(), savedUser.getRole());

        return buildAuthResponse(savedUser);
    }

    public AuthResponse login(AuthRequest request) {
        log.info("Intento de login para email: {}", request.email());

        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.email(), request.password())
        );

        User user = userRepository.findByEmail(request.email())
                .orElseThrow(() -> {
                    log.warn("Login fallido: no se encontró el usuario con email {}", request.email());
                    return new BadRequestException("Credenciales inválidas");
                });

        log.info("Login exitoso: id={}, email={}, rol={}", user.getId(), user.getEmail(), user.getRole());
        return buildAuthResponse(user);
    }

    private AuthResponse buildAuthResponse(User user) {
        UserDetails userDetails = org.springframework.security.core.userdetails.User
                .withUsername(user.getEmail())
                .password(user.getPassword())
                .roles(user.getRole().name())
                .build();

        String token = jwtService.generateToken(userDetails);
        log.debug("Token JWT generado para usuario: {}", user.getEmail());

        return new AuthResponse(
                token,
                user.getId(),
                user.getFullName(),
                user.getEmail(),
                user.getRole()
        );
    }
}