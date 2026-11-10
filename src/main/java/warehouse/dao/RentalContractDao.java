package warehouse.dao;

import jakarta.persistence.LockModeType;
import org.hibernate.Session;
import warehouse.model.Agent;
import warehouse.model.ContractStatus;
import warehouse.model.Notification;
import warehouse.model.RentalContract;
import warehouse.model.Warehouse;
import warehouse.model.WarehouseStatus;
import warehouse.util.HibernateUtil;

import java.time.LocalDate;
import java.util.List;
import java.util.function.Function;

public class RentalContractDao extends GenericDao<RentalContract> {

    public RentalContractDao() {
        super(RentalContract.class);
    }

    public List<RentalContract> findForAgent(Agent agent) {
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            return session.createQuery(
                            "from RentalContract c where c.agent.id = :agentId"
                                    + " or (c.status = :pending and exists ("
                                    + "select 1 from Warehouse w join w.agents a"
                                    + " where w.id = c.warehouse.id and a.id = :agentId))"
                                    + " order by c.startDate desc",
                            RentalContract.class)
                    .setParameter("agentId", agent.getId())
                    .setParameter("pending", ContractStatus.PENDING)
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

    public List<Agent> findAgentsAssignedTo(Warehouse warehouse) {
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            return session.createQuery(
                            "select a from Warehouse w join w.agents a where w.id = :warehouseId", Agent.class)
                    .setParameter("warehouseId", warehouse.getId())
                    .list();
        }
    }

    public List<Warehouse> findFreeWarehousesAssignedTo(Agent agent) {
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            return session.createQuery(
                            "select w from Warehouse w join w.agents a where a.id = :agentId and w.status = :status",
                            Warehouse.class)
                    .setParameter("agentId", agent.getId())
                    .setParameter("status", WarehouseStatus.FREE)
                    .list();
        }
    }

    public boolean isAgentAssigned(Agent agent, Warehouse warehouse) {
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            Long count = session.createQuery(
                            "select count(w) from Warehouse w join w.agents a where w.id = :warehouseId and a.id = :agentId",
                            Long.class)
                    .setParameter("warehouseId", warehouse.getId())
                    .setParameter("agentId", agent.getId())
                    .uniqueResult();
            return count > 0;
        }
    }

    public RentalContract saveRequest(RentalContract contract, List<Notification> notifications) {
        return inTransaction(session -> {
            Warehouse warehouse = lockWarehouse(session, contract.getWarehouse().getId());
            if (warehouse.getStatus() != WarehouseStatus.FREE) {
                throw new IllegalStateException("This warehouse is already rented.");
            }
            if (countOverlapping(session, contract, List.of(ContractStatus.PENDING, ContractStatus.ACTIVE)) > 0) {
                throw new IllegalStateException("This warehouse already has a request or contract for these dates.");
            }
            contract.setWarehouse(warehouse);
            if (contract.getTenant().getId() == null) {
                session.persist(contract.getTenant());
            }
            session.persist(contract);
            notifications.forEach(session::persist);
            return contract;
        });
    }

    public RentalContract confirm(Long contractId, Function<RentalContract, Notification> notification) {
        return inTransaction(session -> {
            RentalContract contract = session.get(RentalContract.class, contractId);
            if (contract == null) {
                throw new IllegalStateException("This request no longer exists.");
            }
            Warehouse warehouse = lockWarehouse(session, contract.getWarehouse().getId());
            session.refresh(contract);
            if (contract.getStatus() != ContractStatus.PENDING) {
                throw new IllegalStateException("Only a pending request can be confirmed.");
            }
            if (warehouse.getStatus() != WarehouseStatus.FREE) {
                throw new IllegalStateException("This warehouse is already rented.");
            }
            if (countOverlapping(session, contract, List.of(ContractStatus.ACTIVE)) > 0) {
                throw new IllegalStateException("This warehouse already has a contract for these dates.");
            }
            contract.setStatus(ContractStatus.ACTIVE);
            warehouse.setStatus(WarehouseStatus.RENTED);
            session.persist(notification.apply(contract));
            return contract;
        });
    }

    private Warehouse lockWarehouse(Session session, Long warehouseId) {
        Warehouse warehouse = session.find(Warehouse.class, warehouseId, LockModeType.PESSIMISTIC_WRITE);
        if (warehouse == null) {
            throw new IllegalStateException("This warehouse no longer exists.");
        }
        session.refresh(warehouse);
        return warehouse;
    }

    private long countOverlapping(Session session, RentalContract contract, List<ContractStatus> statuses) {
        return session.createQuery(
                        "select count(c) from RentalContract c where c.warehouse.id = :warehouseId"
                                + " and c.status in (:statuses) and c.startDate <= :end and c.endDate >= :start"
                                + " and c.id <> :id",
                        Long.class)
                .setParameter("warehouseId", contract.getWarehouse().getId())
                .setParameterList("statuses", statuses)
                .setParameter("start", contract.getStartDate())
                .setParameter("end", contract.getEndDate())
                .setParameter("id", contract.getId() == null ? -1L : contract.getId())
                .uniqueResult();
    }
}
