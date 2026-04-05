package com.petshop;

import java.util.Locale;
import java.util.TimeZone;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class PetShopApplication {

    public static void main(String[] args) {
        TimeZone.setDefault(TimeZone.getTimeZone("America/Bogota"));
        Locale.setDefault(Locale.forLanguageTag("es-CO"));
        SpringApplication.run(PetShopApplication.class, args);
    }
}
