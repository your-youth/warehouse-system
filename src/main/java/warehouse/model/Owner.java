package warehouse.model;

import jakarta.persistence.*;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "owners")
public class Owner extends User {

    @Column(name = "first_name", nullable = false)
    private String firstName;

    @Column(name = "last_name", nullable = false)
    private String lastName;

    private String phone;
    private String email;

    @OneToMany(mappedBy = "owner")
    private List<Warehouse> warehouses = new ArrayList<>();

    protected Owner() {
    }

    public Owner(String username, String password, String firstName, String lastName) {
        super(username, password, Role.OWNER);
        this.firstName = firstName;
        this.lastName = lastName;
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

    public List<Warehouse> getWarehouses() { return warehouses; }
}