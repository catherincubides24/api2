package com.petshop.service;

import com.petshop.dto.user.UserCreateRequest;
import com.petshop.dto.user.UserResponse;
import com.petshop.dto.user.UserUpdateRequest;
import com.petshop.entity.User;
import com.petshop.exception.BadRequestException;
import com.petshop.exception.ResourceNotFoundException;
import com.petshop.repository.UserRepository;
import java.util.List;
import java.util.Objects;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public List<UserResponse> getAllUsers() {
        log.info("Consultando todos los usuarios");
        List<UserResponse> users = userRepository.findAll()
                .stream()
                .map(this::toResponse)
                .toList();
        log.debug("Total de usuarios encontrados: {}", users.size());
        return users;
    }

    public UserResponse getUserById(Long id) {
        log.info("Consultando usuario con id: {}", id);
        return toResponse(findUserById(id));
    }

    @Transactional
    public UserResponse createUser(UserCreateRequest request) {
        log.info("Creando usuario con email: {}", request.email());

        if (userRepository.existsByEmail(request.email())) {
            log.warn("No se pudo crear: ya existe usuario con email {}", request.email());
            throw new BadRequestException("Ya existe un usuario con ese email");
        }

        User user = User.builder()
                .fullName(request.fullName())
                .email(request.email())
                .password(passwordEncoder.encode(request.password()))
                .role(request.role())
                .build();

        UserResponse response = toResponse(userRepository.save(Objects.requireNonNull(user)));
        log.info("Usuario creado exitosamente: id={}, email={}", response.id(), response.email());
        return response;
    }

    @Transactional
    public UserResponse updateUser(Long id, UserUpdateRequest request) {
        log.info("Actualizando usuario id: {}", id);
        User user = findUserById(id);

        userRepository.findByEmail(request.email())
                .filter(existingUser -> !existingUser.getId().equals(id))
                .ifPresent(existingUser -> {
                    log.warn("Email {} ya está en uso por otro usuario", request.email());
                    throw new BadRequestException("Ya existe un usuario con ese email");
                });

        user.setFullName(request.fullName());
        user.setEmail(request.email());
        user.setRole(request.role());

        if (request.password() != null && !request.password().isBlank()) {
            user.setPassword(passwordEncoder.encode(request.password()));
            log.debug("Contraseña actualizada para usuario id: {}", id);
        }

        UserResponse response = toResponse(userRepository.save(user));
        log.info("Usuario actualizado exitosamente: id={}", response.id());
        return response;
    }

    @Transactional
    public void deleteUser(Long id) {
        log.info("Eliminando usuario id: {}", id);
        User user = findUserById(id);
        userRepository.delete(Objects.requireNonNull(user));
        log.info("Usuario eliminado exitosamente: id={}", id);
    }

    private User findUserById(Long id) {
        return userRepository.findById(Objects.requireNonNull(id))
                .orElseThrow(() -> {
                    log.warn("Usuario no encontrado con id: {}", id);
                    return new ResourceNotFoundException("Usuario no encontrado con id " + id);
                });
    }

    private UserResponse toResponse(User user) {
        return new UserResponse(
                user.getId(),
                user.getFullName(),
                user.getEmail(),
                user.getRole(),
                user.getCreatedAt()
        );
    }
}