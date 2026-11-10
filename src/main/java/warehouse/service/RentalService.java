package warehouse.service;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import warehouse.dao.RentalContractDao;
import warehouse.dao.TenantDao;
import warehouse.dao.WarehouseDao;
import warehouse.model.Agent;
import warehouse.model.ContractStatus;
import warehouse.model.Notification;
import warehouse.model.NotificationType;
import warehouse.model.RentalContract;
import warehouse.model.Tenant;
import warehouse.model.Warehouse;
import warehouse.model.WarehouseStatus;

import java.time.LocalDate;
import java.util.List;

public class RentalService {

    private static final Logger log = LogManager.getLogger(RentalService.class);

    private final RentalContractDao contractDao;
    private final TenantDao tenantDao;
    private final WarehouseDao warehouseDao;

    public RentalService(RentalContractDao contractDao, TenantDao tenantDao, WarehouseDao warehouseDao) {
        this.contractDao = contractDao;
        this.tenantDao = tenantDao;
        this.warehouseDao = warehouseDao;
    }

    public RentalContract createRequest(Warehouse warehouse, Tenant tenant, Agent agent,
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
        if (!contractDao.isAgentAssigned(agent, warehouse)) {
            throw new IllegalStateException("You are not assigned to this warehouse.");
        }

        RentalContract contract = new RentalContract(warehouse, tenant, agent, startDate, endDate);
        String message = "New rental request for " + warehouse.getAddress() + " from " + tenant.getFullName()
                + " (" + startDate + " - " + endDate + "), created by " + agent.getFullName();
        List<Notification> notifications = contractDao.findAgentsAssignedTo(warehouse).stream()
                .map(a -> new Notification(a, NotificationType.NEW_RENTAL_REQUEST, message, contract))
                .toList();

        contractDao.saveRequest(contract, notifications);
        log.info("Rental request created: warehouse id={}, tenant {}, agent {}",
                warehouse.getId(), tenant.getFullName(), agent.getUsername());
        return contract;
    }

    public RentalContract confirm(RentalContract request, Agent agent) {
        if (request.getStatus() != ContractStatus.PENDING) {
            throw new IllegalStateException("Only a pending request can be confirmed.");
        }
        if (!contractDao.isAgentAssigned(agent, request.getWarehouse())) {
            throw new IllegalStateException("You are not assigned to this warehouse.");
        }
        RentalContract contract = contractDao.confirm(request.getId(), c -> new Notification(
                c.getWarehouse().getOwner(), NotificationType.WAREHOUSE_RENTED,
                "Your warehouse at " + c.getWarehouse().getAddress() + " was rented to "
                        + c.getTenant().getFullName() + " by " + agent.getFullName()
                        + " (" + c.getStartDate() + " - " + c.getEndDate() + ")",
                c));
        log.info("Rental contract id={} confirmed by {}", contract.getId(), agent.getUsername());
        return contract;
    }

    public void cancel(RentalContract contract) {
        boolean wasActive = contract.getStatus() == ContractStatus.ACTIVE;
        contract.setStatus(ContractStatus.CANCELLED);
        contractDao.update(contract);
        if (wasActive) {
            freeWarehouse(contract.getWarehouse());
        }
        log.info("Rental contract id={} cancelled", contract.getId());
    }

    public void expireFinishedContracts() {
        for (RentalContract contract : contractDao.findActiveEndingBefore(LocalDate.now().minusDays(1))) {
            contract.setStatus(ContractStatus.EXPIRED);
            contractDao.update(contract);
            freeWarehouse(contract.getWarehouse());
            log.info("Rental contract id={} expired", contract.getId());
        }
    }

    public List<RentalContract> getContractsOf(Agent agent) {
        return contractDao.findForAgent(agent);
    }

    public List<Warehouse> getFreeWarehousesOf(Agent agent) {
        return contractDao.findFreeWarehousesAssignedTo(agent);
    }

    public List<Tenant> searchTenants(String text) {
        return text == null || text.isBlank() ? tenantDao.findAll() : tenantDao.search(text.trim());
    }

    private void freeWarehouse(Warehouse warehouse) {
        Warehouse current = warehouseDao.findById(warehouse.getId()).orElse(warehouse);
        current.setStatus(WarehouseStatus.FREE);
        warehouseDao.update(current);
    }
}
