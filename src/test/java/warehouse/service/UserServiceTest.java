package warehouse.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import warehouse.dao.AgentDao;
import warehouse.dao.OwnerDao;
import warehouse.dao.UserDao;
import warehouse.model.Agent;
import warehouse.model.Owner;
import warehouse.model.Role;
import warehouse.model.User;
import warehouse.util.PasswordUtil;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

class UserServiceTest {

    static class FakeUserDao extends UserDao {
        String takenUsername = "taken";

        @Override
        public Optional<User> findByUsername(String username) {
            if (username.equals(takenUsername)) {
                return Optional.of(new User(takenUsername, "x", Role.OWNER));
            }
            return Optional.empty();
        }
    }

    static class FakeOwnerDao extends OwnerDao {
        Owner saved;

        @Override
        public void save(Owner owner) {
            saved = owner;
        }
    }

    static class FakeAgentDao extends AgentDao {
        Agent saved;

        @Override
        public void save(Agent agent) {
            saved = agent;
        }
    }

    private FakeOwnerDao ownerDao;
    private FakeAgentDao agentDao;
    private UserService service;

    @BeforeEach
    void setUp() {
        ownerDao = new FakeOwnerDao();
        agentDao = new FakeAgentDao();
        service = new UserService(new FakeUserDao(), ownerDao, agentDao);
    }

    @Test
    void createOwnerSavesOwnerWithHashedPassword() {
        Owner owner = service.createOwner("ivan", "1234", "Ivan", "Petrov", "0888", "ivan@mail.bg");

        assertSame(owner, ownerDao.saved);
        assertEquals(Role.OWNER, owner.getRole());
        assertNotEquals("1234", owner.getPassword());
        assertTrue(PasswordUtil.matches("1234", owner.getPassword()));
    }

    @Test
    void emptyUsernameIsRejected() {
        assertThrows(IllegalArgumentException.class,
                () -> service.createOwner("  ", "1234", "Ivan", "Petrov", null, null));
        assertNull(ownerDao.saved);
    }

    @Test
    void shortPasswordIsRejected() {
        assertThrows(IllegalArgumentException.class,
                () -> service.createOwner("ivan", "12", "Ivan", "Petrov", null, null));
    }

    @Test
    void takenUsernameIsRejected() {
        IllegalArgumentException e = assertThrows(IllegalArgumentException.class,
                () -> service.createOwner("taken", "1234", "Ivan", "Petrov", null, null));
        assertTrue(e.getMessage().contains("already taken"));
    }

    @Test
    void createAgentSavesFee() {
        Agent agent = service.createAgent("maria", "1234", "Maria", "Ivanova", null, null, 5);

        assertSame(agent, agentDao.saved);
        assertEquals(Role.AGENT, agent.getRole());
        assertEquals(5, agent.getFee());
    }

    @Test
    void feeAboveHundredIsRejected() {
        assertThrows(IllegalArgumentException.class,
                () -> service.createAgent("maria", "1234", "Maria", "Ivanova", null, null, 150));
        assertNull(agentDao.saved);
    }

    @Test
    void updateAgentProfileRejectsNegativeFee() {
        Agent agent = new Agent("maria", "x", "Maria", "Ivanova", 5);
        assertThrows(IllegalArgumentException.class,
                () -> service.updateAgentProfile(agent, "Maria", "Ivanova", null, null, -1));
    }
}
