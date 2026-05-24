// backend/src/test/java/com/petshop/service/UserServiceTest.java
package com.petshop.service;

import com.petshop.dto.user.*;
import com.petshop.entity.*;
import com.petshop.exception.*;
import com.petshop.repository.UserRepository;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDateTime;
import java.util.*;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock private UserRepository userRepository;
    @Mock private PasswordEncoder passwordEncoder;

    @InjectMocks private UserService userService;

    private User user;

    @BeforeEach
    void setUp() {
        user = User.builder().id(1L).fullName("Ana García")
                .email("ana@test.com").password("encoded")
                .role(Role.CUSTOMER).createdAt(LocalDateTime.now()).build();
    }

    @Test
    @DisplayName("getAllUsers - retorna lista completa")
    void getAllUsers_returnsList() {
        when(userRepository.findAll()).thenReturn(List.of(user));

        List<UserResponse> result = userService.getAllUsers();

        assertThat(result).hasSize(1);
        assertThat(result.get(0).email()).isEqualTo("ana@test.com");
    }

    @Test
    @DisplayName("getUserById - retorna usuario cuando existe")
    void getUserById_returnsUser() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));

        UserResponse result = userService.getUserById(1L);

        assertThat(result.fullName()).isEqualTo("Ana García");
    }

    @Test
    @DisplayName("getUserById - lanza excepción cuando no existe")
    void getUserById_throws_whenNotFound() {
        when(userRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userService.getUserById(99L))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    @DisplayName("createUser - crea usuario con contraseña encodeada")
    void createUser_encodesPassword() {
        UserCreateRequest req = new UserCreateRequest("Pedro", "pedro@test.com", "123456", Role.CUSTOMER);
        when(userRepository.existsByEmail("pedro@test.com")).thenReturn(false);
        when(passwordEncoder.encode("123456")).thenReturn("hashed");
        when(userRepository.save(any(User.class))).thenReturn(user);

        UserResponse result = userService.createUser(req);

        assertThat(result).isNotNull();
        verify(passwordEncoder).encode("123456");
    }

    @Test
    @DisplayName("createUser - lanza excepción si email ya existe")
    void createUser_throws_whenEmailDuplicated() {
        UserCreateRequest req = new UserCreateRequest("X", "ana@test.com", "pass", Role.CUSTOMER);
        when(userRepository.existsByEmail("ana@test.com")).thenReturn(true);

        assertThatThrownBy(() -> userService.createUser(req))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("email");
    }

    @Test
    @DisplayName("updateUser - actualiza sin cambiar contraseña si viene vacía")
    void updateUser_doesNotChangePassword_whenBlank() {
        UserUpdateRequest req = new UserUpdateRequest("Nuevo Nombre", "ana@test.com", "", Role.ADMIN);
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(userRepository.findByEmail("ana@test.com")).thenReturn(Optional.of(user));
        when(userRepository.save(any())).thenReturn(user);

        userService.updateUser(1L, req);

        verify(passwordEncoder, never()).encode(any());
    }

    @Test
    @DisplayName("updateUser - encoda la contraseña cuando viene no vacía")
    void updateUser_encodesPassword_whenProvided() {
        UserUpdateRequest req = new UserUpdateRequest("Nuevo", "ana@test.com", "nueva123", Role.CUSTOMER);
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(userRepository.findByEmail("ana@test.com")).thenReturn(Optional.of(user));
        when(passwordEncoder.encode("nueva123")).thenReturn("newHash");
        when(userRepository.save(any())).thenReturn(user);

        userService.updateUser(1L, req);

        verify(passwordEncoder).encode("nueva123");
    }

    @Test
    @DisplayName("updateUser - lanza excepción si email ya lo usa otro usuario")
    void updateUser_throws_whenEmailTakenByOther() {
        User other = User.builder().id(2L).email("otro@test.com").build();
        UserUpdateRequest req = new UserUpdateRequest("X", "otro@test.com", null, Role.CUSTOMER);
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(userRepository.findByEmail("otro@test.com")).thenReturn(Optional.of(other));

        assertThatThrownBy(() -> userService.updateUser(1L, req))
                .isInstanceOf(BadRequestException.class);
    }

    @Test
    @DisplayName("deleteUser - invoca delete cuando usuario existe")
    void deleteUser_deletesSuccessfully() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));

        userService.deleteUser(1L);

        verify(userRepository).delete(user);
    }
}