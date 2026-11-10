package warehouse.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import warehouse.dao.NotificationDao;
import warehouse.dao.RentalContractDao;
import warehouse.model.Agent;
import warehouse.model.ContractStatus;
import warehouse.model.Notification;
import warehouse.model.NotificationType;
import warehouse.model.Owner;
import warehouse.model.RentalContract;
import warehouse.model.Tenant;
import warehouse.model.User;
import warehouse.model.Warehouse;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class NotificationServiceTest {

    private FakeNotificationDao notificationDao;
    private FakeContractDao contractDao;
    private NotificationService service;
    private Owner owner;
    private Agent agent;

    @BeforeEach
    void setUp() {
        owner = TestData.owner(1, "Ivanov");
        agent = TestData.agent(2, "Petrova", 10);
        Warehouse warehouse = TestData.warehouse(10, owner, 2000);
        LocalDate start = LocalDate.now().minusMonths(1);
        RentalContract contract = TestData.withId(
                new RentalContract(warehouse, new Tenant("Dimitar", "Stoyanov", "0877"), agent, start,
                        LocalDate.now().plusDays(3)), 100);
        contract.setStatus(ContractStatus.ACTIVE);

        notificationDao = new FakeNotificationDao();
        contractDao = new FakeContractDao();
        contractDao.endingSoon = List.of(contract);
        service = new NotificationService(notificationDao, contractDao);
    }

    @Test
    void expiringContractWarnsAgentAndOwner() {
        service.checkExpiringContracts();

        assertEquals(2, notificationDao.saved.size());
        assertTrue(notificationDao.saved.stream().allMatch(n -> n.getType() == NotificationType.CONTRACT_EXPIRING));
        assertEquals(List.of(agent, owner), notificationDao.saved.stream().map(Notification::getRecipient).toList());
    }

    @Test
    void expiringWarningIsSentOnlyOnce() {
        service.checkExpiringContracts();
        service.checkExpiringContracts();

        assertEquals(2, notificationDao.saved.size());
    }

    @Test
    void sendSavesNotificationForRecipient() {
        service.send(owner, NotificationType.WAREHOUSE_RENTED, "Rented", null);

        assertEquals(1, notificationDao.saved.size());
        assertEquals(owner, notificationDao.saved.get(0).getRecipient());
        assertEquals("Rented", notificationDao.saved.get(0).getMessage());
    }

    private static class FakeNotificationDao extends NotificationDao {
        final List<Notification> saved = new ArrayList<>();

        @Override
        public void save(Notification notification) {
            saved.add(notification);
        }

        @Override
        public boolean exists(User user, NotificationType type, RentalContract contract) {
            return saved.stream().anyMatch(n -> n.getRecipient() == user && n.getType() == type
                    && n.getContract() == contract);
        }
    }

    private static class FakeContractDao extends RentalContractDao {
        List<RentalContract> endingSoon = List.of();

        @Override
        public List<RentalContract> findActiveEndingBefore(LocalDate date) {
            return endingSoon;
        }
    }
}
