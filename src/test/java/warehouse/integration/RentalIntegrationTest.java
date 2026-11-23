package warehouse.integration;

import org.hibernate.Session;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import warehouse.dao.AgentDao;
import warehouse.dao.NotificationDao;
import warehouse.dao.OwnerDao;
import warehouse.dao.RatingDao;
import warehouse.dao.RentalContractDao;
import warehouse.dao.TenantDao;
import warehouse.dao.UserDao;
import warehouse.dao.WarehouseDao;
import warehouse.model.Agent;
import warehouse.model.ClimateCondition;
import warehouse.model.ContractStatus;
import warehouse.model.NotificationType;
import warehouse.model.Owner;
import warehouse.model.RentalContract;
import warehouse.model.Tenant;
import warehouse.model.User;
import warehouse.model.Warehouse;
import warehouse.model.WarehouseCategory;
import warehouse.model.WarehouseStatus;
import warehouse.service.AuthService;
import warehouse.service.NotificationService;
import warehouse.service.RatingService;
import warehouse.service.RentalService;
import warehouse.service.ReportService;
import warehouse.service.UserService;
import warehouse.service.WarehouseService;
import warehouse.util.HibernateUtil;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

class RentalIntegrationTest {

    private static boolean databaseAvailable;

    private final Set<Long> userIds = new LinkedHashSet<>();
    private final Set<Long> warehouseIds = new LinkedHashSet<>();

    private UserService userService;
    private WarehouseService warehouseService;
    private NotificationService notificationService;
    private ReportService reportService;
    private String suffix;

    @BeforeAll
    static void connect() {
        try {
            HibernateUtil.getSessionFactory();
            databaseAvailable = true;
        } catch (RuntimeException e) {
            databaseAvailable = false;
        }
    }

    @BeforeEach
    void setUp() {
        assumeTrue(databaseAvailable, "PostgreSQL is not reachable, integration tests skipped");
        suffix = "it" + System.nanoTime();
        userService = new UserService(new UserDao(), new OwnerDao(), new AgentDao());
        warehouseService = new WarehouseService(new WarehouseDao());
        notificationService = new NotificationService(new NotificationDao(), new RentalContractDao());
        reportService = new ReportService(new RentalContractDao(), new WarehouseDao(), new OwnerDao(),
                new TenantDao(), new RatingService(new RatingDao()));
    }

