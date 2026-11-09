package warehouse.service;

import warehouse.dao.WarehouseDao;
import warehouse.model.*;

import java.util.List;

public class WarehouseService {

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
        return w;
    }

    public Warehouse updateWarehouse(Warehouse w) {
        validate(w.getAddress(), w.getArea(), w.getPricePerMonth());
        return warehouseDao.update(w);
    }

    public List<Warehouse> getWarehousesOf(Owner owner) {
        return warehouseDao.findByOwner(owner);
    }

    public void assignAgent(Warehouse warehouse, Agent agent) {
        warehouseDao.assignAgent(warehouse.getId(), agent.getId());
    }

    public void removeAgent(Warehouse warehouse, Agent agent) {
        warehouseDao.removeAgent(warehouse.getId(), agent.getId());
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
