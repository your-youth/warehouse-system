package warehouse.service;

import warehouse.model.User;
import warehouse.util.HibernateUtil;
import warehouse.util.PasswordUtil;
import org.hibernate.Session;

import java.util.Optional;

public class AuthService {

    public Optional<User> login(String username, String password) {
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            return session.createQuery(
                            "FROM User u WHERE u.username = :username", User.class)
                    .setParameter("username", username)
                    .uniqueResultOptional()
                    .filter(u -> PasswordUtil.matches(password, u.getPassword()));
        }
    }
}
