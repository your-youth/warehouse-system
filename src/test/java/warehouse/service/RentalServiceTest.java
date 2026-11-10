package warehouse.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import warehouse.model.Agent;
import warehouse.model.Owner;
import warehouse.model.Tenant;
import warehouse.model.Warehouse;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertThrows;

class RentalServiceTest {

    private final RentalService service = new RentalService();
    private Warehouse warehouse;
    private Agent agent;
    private Tenant tenant;

    @BeforeEach
    void setUp() {
        Owner owner = new Owner("owner", "hash", "Georgi", "Ivanov");
        warehouse = new Warehouse("bul. Vitosha 45, Sofia", 250, owner);
        agent = new Agent("agent", "hash", "Maria", "Petrova", 10);
        tenant = new Tenant("Dimitar", "Stoyanov", "0877");
    }

    @Test
    void rejectsMissingDates() {
        assertThrows(IllegalArgumentException.class,
                () -> service.rent(warehouse, tenant, agent, null, LocalDate.now().plusMonths(1)));
    }

    @Test
    void rejectsEndDateBeforeStartDate() {
        LocalDate start = LocalDate.now().plusDays(10);
        assertThrows(IllegalArgumentException.class,
                () -> service.rent(warehouse, tenant, agent, start, start.minusDays(1)));
    }

    @Test
    void rejectsStartDateInThePast() {
        LocalDate start = LocalDate.now().minusDays(1);
        assertThrows(IllegalArgumentException.class,
                () -> service.rent(warehouse, tenant, agent, start, start.plusMonths(1)));
    }
}
