package warehouse.service;

import org.junit.jupiter.api.Test;
import warehouse.dao.UserDao;
import warehouse.model.Role;
import warehouse.model.User;
import warehouse.util.PasswordUtil;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AuthServiceTest {

    private final AuthService service = new AuthService(new FakeUserDao());

    @Test
    void correctPasswordLogsIn() {
        Optional<User> user = service.login("admin", "admin");

        assertTrue(user.isPresent());
        assertEquals(Role.ADMIN, user.get().getRole());
    }

    @Test
    void wrongPasswordIsRejected() {
        assertTrue(service.login("admin", "wrong").isEmpty());
    }

    @Test
    void unknownUserIsRejected() {
        assertTrue(service.login("nobody", "admin").isEmpty());
    }

    private static class FakeUserDao extends UserDao {
        private final User admin = new User("admin", PasswordUtil.hash("admin"), Role.ADMIN);

        @Override
        public Optional<User> findByUsername(String username) {
            return "admin".equals(username) ? Optional.of(admin) : Optional.empty();
        }
    }
}
