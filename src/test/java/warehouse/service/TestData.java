package warehouse.service;

import warehouse.model.Agent;
import warehouse.model.Owner;
import warehouse.model.Warehouse;

import java.lang.reflect.Field;

final class TestData {

    private TestData() {
    }

    static <T> T withId(T entity, long id) {
        Class<?> type = entity.getClass();
        while (type != null) {
            try {
                Field field = type.getDeclaredField("id");
                field.setAccessible(true);
                field.set(entity, id);
                return entity;
            } catch (NoSuchFieldException e) {
                type = type.getSuperclass();
            } catch (IllegalAccessException e) {
                throw new IllegalStateException(e);
            }
        }
        throw new IllegalArgumentException("No id field in " + entity.getClass());
    }

    static Owner owner(long id, String lastName) {
        return withId(new Owner("owner" + id, "hash", "Georgi", lastName), id);
    }

    static Agent agent(long id, String lastName, double fee) {
        Agent agent = withId(new Agent("agent" + id, "hash", "Maria", lastName, fee), id);
        agent.setPhone("0899 00" + id);
        agent.setEmail("agent" + id + "@example.bg");
        return agent;
    }

    static Warehouse warehouse(long id, Owner owner, double price) {
        Warehouse warehouse = withId(new Warehouse("Warehouse " + id, 250, owner), id);
        warehouse.setPricePerMonth(price);
        return warehouse;
    }
}
