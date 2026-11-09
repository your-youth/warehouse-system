package warehouse.model;

import jakarta.persistence.*;
import java.time.LocalDate;

@Entity
@Table(name = "ratings")
public class Rating {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "agent_id")
    private Agent agent;

    @ManyToOne(optional = false)
    @JoinColumn(name = "owner_id")
    private Owner owner;

    @Column(nullable = false)
    private int score;

    private String comment;

    @Column(name = "rating_date", nullable = false)
    private LocalDate date = LocalDate.now();

    protected Rating() {
    }

    public Rating(Agent agent, Owner owner, int score, String comment) {
        if (score < 1 || score > 5) {
            throw new IllegalArgumentException("Score must be between 1 and 5");
        }
        this.agent = agent;
        this.owner = owner;
        this.score = score;
        this.comment = comment;
    }

    public Long getId() {
        return id;
    }
    public Agent getAgent() {
        return agent;
    }
    public Owner getOwner() {
        return owner;
    }
    public int getScore() {
        return score;
    }
    public String getComment() {
        return comment;
    }
    public LocalDate getDate() {
        return date;
    }
}
