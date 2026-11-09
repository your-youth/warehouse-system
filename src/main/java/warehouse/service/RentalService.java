package warehouse.service;

import org.hibernate.Session;
import warehouse.dao.RentalContractDao;
import warehouse.dao.TenantDao;
import warehouse.dao.WarehouseDao;
import warehouse.model.Agent;
import warehouse.model.ContractStatus;
import warehouse.model.NotificationType;
import warehouse.model.RentalContract;
import warehouse.model.Tenant;
import warehouse.model.Warehouse;
import warehouse.model.WarehouseStatus;
import warehouse.util.HibernateUtil;

import java.time.LocalDate;
import java.util.List;

public class RentalService {

    private final RentalContractDao contractDao = new RentalContractDao();
    private final TenantDao tenantDao = new TenantDao();
    private final WarehouseDao warehouseDao = new WarehouseDao();
    private final NotificationService notificationService = new NotificationService();

    public RentalContract rent(Warehouse warehouse, Tenant tenant, Agent agent,
                               LocalDate startDate, LocalDate endDate) {
        if (startDate == null || endDate == null) {
            throw new IllegalArgumentException("Please choose the start and end dates.");
        }
        if (!endDate.isAfter(startDate)) {
            throw new IllegalArgumentException("End date must be after the start date.");
        }
        if (startDate.isBefore(LocalDate.now())) {
            throw new IllegalArgumentException("Start date cannot be in the past.");
        }
        if (warehouse.getStatus() != WarehouseStatus.FREE) {
            throw new IllegalStateException("This warehouse is already rented.");
        }
        if (!isAssigned(agent, warehouse)) {
            throw new IllegalStateException("You are not assigned to this warehouse.");
        }
        if (contractDao.hasOverlap(warehouse, startDate, endDate)) {
            throw new IllegalStateException("This warehouse already has a contract for these dates.");
        }

        if (tenant.getId() == null) {
            tenantDao.save(tenant);
        }

        RentalContract contract = new RentalContract(warehouse, tenant, agent, startDate, endDate);
        contract.setStatus(ContractStatus.ACTIVE);
        contractDao.save(contract);

        warehouse.setStatus(WarehouseStatus.RENTED);
        warehouseDao.update(warehouse);

        notificationService.send(warehouse.getOwner(), NotificationType.WAREHOUSE_RENTED,
                "Your warehouse at " + warehouse.getAddress() + " was rented to " + tenant.getFullName()
                        + " by " + agent.getFullName() + " (" + startDate + " - " + endDate + ")",
                contract);
        return contract;
    }

    public void cancel(RentalContract contract) {
        contract.setStatus(ContractStatus.CANCELLED);
        contractDao.update(contract);
        freeWarehouse(contract.getWarehouse());
    }

    public void expireFinishedContracts() {
        for (RentalContract contract : contractDao.findActiveEndingBefore(LocalDate.now().minusDays(1))) {
            contract.setStatus(ContractStatus.EXPIRED);
            contractDao.update(contract);
            freeWarehouse(contract.getWarehouse());
        }
    }

    public List<RentalContract> getContractsOf(Agent agent) {
        return contractDao.findByAgent(agent);
    }

    public List<Warehouse> getFreeWarehousesOf(Agent agent) {
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            return session.createQuery(
                            "select w from Warehouse w join w.agents a where a.id = :agentId and w.status = :status",
                            Warehouse.class)
                    .setParameter("agentId", agent.getId())
                    .setParameter("status", WarehouseStatus.FREE)
                    .list();
        }
    }

    public List<Tenant> searchTenants(String text) {
        return text == null || text.isBlank() ? tenantDao.findAll() : tenantDao.search(text.trim());
    }

    private void freeWarehouse(Warehouse warehouse) {
        warehouse.setStatus(WarehouseStatus.FREE);
        warehouseDao.update(warehouse);
    }

    private boolean isAssigned(Agent agent, Warehouse warehouse) {
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
}
