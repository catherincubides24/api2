package com.petshop.validation;

import java.util.function.Predicate;

public enum PasswordRule {

    LENGTH(
            "Entre " + PasswordPolicy.MIN_LENGTH + " y " + PasswordPolicy.MAX_LENGTH + " caracteres",
            p -> p.length() >= PasswordPolicy.MIN_LENGTH && p.length() <= PasswordPolicy.MAX_LENGTH),
    UPPERCASE(
            "Una letra mayúscula",
            p -> p.codePoints().anyMatch(Character::isUpperCase)),
    LOWERCASE(
            "Una letra minúscula",
            p -> p.codePoints().anyMatch(Character::isLowerCase)),
    DIGIT(
            "Un número",
            p -> p.codePoints().anyMatch(Character::isDigit)),
    SPECIAL(
            "Un carácter especial",
            p -> p.codePoints().anyMatch(c -> !Character.isLetterOrDigit(c) && !Character.isWhitespace(c)));

    private final String message;
    private final Predicate<String> check;

    PasswordRule(String message, Predicate<String> check) {
        this.message = message;
        this.check = check;
    }

    public boolean isSatisfiedBy(String password) {
        return check.test(password);
    }

    public String message() {
        return message;
    }
}