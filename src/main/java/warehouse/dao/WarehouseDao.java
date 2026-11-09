package warehouse.dao;

import org.hibernate.Session;
import warehouse.model.Owner;
import warehouse.model.Warehouse;
import warehouse.model.WarehouseStatus;
import warehouse.util.HibernateUtil;
import warehouse.model.Agent;

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

    public void assignAgent(Long warehouseId, Long agentId) {
        inTransaction(session -> {
            Warehouse w = session.get(Warehouse.class, warehouseId);
            Agent a = session.get(Agent.class, agentId);
            w.getAgents().add(a);
            return null;
        });
    }

    public void removeAgent(Long warehouseId, Long agentId) {
        inTransaction(session -> {
            Warehouse w = session.get(Warehouse.class, warehouseId);
            w.getAgents().removeIf(a -> a.getId().equals(agentId));
            return null;
        });
    }
}
