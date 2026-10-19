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
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;

@Configuration
@RequiredArgsConstructor
public class DataSeeder {

    private static final String ADMIN_EMAIL = "admin@petshop.com";
    private static final String ADMIN_NAME = "Admin Huellitas Shop";

    private final UserRepository userRepository;
    private final ProductRepository productRepository;
    private final PasswordEncoder passwordEncoder;

    // 12 factors: la contraseña inicial viene del entorno (ADMIN_INITIAL_PASSWORD), nunca del código
    @Value("${app.admin.initial-password}")
    private String adminInitialPassword;

    @Bean
    CommandLineRunner seedData() {
        return args -> {
            seedAdmin();
            seedProducts();
        };
    }

    private void seedAdmin() {
        userRepository.findByEmail(ADMIN_EMAIL).ifPresentOrElse(
                admin -> {
                    if (!ADMIN_NAME.equals(admin.getFullName())) {
                        admin.setFullName(ADMIN_NAME);
                        userRepository.save(admin);
                    }
                },
                () -> {
                    User admin = User.builder()
                            .fullName(ADMIN_NAME)
                            .email(ADMIN_EMAIL)
                            .password(passwordEncoder.encode(adminInitialPassword))
                            .role(Role.ADMIN)
                            .build();
                    userRepository.save(Objects.requireNonNull(admin));
                }
        );
    }

    private void seedProducts() {
        if (productRepository.count() > 0) {
            return;
        }

        List<Product> products = List.of(
                product("Alimento Premium para Perro",
                        "Bolsa de 10kg con fórmula balanceada para perros adultos.",
                        "39900", 25, "photo-1583511666372-62fc211f8377", "Perros"),
                product("Rascador para Gato",
                        "Rascador con poste de sisal y base antideslizante.",
                        "24500", 18, "photo-1592194996308-7b43878e84a6", "Gatos"),
                product("Juguete Mordedor",
                        "Mordedor resistente para entretenimiento y salud dental.",
                        "9990", 40, "photo-1591768575198-88dac53fbd0a", "Accesorios"),
                product("Arena Sanitaria",
                        "Arena aglomerante de bajo olor, presentación de 8kg.",
                        "15750", 30, "photo-1548767797-d8c844163c4c", "Higiene")
        );

        productRepository.saveAll(Objects.requireNonNull(products));
    }

    private Product product(String name, String description, String price, int stock,
                            String imageId, String category) {
        return Product.builder()
                .name(name)
                .description(description)
                .price(new BigDecimal(price))
                .stock(stock)
                .imageUrl("https://images.unsplash.com/" + imageId)
                .category(category)
                .active(true)
                .build();
    }
}