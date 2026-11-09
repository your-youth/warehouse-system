package warehouse.dao;

import warehouse.model.Owner;

public class OwnerDao extends GenericDao<Owner> {
    public OwnerDao() {
        super(Owner.class);
    }
}