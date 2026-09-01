package com.pdfFileReader.util;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PasswordHasherTest {

    @Test
    void hashVerifiesRoundTrip() {
        String stored = PasswordHasher.hash("guclu-parola-123");

        assertTrue(PasswordHasher.verify("guclu-parola-123", stored), "dogru parola dogrulanmali");
        assertFalse(PasswordHasher.verify("yanlis-parola", stored), "yanlis parola reddedilmeli");
    }

    @Test
    void samePasswordProducesDifferentHashes() {
        String first = PasswordHasher.hash("aynı-parola");
        String second = PasswordHasher.hash("aynı-parola");

        assertNotEquals(first, second, "tuz farkli oldugu icin ozetler farkli olmali");
    }

    @Test
    void verifyRejectsMalformedHashes() {
        assertFalse(PasswordHasher.verify("birsey", "bozuk-format"));
        assertFalse(PasswordHasher.verify("birsey", "pbkdf2$abc$!!$!!"));
        assertFalse(PasswordHasher.verify("birsey", ""));
    }
}
