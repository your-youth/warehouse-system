package warehouse.service;

import warehouse.dao.AgentDao;
import warehouse.dao.OwnerDao;
import warehouse.dao.UserDao;
import warehouse.model.Agent;
import warehouse.model.Owner;
import warehouse.util.PasswordUtil;

import java.util.List;

public class UserService {

    private final UserDao userDao;
    private final OwnerDao ownerDao;
    private final AgentDao agentDao;

    public UserService(UserDao userDao, OwnerDao ownerDao, AgentDao agentDao) {
        this.userDao = userDao;
        this.ownerDao = ownerDao;
        this.agentDao = agentDao;
    }

    public Owner createOwner(String username, String password,
                             String firstName, String lastName,
                             String phone, String email) {
        validateCredentials(username, password);
        requireNotBlank(firstName, "First name");
        requireNotBlank(lastName, "Last name");

        Owner owner = new Owner(username.trim(), PasswordUtil.hash(password),
                firstName.trim(), lastName.trim());
        owner.setPhone(phone);
        owner.setEmail(email);
        ownerDao.save(owner);
        return owner;
    }

    public Agent createAgent(String username, String password,
                             String firstName, String lastName,
                             String phone, String email, double fee) {
        validateCredentials(username, password);
        requireNotBlank(firstName, "First name");
        requireNotBlank(lastName, "Last name");
        if (fee < 0 || fee > 100) {
            throw new IllegalArgumentException("Fee must be between 0 and 100 %");
        }

        Agent agent = new Agent(username.trim(), PasswordUtil.hash(password),
                firstName.trim(), lastName.trim(), fee);
        agent.setPhone(phone);
        agent.setEmail(email);
        agentDao.save(agent);
        return agent;
    }

    public Owner updateOwner(Owner owner) {
        return ownerDao.update(owner);
    }

    public Agent updateAgent(Agent agent) {
        return agentDao.update(agent);
    }

    public List<Owner> getAllOwners() {
        return ownerDao.findAll();
    }

    public List<Agent> getAllAgents() {
        return agentDao.findAll();
    }

    private void validateCredentials(String username, String password) {
        requireNotBlank(username, "Username");
        if (password == null || password.length() < 4) {
            throw new IllegalArgumentException("Password must be at least 4 characters");
        }
        if (userDao.findByUsername(username.trim()).isPresent()) {
            throw new IllegalArgumentException("Username '" + username + "' is already taken");
        }
    }

    private void requireNotBlank(String value, String field) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(field + " is required");
        }
    }

    // редактирование профиля владельца
    public Owner updateOwnerProfile(Owner owner, String firstName, String lastName,
                                    String phone, String email) {
        if (owner == null) {
            throw new IllegalArgumentException("Select an owner in the table");
        }
        requireNotBlank(firstName, "First name");
        requireNotBlank(lastName, "Last name");
        owner.setFirstName(firstName.trim());
        owner.setLastName(lastName.trim());
        owner.setPhone(phone);
        owner.setEmail(email);
        return ownerDao.update(owner);
    }


    public Agent updateAgentProfile(Agent agent, String firstName, String lastName, String phone, String email, double fee) {
        if (agent == null) {
            throw new IllegalArgumentException("Select an agent in the table");
        }
        requireNotBlank(firstName, "First name");
        requireNotBlank(lastName, "Last name");
        if (fee < 0 || fee > 100) {
            throw new IllegalArgumentException("Fee must be between 0 and 100 %");
        }
        agent.setFirstName(firstName.trim());
        agent.setLastName(lastName.trim());
        agent.setPhone(phone);
        agent.setEmail(email);
        agent.setFee(fee);
        return agentDao.update(agent);
    }
}