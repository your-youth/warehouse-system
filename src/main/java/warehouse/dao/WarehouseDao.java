package warehouse.dao;

import org.hibernate.Session;
import warehouse.model.Owner;
import warehouse.model.Warehouse;
import warehouse.model.WarehouseStatus;
import warehouse.util.HibernateUtil;

import java.util.List;

public class WarehouseDao extends GenericDao<Warehouse> {

    public WarehouseDao() {
        super(Warehouse.class);
    }

    public List<Warehouse> findByOwner(Owner owner) {
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            return session.createQuery(
                    "select distinct w from Warehouse w left join fetch w.agents where w.owner.id = :ownerId",
                            Warehouse.class)
                    .setParameter("ownerId", owner.getId())
                    .list();
        }
    }

    public List<Warehouse> findFree() {
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            return session.createQuery("from Warehouse w where w.status = :status", Warehouse.class)
                    .setParameter("status", WarehouseStatus.FREE)
                    .list();
        }
    }
}
