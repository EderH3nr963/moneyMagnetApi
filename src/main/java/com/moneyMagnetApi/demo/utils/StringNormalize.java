package com.moneyMagnetApi.demo.utils;

import java.text.Normalizer;

public class StringNormalize {

    public static String normalize(String input) {
        if (input == null) return null;

        String normalized = Normalizer.normalize(input, Normalizer.Form.NFD);
        normalized = normalized.replaceAll("[^\\p{ASCII}]", "");
        return normalized.toLowerCase().trim();
    }
}

