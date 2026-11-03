package warehouse.util;

import warehouse.dao.UserDao;
import warehouse.model.Role;
import warehouse.model.User;

public final class DataInitializer {

    private DataInitializer() {
    }

    // при первом запуске создаёт администратора admin / admin
    public static void ensureAdminExists() {
        UserDao userDao = new UserDao();
        if (userDao.findByUsername("admin").isEmpty()) {
            userDao.save(new User("admin", PasswordUtil.hash("admin"), Role.ADMIN));
            System.out.println("Default admin created: admin / admin");
        }
    }
}
