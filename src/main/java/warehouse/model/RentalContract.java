package warehouse.model;

import jakarta.persistence.*;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

@Entity
@Table(name = "rental_contracts")
public class RentalContract {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "warehouse_id")
    private Warehouse warehouse;

    @ManyToOne(optional = false)
    @JoinColumn(name = "tenant_id")
    private Tenant tenant;

    @ManyToOne(optional = false)
    @JoinColumn(name = "agent_id")
    private Agent agent;

    @Column(name = "start_date", nullable = false)
    private LocalDate startDate;

    @Column(name = "end_date", nullable = false)
    private LocalDate endDate;

    @Column(name = "price_per_month", nullable = false)
    private double pricePerMonth;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private ContractStatus status = ContractStatus.PENDING;

    @Column(name = "created_date", nullable = false)
    private LocalDate createdDate = LocalDate.now();

    protected RentalContract() {
    }

    public RentalContract(Warehouse warehouse, Tenant tenant, Agent agent,
                          LocalDate startDate, LocalDate endDate) {
        this.warehouse = warehouse;
        this.tenant = tenant;
        this.agent = agent;
        this.startDate = startDate;
        this.endDate = endDate;
        this.pricePerMonth = warehouse.getPricePerMonth();
    }

    public long getMonths() {
        return Math.max(1, ChronoUnit.MONTHS.between(startDate, endDate));
    }

    public double getTotalPrice() {
        return pricePerMonth * getMonths();
    }

    public double getAgentCommission() {
        return getTotalPrice() * agent.getFee() / 100;
    }

    public Long getId() { return id; }

    public Warehouse getWarehouse() { return warehouse; }
    public void setWarehouse(Warehouse warehouse) { this.warehouse = warehouse; }

    public Tenant getTenant() { return tenant; }
    public void setTenant(Tenant tenant) { this.tenant = tenant; }

    public Agent getAgent() { return agent; }
    public void setAgent(Agent agent) { this.agent = agent; }

    public LocalDate getStartDate() { return startDate; }
    public void setStartDate(LocalDate startDate) { this.startDate = startDate; }

    public LocalDate getEndDate() { return endDate; }
    public void setEndDate(LocalDate endDate) { this.endDate = endDate; }

    public double getPricePerMonth() { return pricePerMonth; }
    public void setPricePerMonth(double price) { this.pricePerMonth = price; }

    public ContractStatus getStatus() { return status; }
    public void setStatus(ContractStatus status) { this.status = status; }

    public LocalDate getCreatedDate() { return createdDate; }
}
