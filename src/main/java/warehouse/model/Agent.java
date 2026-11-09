package warehouse.model;

import jakarta.persistence.*;
import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "agents")
public class Agent extends User {

    @Column(name = "first_name", nullable = false)
    private String firstName;

    @Column(name = "last_name", nullable = false)
    private String lastName;

    private String phone;
    private String email;

    private double fee;

    @ManyToMany(mappedBy = "agents")
    private Set<Warehouse> warehouses = new HashSet<>();

    protected Agent() {
    }

    public Agent(String username, String password,
                 String firstName, String lastName, double fee) {
        super(username, password, Role.AGENT);
        this.firstName = firstName;
        this.lastName = lastName;
        this.fee = fee;
    }

    public String getFullName() {
        return firstName + " " + lastName;
    }

    public String getFirstName() { return firstName; }
    public void setFirstName(String firstName) { this.firstName = firstName; }

    public String getLastName() { return lastName; }
    public void setLastName(String lastName) { this.lastName = lastName; }

    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public double getFee() { return fee; }
    public void setFee(double fee) { this.fee = fee; }

    public Set<Warehouse> getWarehouses() { return warehouses; }
}