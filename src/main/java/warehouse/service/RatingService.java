package warehouse.service;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import warehouse.dao.RatingDao;
import warehouse.model.Agent;
import warehouse.model.Owner;
import warehouse.model.Rating;

public class RatingService {

    private static final Logger log = LogManager.getLogger(RatingService.class);

    private final RatingDao ratingDao;

    public RatingService(RatingDao ratingDao) {
        this.ratingDao = ratingDao;
    }

    public void rateAgent(Owner owner, Agent agent, int score, String comment) {
        if (agent == null) {
            throw new IllegalArgumentException("Choose an agent");
        }
        ratingDao.save(new Rating(agent, owner, score, comment));
        log.info("Owner {} rated agent {}: {}/5", owner.getUsername(), agent.getUsername(), score);
    }

    public double getAverageRating(Agent agent) {
        return ratingDao.averageForAgent(agent);
    }
}