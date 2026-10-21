package com.petshop;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

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

    @Test
    void emailInvalidoDevuelve400() throws Exception {
        mvc.perform(post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"fullName":"Ana","email":"no-es-email","password":"123456","role":"CUSTOMER"}
                """))
           .andExpect(status().isBadRequest());
    }

    @Test
    void passwordCortaDevuelve400() throws Exception {
        mvc.perform(post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"fullName":"Ana","email":"ana@test.com","password":"123","role":"CUSTOMER"}
                """))
           .andExpect(status().isBadRequest());
    }

    // Un registro público NUNCA debe poder crear administradores.
    @Test
    void registroPublicoNoPuedeCrearAdmin() throws Exception {
        mvc.perform(post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"fullName":"Mala","email":"mala@test.com","password":"123456","role":"ADMIN"}
                """))
           .andExpect(jsonPath("$.role").value("CUSTOMER"));
    }
}