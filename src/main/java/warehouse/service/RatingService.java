package warehouse.service;

import warehouse.dao.RatingDao;
import warehouse.model.Agent;
import warehouse.model.Owner;
import warehouse.model.Rating;

public class RatingService {

    private final RatingDao ratingDao;

    public RatingService(RatingDao ratingDao) {
        this.ratingDao = ratingDao;
    }

    public void rateAgent(Owner owner, Agent agent, int score, String comment) {
        if (agent == null) {
            throw new IllegalArgumentException("Choose an agent");
        }
        ratingDao.save(new Rating(agent, owner, score, comment));
    }

    public double getAverageRating(Agent agent) {
        return ratingDao.averageForAgent(agent);
    }
}
