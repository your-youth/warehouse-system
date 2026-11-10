package warehouse.dao;

import org.hibernate.Session;
import org.hibernate.Transaction;
import warehouse.util.HibernateUtil;

import java.util.List;
import java.util.Optional;
import java.util.function.Function;

public abstract class GenericDao<T> {

    private final Class<T> type;

    protected GenericDao(Class<T> type) {
        this.type = type;
    }

    public void save(T entity) {
        inTransaction(session -> {
            session.persist(entity);
            return null;
        });
    }

    public T update(T entity) {
        return inTransaction(session -> session.merge(entity));
    }

    public void delete(T entity) {
        inTransaction(session -> {
            session.remove(session.merge(entity));
            return null;
        });
    }

    public Optional<T> findById(Long id) {
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            return Optional.ofNullable(session.get(type, id));
        }
    }

    public List<T> findAll() {
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            return session.createQuery("from " + type.getSimpleName(), type).list();
        }
    }

    protected <R> R inTransaction(Function<Session, R> work) {
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            Transaction tx = session.beginTransaction();
            try {
                R result = work.apply(session);
                tx.commit();
                return result;
            } catch (RuntimeException e) {
                if (tx.isActive()) {
                    tx.rollback();
                }
                throw e;
            }
        }
    }
}