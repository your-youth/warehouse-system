package warehouse.service;

import warehouse.dao.RentalContractDao;
import warehouse.dao.WarehouseDao;
import warehouse.model.RentalContract;
import warehouse.model.Warehouse;
import warehouse.model.WarehouseStatus;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class ReportService {

    public record AgentReport(String agentName, int contracts, double revenue, double commission) {
    }

    public record WarehouseReport(String address, String owner, int contracts, double revenue) {
    }

    public record Summary(int contracts, double revenue, double commission,
                          long totalWarehouses, long rentedWarehouses) {

        public double occupancyPercent() {
            return totalWarehouses == 0 ? 0 : rentedWarehouses * 100.0 / totalWarehouses;
        }
    }

    private final RentalContractDao contractDao = new RentalContractDao();
    private final WarehouseDao warehouseDao = new WarehouseDao();

    public List<RentalContract> getContracts(LocalDate from, LocalDate to) {
        return contractDao.findStartedBetween(from, to);
    }

    public Summary getSummary(LocalDate from, LocalDate to) {
        List<RentalContract> contracts = getContracts(from, to);
        double revenue = contracts.stream().mapToDouble(RentalContract::getTotalPrice).sum();
        double commission = contracts.stream().mapToDouble(RentalContract::getAgentCommission).sum();

        List<Warehouse> warehouses = warehouseDao.findAll();
        long rented = warehouses.stream().filter(w -> w.getStatus() == WarehouseStatus.RENTED).count();

        return new Summary(contracts.size(), revenue, commission, warehouses.size(), rented);
    }

    public List<AgentReport> getAgentReport(LocalDate from, LocalDate to) {
        Map<Long, List<RentalContract>> byAgent = new LinkedHashMap<>();
        for (RentalContract c : getContracts(from, to)) {
            byAgent.computeIfAbsent(c.getAgent().getId(), id -> new ArrayList<>()).add(c);
        }

        List<AgentReport> result = new ArrayList<>();
        for (List<RentalContract> list : byAgent.values()) {
            result.add(new AgentReport(
                    list.get(0).getAgent().getFullName(),
                    list.size(),
                    list.stream().mapToDouble(RentalContract::getTotalPrice).sum(),
                    list.stream().mapToDouble(RentalContract::getAgentCommission).sum()));
        }
        result.sort(Comparator.comparingDouble(AgentReport::revenue).reversed());
        return result;
    }

    public List<WarehouseReport> getWarehouseReport(LocalDate from, LocalDate to) {
        Map<Long, List<RentalContract>> byWarehouse = new LinkedHashMap<>();
        for (RentalContract c : getContracts(from, to)) {
            byWarehouse.computeIfAbsent(c.getWarehouse().getId(), id -> new ArrayList<>()).add(c);
        }

        List<WarehouseReport> result = new ArrayList<>();
        for (List<RentalContract> list : byWarehouse.values()) {
            Warehouse w = list.get(0).getWarehouse();
            result.add(new WarehouseReport(
                    w.getAddress(),
                    w.getOwner().getFullName(),
                    list.size(),
                    list.stream().mapToDouble(RentalContract::getTotalPrice).sum()));
        }
        result.sort(Comparator.comparingDouble(WarehouseReport::revenue).reversed());
        return result;
    }
}
