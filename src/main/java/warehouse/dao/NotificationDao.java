package warehouse.dao;

import org.hibernate.Session;
import warehouse.model.Notification;
import warehouse.model.NotificationType;
import warehouse.model.RentalContract;
import warehouse.model.User;
import warehouse.util.HibernateUtil;

import java.util.List;

public class NotificationDao extends GenericDao<Notification> {

    public NotificationDao() {
        super(Notification.class);
    }

    public List<Notification> findByRecipient(User user) {
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            return session.createQuery(
                            "from Notification n where n.recipient.id = :userId order by n.createdAt desc",
                            Notification.class)
                    .setParameter("userId", user.getId())
                    .list();
        }
    }

    public long countUnread(User user) {
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            return session.createQuery(
                            "select count(n) from Notification n where n.recipient.id = :userId and n.read = false",
                            Long.class)
                    .setParameter("userId", user.getId())
                    .uniqueResult();
        }
    }

    public boolean exists(User user, NotificationType type, RentalContract contract) {
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            Long count = session.createQuery(
                            "select count(n) from Notification n where n.recipient.id = :userId"
                                    + " and n.type = :type and n.contract.id = :contractId",
                            Long.class)
                    .setParameter("userId", user.getId())
                    .setParameter("type", type)
                    .setParameter("contractId", contract.getId())
                    .uniqueResult();
            return count > 0;
        }
    }

    public void markAllRead(User user) {
        inTransaction(session -> session.createMutationQuery(
                        "update Notification n set n.read = true where n.recipient.id = :userId and n.read = false")
                .setParameter("userId", user.getId())
                .executeUpdate());
    }
}
