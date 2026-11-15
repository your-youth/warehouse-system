package warehouse.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import warehouse.dao.WarehouseDao;
import warehouse.model.*;

import static org.junit.jupiter.api.Assertions.*;

class WarehouseServiceTest {

    static class FakeWarehouseDao extends WarehouseDao {
        Warehouse saved;

        @Override
        public void save(Warehouse warehouse) {
            saved = warehouse;
        }
    }

    private FakeWarehouseDao dao;
    private WarehouseService service;
    private Owner owner;

    @BeforeEach
    void setUp() {
        dao = new FakeWarehouseDao();
        service = new WarehouseService(dao);
        owner = new Owner("owner1", "x", "Ivan", "Petrov");
    }

    @Test
    void addWarehouseSavesItAsFree() {
        Warehouse w = service.addWarehouse(owner, "Varna, Primorska 10", 120, 6,
                ClimateCondition.COOLED, "food", WarehouseCategory.A, 1500);

        assertSame(w, dao.saved);
        assertEquals(WarehouseStatus.FREE, w.getStatus());
        assertEquals(owner, w.getOwner());
        assertEquals(1500, w.getPricePerMonth());
    }

    @Test
    void emptyAddressIsRejected() {
        assertThrows(IllegalArgumentException.class,
                () -> service.addWarehouse(owner, "", 120, 6, null, null, null, 1500));
        assertNull(dao.saved);
    }

    @Test
    void zeroAreaIsRejected() {
        assertThrows(IllegalArgumentException.class,
                () -> service.addWarehouse(owner, "Varna", 0, 6, null, null, null, 1500));
    }

    @Test
    void negativePriceIsRejected() {
        assertThrows(IllegalArgumentException.class,
                () -> service.addWarehouse(owner, "Varna", 120, 6, null, null, null, -10));
    }
}