package warehouse.model;

import jakarta.persistence.*;
import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "warehouses")
public class Warehouse {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String address;

    private double area;
    private double height;

    @Enumerated(EnumType.STRING)
    @Column(name = "climate_condition", length = 30)
    private ClimateCondition climateCondition;

    @Column(name = "goods_type")
    private String goodsType;

    @Enumerated(EnumType.STRING)
    @Column(length = 5)
    private WarehouseCategory category;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private WarehouseStatus status = WarehouseStatus.FREE;

    @Column(name = "price_per_month")
    private double pricePerMonth;

    @ManyToOne(optional = false)
    @JoinColumn(name = "owner_id")
    private Owner owner;

    @ManyToMany
    @JoinTable(name = "warehouse_agents",
            joinColumns = @JoinColumn(name = "warehouse_id"),
            inverseJoinColumns = @JoinColumn(name = "agent_id"))
    private Set<Agent> agents = new HashSet<>();

    protected Warehouse() {
    }

    public Warehouse(String address, double area, Owner owner) {
        this.address = address;
        this.area = area;
        this.owner = owner;
    }

    public void addAgent(Agent agent) {
        agents.add(agent);
        agent.getWarehouses().add(this);
    }

    public void removeAgent(Agent agent) {
        agents.remove(agent);
        agent.getWarehouses().remove(this);
    }

    public Long getId() { return id; }

    public String getAddress() { return address; }
    public void setAddress(String address) { this.address = address; }

    public double getArea() { return area; }
    public void setArea(double area) { this.area = area; }

    public double getHeight() { return height; }
    public void setHeight(double height) { this.height = height; }

    public ClimateCondition getClimateCondition() { return climateCondition; }
    public void setClimateCondition(ClimateCondition c) { this.climateCondition = c; }

    public String getGoodsType() { return goodsType; }
    public void setGoodsType(String goodsType) { this.goodsType = goodsType; }

    public WarehouseCategory getCategory() { return category; }
    public void setCategory(WarehouseCategory category) { this.category = category; }

    public WarehouseStatus getStatus() { return status; }
    public void setStatus(WarehouseStatus status) { this.status = status; }

    public double getPricePerMonth() { return pricePerMonth; }
    public void setPricePerMonth(double price) { this.pricePerMonth = price; }

    public Owner getOwner() { return owner; }
    public void setOwner(Owner owner) { this.owner = owner; }

    public Set<Agent> getAgents() { return agents; }
}