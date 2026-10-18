package com.petshop.validation;

import java.util.Arrays;
import java.util.List;

public final class PasswordPolicy {

    public static final int MIN_LENGTH = 12;
    public static final int MAX_LENGTH = 25;

    private PasswordPolicy() {
    }

    public static List<PasswordRule> failedRules(String password) {
        return Arrays.stream(PasswordRule.values())
                .filter(rule -> !rule.isSatisfiedBy(password))
                .toList();
    }
}