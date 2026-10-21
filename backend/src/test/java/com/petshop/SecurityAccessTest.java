package com.petshop;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
class SecurityAccessTest {

    @Autowired MockMvc mvc;

    @Test
    void productosPublicosNoRequierenToken() throws Exception {
        mvc.perform(get("/api/products")).andExpect(status().isOk());
    }

    @Test
    void usuariosSinTokenEsRechazado() throws Exception {
        mvc.perform(get("/api/users")).andExpect(status().is4xxClientError());
    }

    @Test
    @WithMockUser(roles = "CUSTOMER")
    void customerNoPuedeListarUsuarios() throws Exception {
        mvc.perform(get("/api/users")).andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "CUSTOMER")
    void customerNoPuedeListarTodosLosPedidos() throws Exception {
        mvc.perform(get("/api/orders")).andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void adminSiPuedeListarUsuarios() throws Exception {
        mvc.perform(get("/api/users")).andExpect(status().isOk());
    }
}