    @AfterEach
    void cleanUp() {
        if (!databaseAvailable || userIds.isEmpty()) {
            return;
        }
        String users = join(userIds);
        String warehouses = warehouseIds.isEmpty() ? "-1" : join(warehouseIds);
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            var tx = session.beginTransaction();
            for (String sql : List.of(
                    "delete from notifications where recipient_id in (" + users + ")",
                    "delete from ratings where agent_id in (" + users + ")",
                    "create temp table it_tenants on commit drop as"
                            + " select tenant_id from rental_contracts where warehouse_id in (" + warehouses + ")",
                    "delete from rental_contracts where warehouse_id in (" + warehouses + ")",
                    "delete from tenants where id in (select tenant_id from it_tenants)",
                    "delete from warehouse_agents where warehouse_id in (" + warehouses + ")",
                    "delete from warehouses where id in (" + warehouses + ")",
                    "delete from agents where id in (" + users + ")",
                    "delete from owners where id in (" + users + ")",
                    "delete from users where id in (" + users + ")")) {
                session.createNativeMutationQuery(sql).executeUpdate();
            }
            tx.commit();
        }
    }

    @Test
    void createdUsersCanLogInWithTheirRole() {
        Owner owner = owner("Georgi", "Ivanov");
        Agent agent = agent("Maria", "Petrova");
        AuthService auth = new AuthService(new UserDao());

        Optional<User> ownerLogin = auth.login(owner.getUsername(), "test1234");
        Optional<User> agentLogin = auth.login(agent.getUsername(), "test1234");

        assertTrue(ownerLogin.orElseThrow() instanceof Owner);
        assertTrue(agentLogin.orElseThrow() instanceof Agent);
        assertTrue(auth.login(owner.getUsername(), "wrong").isEmpty());
    }

    @Test
    void fullRentalFlowFromRequestToReport() {
        Owner owner = owner("Georgi", "Ivanov");
        Agent agent = agent("Maria", "Petrova");
        Warehouse warehouse = warehouse(owner, "bul. Vitosha 45, Sofia", 2000);
        warehouseService.assignAgent(warehouse, agent);

        RentalContract request = rental().createRequest(freeWarehouseOf(agent), new Tenant("Dimitar", "Stoyanov", "0877"),
                agent, LocalDate.now(), LocalDate.now().plusMonths(3));
        assertEquals(ContractStatus.PENDING, request.getStatus());
        assertEquals(1, count(agent, NotificationType.NEW_RENTAL_REQUEST));

        RentalContract contract = rental().confirm(request, agent);
        assertEquals(ContractStatus.ACTIVE, contract.getStatus());
        assertEquals(WarehouseStatus.RENTED, new WarehouseDao().findById(warehouse.getId()).orElseThrow().getStatus());
        assertEquals(1, count(owner, NotificationType.WAREHOUSE_RENTED));

        List<RentalContract> ownerReport = reportService.getContracts(LocalDate.now().withDayOfMonth(1),
                LocalDate.now().plusDays(1), owner);
        assertEquals(1, ownerReport.size());
        assertEquals(6000, ownerReport.get(0).getTotalPrice());
    }

    @Test
    void unassignedAgentCannotRequestWarehouse() {
        Owner owner = owner("Georgi", "Ivanov");
        Agent assigned = agent("Maria", "Petrova");
        Agent other = agent("Ivan", "Georgiev");
        Warehouse warehouse = warehouse(owner, "bul. Vitosha 45, Sofia", 2000);
        warehouseService.assignAgent(warehouse, assigned);
        Warehouse free = freeWarehouseOf(assigned);

        assertThrows(IllegalStateException.class, () -> rental().createRequest(free,
                new Tenant("X", "Y", "1"), other, LocalDate.now(), LocalDate.now().plusMonths(1)));
    }

    @Test
    void parallelRequestsCreateOnlyOneContract() throws InterruptedException {
        Owner owner = owner("Georgi", "Ivanov");
        Agent first = agent("Maria", "Petrova");
        Agent second = agent("Ivan", "Georgiev");
        Warehouse warehouse = warehouse(owner, "bul. Slivnitsa 8, Varna", 3000);
        warehouseService.assignAgent(warehouse, first);
        warehouseService.assignAgent(warehouse, second);
        Warehouse free = freeWarehouseOf(first);

        AtomicInteger created = new AtomicInteger();
        AtomicInteger blocked = new AtomicInteger();
        runInParallel(8, i -> {
            Agent agent = i % 2 == 0 ? first : second;
            try {
                rental().createRequest(free, new Tenant("Parallel", "Tenant" + i, "1"), agent,
                        LocalDate.now().plusDays(1), LocalDate.now().plusMonths(2));
                created.incrementAndGet();
            } catch (IllegalStateException e) {
                blocked.incrementAndGet();
            }
        });

        assertEquals(1, created.get());
        assertEquals(7, blocked.get());
    }

    @Test
    void parallelConfirmationsSucceedOnlyOnce() throws InterruptedException {
        Owner owner = owner("Georgi", "Ivanov");
        Agent first = agent("Maria", "Petrova");
        Agent second = agent("Ivan", "Georgiev");
        Warehouse warehouse = warehouse(owner, "bul. Slivnitsa 8, Varna", 3000);
        warehouseService.assignAgent(warehouse, first);
        warehouseService.assignAgent(warehouse, second);
        RentalContract request = rental().createRequest(freeWarehouseOf(first), new Tenant("Dimitar", "Stoyanov", "0877"),
                first, LocalDate.now(), LocalDate.now().plusMonths(1));

        AtomicInteger confirmed = new AtomicInteger();
        AtomicInteger blocked = new AtomicInteger();
        runInParallel(2, i -> {
            try {
                rental().confirm(request, i == 0 ? first : second);
                confirmed.incrementAndGet();
            } catch (IllegalStateException e) {
                blocked.incrementAndGet();
            }
        });

        assertEquals(1, confirmed.get());
        assertEquals(1, blocked.get());
    }

    @Test
    void ownersAndAgentsSeeOnlyTheirOwnData() {
        Owner ownerA = owner("Georgi", "Ivanov");
        Owner ownerB = owner("Elena", "Dimitrova");
        Agent agentA = agent("Maria", "Petrova");
        Agent agentB = agent("Ivan", "Georgiev");
        Warehouse sofia = warehouse(ownerA, "bul. Vitosha 45, Sofia", 2000);
        Warehouse varna = warehouse(ownerB, "bul. Slivnitsa 8, Varna", 3000);
        warehouseService.assignAgent(sofia, agentA);
        warehouseService.assignAgent(varna, agentB);

        RentalContract request = rental().createRequest(freeWarehouseOf(agentA), new Tenant("Dimitar", "Stoyanov", "0877"),
                agentA, LocalDate.now(), LocalDate.now().plusMonths(2));
        rental().confirm(request, agentA);

        assertEquals(List.of(sofia.getId()), ids(warehouseService.getWarehousesOf(ownerA)));
        assertEquals(List.of(varna.getId()), ids(warehouseService.getWarehousesOf(ownerB)));
        assertEquals(0, count(agentB, NotificationType.NEW_RENTAL_REQUEST));
        assertEquals(0, notificationService.getFor(ownerB).size());
        assertTrue(rental().getContractsOf(agentB).isEmpty());
        assertTrue(reportService.getContracts(LocalDate.now().withDayOfMonth(1), LocalDate.now().plusDays(1), ownerB).isEmpty());
    }

    @Test
    void tenantHistoryIsFilteredForOwners() {
        Owner ownerA = owner("Georgi", "Ivanov");
        Owner ownerB = owner("Elena", "Dimitrova");
        Agent agentA = agent("Maria", "Petrova");
        Agent agentB = agent("Ivan", "Georgiev");
        warehouseService.assignAgent(warehouse(ownerA, "bul. Vitosha 45, Sofia", 2000), agentA);
        warehouseService.assignAgent(warehouse(ownerB, "bul. Slivnitsa 8, Varna", 3000), agentB);

        Tenant tenant = new Tenant("Dimitar", "Stoyanov", "0877");
        rental().confirm(rental().createRequest(freeWarehouseOf(agentA), tenant, agentA,
                LocalDate.now(), LocalDate.now().plusMonths(1)), agentA);
        Tenant saved = new TenantDao().findById(tenant.getId()).orElseThrow();
        rental().confirm(rental().createRequest(freeWarehouseOf(agentB), saved, agentB,
                LocalDate.now(), LocalDate.now().plusMonths(1)), agentB);

        assertEquals(2, reportService.getTenantHistory(saved, agentA).size());
        assertEquals(1, reportService.getTenantHistory(saved, ownerA).size());
    }

    private RentalService rental() {
        return new RentalService(new RentalContractDao(), new TenantDao(), new WarehouseDao());
    }

    private Owner owner(String firstName, String lastName) {
        Owner owner = userService.createOwner("owner" + suffix + userIds.size(), "test1234", firstName, lastName, "", "");
        userIds.add(owner.getId());
        return owner;
    }

    private Agent agent(String firstName, String lastName) {
        Agent agent = userService.createAgent("agent" + suffix + userIds.size(), "test1234", firstName, lastName, "", "", 10);
        userIds.add(agent.getId());
        return agent;
    }

    private Warehouse warehouse(Owner owner, String address, double price) {
        Warehouse warehouse = warehouseService.addWarehouse(owner, address, 250, 8, ClimateCondition.NORMAL,
                "General", WarehouseCategory.A, price);
        warehouseIds.add(warehouse.getId());
        return warehouse;
    }

    private Warehouse freeWarehouseOf(Agent agent) {
        return rental().getFreeWarehousesOf(agent).stream()
                .filter(w -> warehouseIds.contains(w.getId()))
                .findFirst()
                .orElseThrow();
    }

    private long count(User user, NotificationType type) {
        return notificationService.getFor(user).stream().filter(n -> n.getType() == type).count();
    }

    private static List<Long> ids(List<Warehouse> warehouses) {
        return warehouses.stream().map(Warehouse::getId).toList();
    }

    private static String join(Set<Long> ids) {
        return String.join(",", ids.stream().map(String::valueOf).toList());
    }

    private interface Task {
        void run(int index);
    }

    private static void runInParallel(int threads, Task task) throws InterruptedException {
        ExecutorService pool = Executors.newFixedThreadPool(threads);
        CountDownLatch start = new CountDownLatch(1);
        List<Throwable> errors = new ArrayList<>();
        for (int i = 0; i < threads; i++) {
            int index = i;
            pool.submit(() -> {
                try {
                    start.await();
                    task.run(index);
                } catch (Throwable e) {
                    synchronized (errors) {
                        errors.add(e);
                    }
                }
            });
        }
        start.countDown();
        pool.shutdown();
        assertTrue(pool.awaitTermination(60, TimeUnit.SECONDS));
        assertTrue(errors.isEmpty(), "Unexpected errors: " + errors);
    }
}
