package gr.cf9.pants.expense_tracker.core.util;

import org.junit.jupiter.api.Test;

import java.util.Locale;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class NameNormalizerTest {

    @Test
    void trimsAndConvertsToUppercase() {
        assertEquals("GROCERIES", NameNormalizer.normalizeName("  Groceries  "));
    }

    @Test
    void usesRootLocaleForUppercaseConversion() {
        Locale originalLocale = Locale.getDefault();

        try {
            Locale.setDefault(Locale.forLanguageTag("tr-TR"));
            assertEquals("I", NameNormalizer.normalizeName("i"));
        } finally {
            Locale.setDefault(originalLocale);
        }
    }
}
