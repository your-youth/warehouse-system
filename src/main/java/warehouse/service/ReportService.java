package warehouse.service;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import warehouse.dao.OwnerDao;
import warehouse.dao.RentalContractDao;
import warehouse.dao.WarehouseDao;
import warehouse.model.Agent;
import warehouse.model.Owner;
import warehouse.model.RentalContract;
import warehouse.model.User;
import warehouse.model.Warehouse;
import warehouse.model.WarehouseStatus;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class ReportService {

    private static final Logger log = LogManager.getLogger(ReportService.class);

    public record AgentReport(String agentName, String phone, String email, double fee, double rating,
                              int contracts, double revenue, double commission) {
    }

    public record WarehouseReport(String address, String owner, int contracts, double revenue) {
    }

    public record Summary(int contracts, double revenue, double commission,
                          long totalWarehouses, long rentedWarehouses) {

        public double occupancyPercent() {
            return totalWarehouses == 0 ? 0 : rentedWarehouses * 100.0 / totalWarehouses;
        }
    }

    private final RentalContractDao contractDao;
    private final WarehouseDao warehouseDao;
    private final OwnerDao ownerDao;
    private final RatingService ratingService;

    public ReportService(RentalContractDao contractDao, WarehouseDao warehouseDao,
                         OwnerDao ownerDao, RatingService ratingService) {
        this.contractDao = contractDao;
        this.warehouseDao = warehouseDao;
        this.ownerDao = ownerDao;
        this.ratingService = ratingService;
    }

    public List<RentalContract> getContracts(LocalDate from, LocalDate to, User viewer) {
        List<RentalContract> contracts = contractDao.findStartedBetween(from, to);
        if (viewer instanceof Owner owner) {
            contracts.removeIf(c -> !c.getWarehouse().getOwner().getId().equals(owner.getId()));
        } else if (viewer instanceof Agent agent) {
            contracts.removeIf(c -> !c.getAgent().getId().equals(agent.getId()));
        }
        return contracts;
    }

    public Summary getSummary(LocalDate from, LocalDate to, User viewer) {
        List<RentalContract> contracts = getContracts(from, to, viewer);
        double revenue = contracts.stream().mapToDouble(RentalContract::getTotalPrice).sum();
        double commission = contracts.stream().mapToDouble(RentalContract::getAgentCommission).sum();

        List<Warehouse> warehouses = viewer instanceof Owner owner
                ? warehouseDao.findByOwner(owner)
                : warehouseDao.findAll();
        long rented = warehouses.stream().filter(w -> w.getStatus() == WarehouseStatus.RENTED).count();

        log.info("Report summary {} to {} for {}", from, to, viewer == null ? "-" : viewer.getUsername());
        return new Summary(contracts.size(), revenue, commission, warehouses.size(), rented);
    }

    public List<AgentReport> getAgentReport(LocalDate from, LocalDate to, User viewer) {
        Map<Long, List<RentalContract>> byAgent = new LinkedHashMap<>();
        for (RentalContract c : getContracts(from, to, viewer)) {
            byAgent.computeIfAbsent(c.getAgent().getId(), id -> new ArrayList<>()).add(c);
        }

        List<AgentReport> result = new ArrayList<>();
        for (List<RentalContract> list : byAgent.values()) {
            Agent agent = list.get(0).getAgent();
            result.add(new AgentReport(
                    agent.getFullName(),
                    agent.getPhone(),
                    agent.getEmail(),
                    agent.getFee(),
                    ratingService.getAverageRating(agent),
                    list.size(),
                    list.stream().mapToDouble(RentalContract::getTotalPrice).sum(),
                    list.stream().mapToDouble(RentalContract::getAgentCommission).sum()));
        }
        result.sort(Comparator.comparingDouble(AgentReport::revenue).reversed());
        return result;
    }

    public List<WarehouseReport> getWarehouseReport(LocalDate from, LocalDate to, User viewer) {
        Map<Long, List<RentalContract>> byWarehouse = new LinkedHashMap<>();
        for (RentalContract c : getContracts(from, to, viewer)) {
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

    public List<Warehouse> getAvailableWarehouses(User viewer) {
        List<Warehouse> warehouses;
        if (viewer instanceof Owner owner) {
            warehouses = new ArrayList<>(warehouseDao.findByOwner(owner));
            warehouses.removeIf(w -> w.getStatus() != WarehouseStatus.FREE);
        } else if (viewer instanceof Agent agent) {
            warehouses = contractDao.findFreeWarehousesAssignedTo(agent);
        } else {
            warehouses = warehouseDao.findFree();
        }
        warehouses.sort(Comparator.comparing(Warehouse::getAddress));
        return warehouses;
    }

    public List<Warehouse> getWarehousesOf(Owner owner) {
        List<Warehouse> warehouses = new ArrayList<>(warehouseDao.findByOwner(owner));
        warehouses.sort(Comparator.comparing(Warehouse::getAddress));
        return warehouses;
    }

    public List<Owner> getOwnersFor(User viewer) {
        if (viewer instanceof Owner owner) {
            return List.of(owner);
        }
        List<Owner> owners = new ArrayList<>(ownerDao.findAll());
        owners.sort(Comparator.comparing(Owner::getFullName));
        return owners;
    }
}
