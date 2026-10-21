package com.petshop;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.petshop.entity.Role;
import com.petshop.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
class AuthRegistrationTest {

    @Autowired
    MockMvc mvc;

    @Autowired
    UserRepository userRepository;

    @Test
    void emailInvalidoDevuelve400() throws Exception {
        mvc.perform(post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"fullName":"Ana Corta","email":"ana.cortaexample.com","password":"Fabulosa-Clave-2026!"}
                    """))
            .andExpect(status().isBadRequest());
    }

    @Test
    void passwordCortaDevuelve400() throws Exception {
        mvc.perform(post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"fullName":"Ana Corta","email":"ana.corta@example.com","password":"Abc1!"}
                    """))
            .andExpect(status().isBadRequest());
    }

    // Un registro público NUNCA debe poder crear administradores.
    @Test
    void registroPublicoNoPuedeCrearAdmin() throws Exception {
        mvc.perform(post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"fullName":"Bosy","email":"bosy@test.com","password":"Segura-Clave-2026!","role":"ADMIN"}
                    """))
            .andDo(print())
            .andExpect(status().is2xxSuccessful());

        Role rolGuardado = userRepository.findByEmail("bosy@test.com")
                .orElseThrow(() -> new AssertionError("El usuario no se guardó"))
                .getRole();

        assertEquals(Role.CUSTOMER, rolGuardado);
    }

    @Test
    void registroSinRolCreaCustomer() throws Exception {
        mvc.perform(post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"fullName":"Ana Prueba","email":"ana.prueba@example.com","password":"Fabulosa-Clave-2026!"}
                    """))
            .andDo(print())
            .andExpect(status().is2xxSuccessful());

        Role rol = userRepository.findByEmail("ana.prueba@example.com")
                .orElseThrow(() -> new AssertionError("El usuario no se guardó"))
                .getRole();

        assertEquals(Role.CUSTOMER, rol);
    }
}