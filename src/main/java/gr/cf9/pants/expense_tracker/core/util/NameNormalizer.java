package gr.cf9.pants.expense_tracker.core.util;

import java.util.Locale;

public class NameNormalizer {

    public static String normalizeName(String name) {
        return name.trim().toUpperCase(Locale.ROOT);
    }
}
