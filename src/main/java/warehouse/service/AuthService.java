package warehouse.service;

import warehouse.model.User;
import warehouse.util.HibernateUtil;
import org.hibernate.Session;

import java.util.Optional;

/**
 * Handles user authentication.
 * "FROM User" also returns subclasses (Owner, Agent) when User uses inheritance.
 */
public class AuthService {

    public Optional<User> login(String username, String password) {
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            return session.createQuery(
                            "FROM User u WHERE u.username = :username", User.class)
                    .setParameter("username", username)
                    .uniqueResultOptional()
                    .filter(u -> u.getPassword().equals(password));
        }
    }
}
