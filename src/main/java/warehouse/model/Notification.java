package warehouse.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "notifications")
public class Notification {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "recipient_id")
    private User recipient;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private NotificationType type;

    @Column(nullable = false, length = 500)
    private String message;

    @ManyToOne
    @JoinColumn(name = "contract_id")
    private RentalContract contract;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(name = "is_read", nullable = false)
    private boolean read;

    protected Notification() {
    }

    public Notification(User recipient, NotificationType type, String message, RentalContract contract) {
        this.recipient = recipient;
        this.type = type;
        this.message = message;
        this.contract = contract;
    }

    public Long getId() { return id; }

    public User getRecipient() { return recipient; }

    public NotificationType getType() { return type; }

    public String getMessage() { return message; }

    public RentalContract getContract() { return contract; }

    public LocalDateTime getCreatedAt() { return createdAt; }

    public boolean isRead() { return read; }
    public void setRead(boolean read) { this.read = read; }
}
