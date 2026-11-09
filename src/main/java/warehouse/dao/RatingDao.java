package warehouse.dao;

import org.hibernate.Session;
import warehouse.model.Agent;
import warehouse.model.Rating;
import warehouse.util.HibernateUtil;

public class RatingDao extends GenericDao<Rating> {

    public RatingDao() {
        super(Rating.class);
    }

    public double averageForAgent(Agent agent) {
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            Double avg = session.createQuery(
                            "select avg(r.score) from Rating r where r.agent.id = :agentId", Double.class)
                    .setParameter("agentId", agent.getId())
                    .uniqueResult();
            return avg == null ? 0 : avg;
        }
    }
}
