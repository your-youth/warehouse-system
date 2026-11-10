package warehouse.model;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class RatingTest {

    private final Owner owner = new Owner("owner", "hash", "Georgi", "Ivanov");
    private final Agent agent = new Agent("agent", "hash", "Maria", "Petrova", 10);

    @Test
    void acceptsScoresFromOneToFive() {
        assertEquals(1, new Rating(agent, owner, 1, null).getScore());
        assertEquals(5, new Rating(agent, owner, 5, "Great").getScore());
    }

    @Test
    void rejectsScoresOutsideOneToFive() {
        assertThrows(IllegalArgumentException.class, () -> new Rating(agent, owner, 0, null));
        assertThrows(IllegalArgumentException.class, () -> new Rating(agent, owner, 6, null));
    }
}
