package warehouse.model;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class RatingTest {

    @Test
    void scoreAboveFiveIsRejected() {
        assertThrows(IllegalArgumentException.class,
                () -> new Rating(null, null, 6, "too high"));
    }

    @Test
    void scoreBelowOneIsRejected() {
        assertThrows(IllegalArgumentException.class,
                () -> new Rating(null, null, 0, "too low"));
    }

    @Test
    void validScoreIsAccepted() {
        Rating rating = new Rating(null, null, 5, "great");
        assertEquals(5, rating.getScore());
        assertEquals("great", rating.getComment());
    }
}