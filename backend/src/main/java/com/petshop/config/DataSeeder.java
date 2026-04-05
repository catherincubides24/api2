package com.petshop.config;

import com.petshop.entity.Product;
import com.petshop.entity.Role;
import com.petshop.entity.User;
import com.petshop.repository.ProductRepository;
import com.petshop.repository.UserRepository;
import java.math.BigDecimal;
import java.util.List;
import java.util.Objects;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;

@Configuration
@RequiredArgsConstructor
public class DataSeeder {

    private final UserRepository userRepository;
    private final ProductRepository productRepository;
    private final PasswordEncoder passwordEncoder;

    @Bean
    CommandLineRunner seedData() {
        return args -> {
            if (!userRepository.existsByEmail("admin@petshop.com")) {
                User admin = User.builder()
                        .fullName("Admin Huellitas Shop")
                        .email("admin@petshop.com")
                        .password(passwordEncoder.encode("Admin123!"))
                        .role(Role.ADMIN)
                        .build();
                userRepository.save(Objects.requireNonNull(admin));
            } else {
                userRepository.findByEmail("admin@petshop.com").ifPresent(admin -> {
                    if (!"Admin Huellitas Shop".equals(admin.getFullName())) {
                        admin.setFullName("Admin Huellitas Shop");
                        userRepository.save(admin);
                    }
                });
            }

            if (productRepository.count() == 0) {
                List<Product> products = List.of(
                        Product.builder()
                                .name("Alimento Premium para Perro")
                                .description("Bolsa de 10kg con fórmula balanceada para perros adultos.")
                                .price(new BigDecimal("39.90"))
                                .stock(25)
                                .imageUrl("https://images.unsplash.com/photo-1583511666372-62fc211f8377")
                                .category("Perros")
                                .active(true)
                                .build(),
                        Product.builder()
                                .name("Rascador para Gato")
                                .description("Rascador con poste de sisal y base antideslizante.")
                                .price(new BigDecimal("24.50"))
                                .stock(18)
                                .imageUrl("https://images.unsplash.com/photo-1592194996308-7b43878e84a6")
                                .category("Gatos")
                                .active(true)
                                .build(),
                        Product.builder()
                                .name("Juguete Mordedor")
                                .description("Mordedor resistente para entretenimiento y salud dental.")
                                .price(new BigDecimal("9.99"))
                                .stock(40)
                                .imageUrl("https://images.unsplash.com/photo-1591768575198-88dac53fbd0a")
                                .category("Accesorios")
                                .active(true)
                                .build(),
                        Product.builder()
                                .name("Arena Sanitaria")
                                .description("Arena aglomerante de bajo olor, presentación de 8kg.")
                                .price(new BigDecimal("15.75"))
                                .stock(30)
                                .imageUrl("https://images.unsplash.com/photo-1548767797-d8c844163c4c")
                                .category("Higiene")
                                .active(true)
                                .build()
                );

                productRepository.saveAll(Objects.requireNonNull(products));
            }
        };
    }
}
