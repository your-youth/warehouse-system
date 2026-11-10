package warehouse.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import warehouse.dao.OwnerDao;
import warehouse.dao.RatingDao;
import warehouse.dao.RentalContractDao;
import warehouse.dao.WarehouseDao;
import warehouse.model.Agent;
import warehouse.model.ContractStatus;
import warehouse.model.Owner;
import warehouse.model.RentalContract;
import warehouse.model.Tenant;
import warehouse.model.Warehouse;
import warehouse.model.WarehouseStatus;
import warehouse.service.ReportService.AgentReport;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ReportServiceTest {

    private static final LocalDate FROM = LocalDate.of(2026, 1, 1);
    private static final LocalDate TO = LocalDate.of(2026, 12, 31);

    private Owner ivanov;
    private Owner dimitrov;
    private Agent petrova;
    private Warehouse sofia;
    private Warehouse plovdiv;
    private ReportService service;

    @BeforeEach
    void setUp() {
        ivanov = TestData.owner(1, "Ivanov");
        dimitrov = TestData.owner(2, "Dimitrov");
        petrova = TestData.agent(3, "Petrova", 10);
        Agent georgieva = TestData.agent(4, "Georgieva", 5);
        sofia = TestData.warehouse(10, ivanov, 2000);
        plovdiv = TestData.warehouse(11, dimitrov, 3000);
        sofia.setStatus(WarehouseStatus.RENTED);

        Tenant tenant = new Tenant("Dimitar", "Stoyanov", "0877");
        LocalDate start = LocalDate.of(2026, 3, 1);
        RentalContract first = new RentalContract(sofia, tenant, petrova, start, start.plusMonths(6));
        RentalContract second = new RentalContract(plovdiv, tenant, georgieva, start, start.plusMonths(2));
        first.setStatus(ContractStatus.ACTIVE);
        second.setStatus(ContractStatus.ACTIVE);

        FakeContractDao contractDao = new FakeContractDao(new ArrayList<>(List.of(first, second)));
        FakeWarehouseDao warehouseDao = new FakeWarehouseDao(List.of(sofia, plovdiv));
        RatingService ratingService = new RatingService(new FakeRatingDao());
        service = new ReportService(contractDao, warehouseDao, new OwnerDao(), ratingService);
    }

    @Test
    void ownerSeesOnlyContractsForTheirWarehouses() {
        assertEquals(1, service.getContracts(FROM, TO, ivanov).size());
        assertEquals(sofia, service.getContracts(FROM, TO, ivanov).get(0).getWarehouse());
    }

    @Test
    void adminSeesAllContracts() {
        assertEquals(2, service.getContracts(FROM, TO, null).size());
    }

    @Test
    void agentReportHasCuratorDetailsAndRating() {
        List<AgentReport> report = service.getAgentReport(FROM, TO, petrova);

        assertEquals(1, report.size());
        AgentReport row = report.get(0);
        assertEquals("Maria Petrova", row.agentName());
        assertEquals("0899 003", row.phone());
        assertEquals("agent3@example.bg", row.email());
        assertEquals(10, row.fee());
        assertEquals(4.5, row.rating());
        assertEquals(12000, row.revenue());
        assertEquals(1200, row.commission());
    }

    @Test
    void summaryForOwnerCountsOnlyTheirWarehouses() {
        ReportService.Summary summary = service.getSummary(FROM, TO, ivanov);

        assertEquals(1, summary.contracts());
        assertEquals(12000, summary.revenue());
        assertEquals(1, summary.totalWarehouses());
        assertEquals(100, summary.occupancyPercent());
    }

    @Test
    void availableWarehousesAreOnlyFreeOnes() {
        List<Warehouse> available = service.getAvailableWarehouses(null);

        assertEquals(List.of(plovdiv), available);
    }

    @Test
    void ownerReportListsThatOwnersWarehouses() {
        assertEquals(List.of(plovdiv), service.getWarehousesOf(dimitrov));
    }

    private static class FakeContractDao extends RentalContractDao {
        private final List<RentalContract> contracts;

        FakeContractDao(List<RentalContract> contracts) {
            this.contracts = contracts;
        }

        @Override
        public List<RentalContract> findStartedBetween(LocalDate from, LocalDate to) {
            return new ArrayList<>(contracts);
        }
    }

    private static class FakeWarehouseDao extends WarehouseDao {
        private final List<Warehouse> warehouses;

        FakeWarehouseDao(List<Warehouse> warehouses) {
            this.warehouses = warehouses;
        }

        @Override
        public List<Warehouse> findAll() {
            return new ArrayList<>(warehouses);
        }

        @Override
        public List<Warehouse> findByOwner(Owner owner) {
            return warehouses.stream().filter(w -> w.getOwner().getId().equals(owner.getId())).toList();
        }

        @Override
        public List<Warehouse> findFree() {
            return new ArrayList<>(warehouses.stream().filter(w -> w.getStatus() == WarehouseStatus.FREE).toList());
        }
    }

    private static class FakeRatingDao extends RatingDao {
        @Override
        public double averageForAgent(Agent agent) {
            return 4.5;
        }
    }
}
