package warehouse.service;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import warehouse.dao.UserDao;
import warehouse.model.User;
import warehouse.util.PasswordUtil;

import java.util.Optional;

public class AuthService {

    private static final Logger log = LogManager.getLogger(AuthService.class);

    private final UserDao userDao;

    public AuthService(UserDao userDao) {
        this.userDao = userDao;
    }

    public Optional<User> login(String username, String password) {
        Optional<User> user = userDao.findByUsername(username)
                .filter(u -> PasswordUtil.matches(password, u.getPassword()));
        if (user.isPresent()) {
            log.info("User logged in: {} ({})", username, user.get().getRole());
        } else {
            log.warn("Failed login for username: {}", username);
        }
        return user;
    }
}
