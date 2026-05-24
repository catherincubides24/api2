// backend/src/test/java/com/petshop/service/AuthServiceTest.java
package com.petshop.service;

import com.petshop.dto.auth.*;
import com.petshop.entity.*;
import com.petshop.exception.BadRequestException;
import com.petshop.repository.UserRepository;
import com.petshop.security.JwtService;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.*;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock private UserRepository userRepository;
    @Mock private PasswordEncoder passwordEncoder;
    @Mock private AuthenticationManager authenticationManager;
    @Mock private JwtService jwtService;

    @InjectMocks private AuthService authService;

    private User user;

    @BeforeEach
    void setUp() {
        user = User.builder().id(1L).fullName("Admin").email("admin@test.com")
                .password("encoded").role(Role.ADMIN).build();
    }

    @Test
    @DisplayName("register - crea usuario y devuelve token")
    void register_createsUserAndReturnsToken() {
        RegisterRequest req = new RegisterRequest("Admin", "admin@test.com", "Admin123!", Role.ADMIN);
        when(userRepository.existsByEmail("admin@test.com")).thenReturn(false);
        when(passwordEncoder.encode("Admin123!")).thenReturn("encoded");
        when(userRepository.save(any(User.class))).thenReturn(user);
        when(jwtService.generateToken(any())).thenReturn("jwt-token");

        AuthResponse result = authService.register(req);

        assertThat(result.token()).isEqualTo("jwt-token");
        assertThat(result.email()).isEqualTo("admin@test.com");
        assertThat(result.role()).isEqualTo(Role.ADMIN);
    }

    @Test
    @DisplayName("register - asigna CUSTOMER cuando role es null")
    void register_defaultsToCustomer_whenRoleNull() {
        RegisterRequest req = new RegisterRequest("Juan", "juan@test.com", "pass123", null);
        User customer = User.builder().id(2L).fullName("Juan").email("juan@test.com")
                .password("enc").role(Role.CUSTOMER).build();
        when(userRepository.existsByEmail("juan@test.com")).thenReturn(false);
        when(passwordEncoder.encode(any())).thenReturn("enc");
        when(userRepository.save(any())).thenReturn(customer);
        when(jwtService.generateToken(any())).thenReturn("token");

        AuthResponse result = authService.register(req);

        assertThat(result.role()).isEqualTo(Role.CUSTOMER);
    }

    @Test
    @DisplayName("register - lanza excepción si email duplicado")
    void register_throws_whenEmailExists() {
        RegisterRequest req = new RegisterRequest("X", "admin@test.com", "pass", Role.CUSTOMER);
        when(userRepository.existsByEmail("admin@test.com")).thenReturn(true);

        assertThatThrownBy(() -> authService.register(req))
                .isInstanceOf(BadRequestException.class);
    }

    @Test
    @DisplayName("login - autentica y devuelve token")
    void login_authenticatesAndReturnsToken() {
        AuthRequest req = new AuthRequest("admin@test.com", "Admin123!");
        when(userRepository.findByEmail("admin@test.com")).thenReturn(Optional.of(user));
        when(jwtService.generateToken(any())).thenReturn("jwt-token");

        AuthResponse result = authService.login(req);

        assertThat(result.token()).isEqualTo("jwt-token");
        verify(authenticationManager).authenticate(any(UsernamePasswordAuthenticationToken.class));
    }

    @Test
    @DisplayName("login - lanza excepción con credenciales inválidas")
    void login_throws_whenBadCredentials() {
        AuthRequest req = new AuthRequest("x@x.com", "wrong");
        doThrow(new BadCredentialsException("bad"))
                .when(authenticationManager).authenticate(any());

        assertThatThrownBy(() -> authService.login(req))
                .isInstanceOf(BadCredentialsException.class);
    }
}