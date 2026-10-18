package com.petshop.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import java.util.List;
import java.util.stream.Collectors;

public class StrongPasswordValidator implements ConstraintValidator<StrongPassword, String> {

    @Override
    public boolean isValid(String password, ConstraintValidatorContext context) {
        // null = "no enviado". La obligatoriedad la decide @NotBlank donde aplique.
        if (password == null) {
            return true;
        }

        List<PasswordRule> failed = PasswordPolicy.failedRules(password);
        if (failed.isEmpty()) {
            return true;
        }

        String detail = failed.stream()
                .map(PasswordRule::message)
                .collect(Collectors.joining("; "));

        context.disableDefaultConstraintViolation();
        context.buildConstraintViolationWithTemplate("La contraseña debe tener: " + detail)
                .addConstraintViolation();
        return false;
    }
}