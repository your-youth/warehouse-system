package warehouse.service;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import warehouse.dao.WarehouseDao;
import warehouse.model.*;

import java.util.List;

public class WarehouseService {

    private static final Logger log = LogManager.getLogger(WarehouseService.class);

    private final WarehouseDao warehouseDao;

    public WarehouseService(WarehouseDao warehouseDao) {
        this.warehouseDao = warehouseDao;
    }

    public Warehouse addWarehouse(Owner owner, String address, double area, double height,
                                  ClimateCondition climate, String goodsType,
                                  WarehouseCategory category, double pricePerMonth) {
        validate(address, area, pricePerMonth);
        Warehouse w = new Warehouse(address.trim(), area, owner);
        w.setHeight(height);
        w.setClimateCondition(climate);
        w.setGoodsType(goodsType);
        w.setCategory(category);
        w.setPricePerMonth(pricePerMonth);
        warehouseDao.save(w);
        log.info("Warehouse added: '{}' by owner {}", w.getAddress(), owner.getUsername());
        return w;
    }

    public Warehouse updateWarehouse(Warehouse w) {
        validate(w.getAddress(), w.getArea(), w.getPricePerMonth());
        Warehouse current = warehouseDao.findById(w.getId())
                .orElseThrow(() -> new IllegalArgumentException("This warehouse no longer exists"));
        w.setStatus(current.getStatus());
        log.info("Warehouse updated: id={}", w.getId());
        return warehouseDao.update(w);
    }

    public List<Warehouse> getWarehousesOf(Owner owner) {
        return warehouseDao.findByOwner(owner);
    }

    public void assignAgent(Warehouse warehouse, Agent agent) {
        warehouseDao.assignAgent(warehouse.getId(), agent.getId());
        log.info("Agent {} assigned to warehouse id={}", agent.getUsername(), warehouse.getId());
    }

    public void removeAgent(Warehouse warehouse, Agent agent) {
        warehouseDao.removeAgent(warehouse.getId(), agent.getId());
        log.info("Agent {} removed from warehouse id={}", agent.getUsername(), warehouse.getId());
    }

    private void validate(String address, double area, double price) {
        if (address == null || address.isBlank()) {
            throw new IllegalArgumentException("Address is required");
        }
        if (area <= 0) {
            throw new IllegalArgumentException("Area must be greater than 0");
        }
        if (price < 0) {
            throw new IllegalArgumentException("Price cannot be negative");
        }
    }
}