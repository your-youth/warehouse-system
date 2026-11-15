package warehouse.util;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class PasswordUtilTest {

    @Test
    void correctPasswordMatches() {
        String hash = PasswordUtil.hash("secret");
        assertTrue(PasswordUtil.matches("secret", hash));
    }

    @Test
    void wrongPasswordDoesNotMatch() {
        String hash = PasswordUtil.hash("secret");
        assertFalse(PasswordUtil.matches("Secret", hash));
    }

    @Test
    void hashIsNotPlainText() {
        assertNotEquals("secret", PasswordUtil.hash("secret"));
    }

    @Test
    void sameInputGivesSameHash() {
        assertEquals(PasswordUtil.hash("1234"), PasswordUtil.hash("1234"));
    }
}