package warehouse.dao;

import org.hibernate.Session;
import warehouse.model.Agent;
import warehouse.model.ContractStatus;
import warehouse.model.RentalContract;
import warehouse.model.Warehouse;
import warehouse.util.HibernateUtil;

import java.time.LocalDate;
import java.util.List;

public class RentalContractDao extends GenericDao<RentalContract> {

    public RentalContractDao() {
        super(RentalContract.class);
    }

    public List<RentalContract> findByAgent(Agent agent) {
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            return session.createQuery(
                            "from RentalContract c where c.agent.id = :agentId order by c.startDate desc",
                            RentalContract.class)
                    .setParameter("agentId", agent.getId())
                    .list();
        }
    }

    public List<RentalContract> findByWarehouse(Warehouse warehouse) {
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            return session.createQuery(
                            "from RentalContract c where c.warehouse.id = :warehouseId order by c.startDate desc",
                            RentalContract.class)
                    .setParameter("warehouseId", warehouse.getId())
                    .list();
        }
    }

    public List<RentalContract> findStartedBetween(LocalDate from, LocalDate to) {
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            return session.createQuery(
                            "from RentalContract c where c.startDate between :from and :to"
                                    + " and c.status <> :cancelled order by c.startDate",
                            RentalContract.class)
                    .setParameter("from", from)
                    .setParameter("to", to)
                    .setParameter("cancelled", ContractStatus.CANCELLED)
                    .list();
        }
    }

    public List<RentalContract> findActiveEndingBefore(LocalDate date) {
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            return session.createQuery(
                            "from RentalContract c where c.status = :active and c.endDate <= :date",
                            RentalContract.class)
                    .setParameter("active", ContractStatus.ACTIVE)
                    .setParameter("date", date)
                    .list();
        }
    }

    public boolean hasOverlap(Warehouse warehouse, LocalDate start, LocalDate end) {
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            Long count = session.createQuery(
                            "select count(c) from RentalContract c where c.warehouse.id = :warehouseId"
                                    + " and c.status in (:statuses) and c.startDate <= :end and c.endDate >= :start",
                            Long.class)
                    .setParameter("warehouseId", warehouse.getId())
                    .setParameterList("statuses", List.of(ContractStatus.PENDING, ContractStatus.ACTIVE))
                    .setParameter("start", start)
                    .setParameter("end", end)
                    .uniqueResult();
            return count > 0;
        }
    }
}
