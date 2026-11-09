package warehouse.dao;

import org.hibernate.Session;
import warehouse.model.User;
import warehouse.util.HibernateUtil;

import java.util.Optional;

public class UserDao extends GenericDao<User> {

    public UserDao() {
        super(User.class);
    }

    public Optional<User> findByUsername(String username) {
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            return session.createQuery("from User u where u.username = :username", User.class)
                    .setParameter("username", username)
                    .uniqueResultOptional();
        }
    }
}