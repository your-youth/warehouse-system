package warehouse.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import warehouse.dao.RentalContractDao;
import warehouse.dao.TenantDao;
import warehouse.dao.WarehouseDao;
import warehouse.model.Agent;
import warehouse.model.ContractStatus;
import warehouse.model.Notification;
import warehouse.model.NotificationType;
import warehouse.model.Owner;
import warehouse.model.RentalContract;
import warehouse.model.Tenant;
import warehouse.model.Warehouse;
import warehouse.model.WarehouseStatus;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RentalServiceTest {

    private FakeContractDao contractDao;
    private FakeWarehouseDao warehouseDao;
    private RentalService service;

    private Owner owner;
    private Agent agent;
    private Agent otherAgent;
    private Warehouse warehouse;
    private Tenant tenant;
    private LocalDate start;

    @BeforeEach
    void setUp() {
        owner = TestData.owner(1, "Ivanov");
        agent = TestData.agent(2, "Petrova", 10);
        otherAgent = TestData.agent(3, "Georgieva", 5);
        warehouse = TestData.warehouse(10, owner, 2000);
        tenant = new Tenant("Dimitar", "Stoyanov", "0877");
        start = LocalDate.now().plusDays(1);

        contractDao = new FakeContractDao();
        contractDao.assignedAgents = List.of(agent, otherAgent);
        warehouseDao = new FakeWarehouseDao(warehouse);
        service = new RentalService(contractDao, new TenantDao(), warehouseDao);
    }

    @Test
    void rejectsMissingDates() {
        assertThrows(IllegalArgumentException.class,
                () -> service.createRequest(warehouse, tenant, agent, null, start.plusMonths(1)));
    }

    @Test
    void rejectsEndDateBeforeStartDate() {
        assertThrows(IllegalArgumentException.class,
                () -> service.createRequest(warehouse, tenant, agent, start, start.minusDays(1)));
    }

    @Test
    void rejectsStartDateInThePast() {
        LocalDate yesterday = LocalDate.now().minusDays(1);
        assertThrows(IllegalArgumentException.class,
                () -> service.createRequest(warehouse, tenant, agent, yesterday, yesterday.plusMonths(1)));
    }

    @Test
    void rejectsWarehouseThatIsAlreadyRented() {
        warehouse.setStatus(WarehouseStatus.RENTED);
        assertThrows(IllegalStateException.class,
                () -> service.createRequest(warehouse, tenant, agent, start, start.plusMonths(1)));
    }

    @Test
    void rejectsAgentNotAssignedToWarehouse() {
        contractDao.agentAssigned = false;
        assertThrows(IllegalStateException.class,
                () -> service.createRequest(warehouse, tenant, agent, start, start.plusMonths(1)));
    }

    @Test
    void newRequestIsPendingAndNotifiesEveryAssignedAgent() {
        RentalContract request = service.createRequest(warehouse, tenant, agent, start, start.plusMonths(3));

        assertEquals(ContractStatus.PENDING, request.getStatus());
        assertSame(request, contractDao.savedRequest);
        assertEquals(2, contractDao.savedNotifications.size());
        assertTrue(contractDao.savedNotifications.stream()
                .allMatch(n -> n.getType() == NotificationType.NEW_RENTAL_REQUEST && n.getContract() == request));
        assertEquals(List.of(agent, otherAgent),
                contractDao.savedNotifications.stream().map(Notification::getRecipient).toList());
        assertEquals(WarehouseStatus.FREE, warehouse.getStatus());
    }

    @Test
    void confirmingRequestRentsWarehouseAndNotifiesOwner() {
        RentalContract request = service.createRequest(warehouse, tenant, agent, start, start.plusMonths(3));

        RentalContract contract = service.confirm(request, otherAgent);

        assertEquals(ContractStatus.ACTIVE, contract.getStatus());
        assertEquals(WarehouseStatus.RENTED, warehouse.getStatus());
        assertEquals(owner, contractDao.confirmNotification.getRecipient());
        assertEquals(NotificationType.WAREHOUSE_RENTED, contractDao.confirmNotification.getType());
    }

    @Test
    void onlyPendingRequestsCanBeConfirmed() {
        RentalContract request = service.createRequest(warehouse, tenant, agent, start, start.plusMonths(3));
        request.setStatus(ContractStatus.ACTIVE);

        assertThrows(IllegalStateException.class, () -> service.confirm(request, agent));
    }

    @Test
    void cancellingActiveContractFreesWarehouse() {
        RentalContract contract = new RentalContract(warehouse, tenant, agent, start, start.plusMonths(1));
        contract.setStatus(ContractStatus.ACTIVE);
        warehouse.setStatus(WarehouseStatus.RENTED);

        service.cancel(contract);

        assertEquals(ContractStatus.CANCELLED, contract.getStatus());
        assertEquals(WarehouseStatus.FREE, warehouseDao.updated.getStatus());
    }

    @Test
    void cancellingPendingRequestDoesNotTouchWarehouse() {
        RentalContract request = service.createRequest(warehouse, tenant, agent, start, start.plusMonths(1));

        service.cancel(request);

        assertEquals(ContractStatus.CANCELLED, request.getStatus());
        assertEquals(null, warehouseDao.updated);
    }

    private static class FakeContractDao extends RentalContractDao {
        boolean agentAssigned = true;
        List<Agent> assignedAgents = new ArrayList<>();
        RentalContract savedRequest;
        List<Notification> savedNotifications = new ArrayList<>();
        Notification confirmNotification;

        @Override
        public boolean isAgentAssigned(Agent agent, Warehouse warehouse) {
            return agentAssigned;
        }

        @Override
        public List<Agent> findAgentsAssignedTo(Warehouse warehouse) {
            return assignedAgents;
        }

        @Override
        public RentalContract saveRequest(RentalContract contract, List<Notification> notifications) {
            savedRequest = contract;
            savedNotifications = notifications;
            return contract;
        }

        @Override
        public RentalContract confirm(Long contractId, Function<RentalContract, Notification> notification) {
            savedRequest.setStatus(ContractStatus.ACTIVE);
            savedRequest.getWarehouse().setStatus(WarehouseStatus.RENTED);
            confirmNotification = notification.apply(savedRequest);
            return savedRequest;
        }

        @Override
        public RentalContract update(RentalContract contract) {
            return contract;
        }
    }

    private static class FakeWarehouseDao extends WarehouseDao {
        private final Warehouse warehouse;
        Warehouse updated;

        FakeWarehouseDao(Warehouse warehouse) {
            this.warehouse = warehouse;
        }

        @Override
        public Optional<Warehouse> findById(Long id) {
            return Optional.of(warehouse);
        }

        @Override
        public Warehouse update(Warehouse warehouse) {
            updated = warehouse;
            return warehouse;
        }
    }
}
