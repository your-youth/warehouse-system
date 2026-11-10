package warehouse.util;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PasswordUtilTest {

    @Test
    void hashIsNotThePlainPassword() {
        assertNotEquals("secret", PasswordUtil.hash("secret"));
    }

    @Test
    void sameInputGivesSameHash() {
        assertEquals(PasswordUtil.hash("secret"), PasswordUtil.hash("secret"));
    }

    @Test
    void matchesOnlyTheRightPassword() {
        String stored = PasswordUtil.hash("secret");

        assertTrue(PasswordUtil.matches("secret", stored));
        assertFalse(PasswordUtil.matches("Secret", stored));
        assertFalse(PasswordUtil.matches("wrong", stored));
    }
}
