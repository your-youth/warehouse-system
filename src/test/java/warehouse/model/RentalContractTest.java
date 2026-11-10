package warehouse.model;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;

class RentalContractTest {

    private Warehouse warehouse;
    private Agent agent;
    private Tenant tenant;

    @BeforeEach
    void setUp() {
        Owner owner = new Owner("owner", "hash", "Georgi", "Ivanov");
        warehouse = new Warehouse("bul. Vitosha 45, Sofia", 250, owner);
        warehouse.setPricePerMonth(2000);
        agent = new Agent("agent", "hash", "Maria", "Petrova", 10);
        tenant = new Tenant("Dimitar", "Stoyanov", "0877");
    }

    @Test
    void totalIsMonthsTimesMonthlyPrice() {
        LocalDate start = LocalDate.of(2026, 1, 1);
        RentalContract contract = new RentalContract(warehouse, tenant, agent, start, start.plusMonths(6));

        assertEquals(6, contract.getMonths());
        assertEquals(12000, contract.getTotalPrice());
    }

    @Test
    void commissionIsAgentFeePercentOfTotal() {
        LocalDate start = LocalDate.of(2026, 1, 1);
        RentalContract contract = new RentalContract(warehouse, tenant, agent, start, start.plusMonths(3));

        assertEquals(600, contract.getAgentCommission());
    }

    @Test
    void shortRentalCountsAsOneMonth() {
        LocalDate start = LocalDate.of(2026, 1, 1);
        RentalContract contract = new RentalContract(warehouse, tenant, agent, start, start.plusDays(5));

        assertEquals(1, contract.getMonths());
        assertEquals(2000, contract.getTotalPrice());
    }

    @Test
    void priceIsCopiedWhenContractIsCreated() {
        LocalDate start = LocalDate.of(2026, 1, 1);
        RentalContract contract = new RentalContract(warehouse, tenant, agent, start, start.plusMonths(2));
        warehouse.setPricePerMonth(5000);

        assertEquals(2000, contract.getPricePerMonth());
        assertEquals(4000, contract.getTotalPrice());
    }

    @Test
    void newContractIsPending() {
        LocalDate start = LocalDate.of(2026, 1, 1);
        RentalContract contract = new RentalContract(warehouse, tenant, agent, start, start.plusMonths(1));

        assertEquals(ContractStatus.PENDING, contract.getStatus());
    }
}
