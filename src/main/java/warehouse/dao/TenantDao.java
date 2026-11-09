package warehouse.dao;

import org.hibernate.Session;
import warehouse.model.Tenant;
import warehouse.util.HibernateUtil;

import java.util.List;

public class TenantDao extends GenericDao<Tenant> {

    public TenantDao() {
        super(Tenant.class);
    }

    public List<Tenant> search(String text) {
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            return session.createQuery(
                            "from Tenant t where lower(t.firstName) like :text or lower(t.lastName) like :text"
                                    + " or lower(t.companyName) like :text or t.phone like :text", Tenant.class)
                    .setParameter("text", "%" + text.toLowerCase() + "%")
                    .list();
        }
    }
}
