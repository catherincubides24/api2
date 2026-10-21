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

    @Autowired MockMvc mvc;
    @Autowired UserRepository userRepository;

    @Test
    void emailInvalidoDevuelve400() throws Exception {
        mvc.perform(post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"fullName":"Ana","email":"no-es-email","password":"Abc12345!"}
                """))
           .andExpect(status().isBadRequest());
    }

    @Test
    void passwordCortaDevuelve400() throws Exception {
        mvc.perform(post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"fullName":"Ana","email":"ana@test.com","password":"123"}
                """))
           .andExpect(status().isBadRequest());
    }

    // Un registro público NUNCA debe poder crear administradores.
    @Test
    void registroPublicoNoPuedeCrearAdmin() throws Exception {
        mvc.perform(post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"fullName":"Mala","email":"mala@test.com","password":"Abc12345!","role":"ADMIN"}
                """))
           .andDo(print())
           .andExpect(status().is2xxSuccessful());

        Role rolGuardado = userRepository.findByEmail("mala@test.com")
                .orElseThrow(() -> new AssertionError("El usuario no se guardó"))
                .getRole();

        assertEquals(Role.CUSTOMER, rolGuardado);
    }
}