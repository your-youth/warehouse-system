package warehouse.util;

import warehouse.model.User;

/**
 * Keeps the currently logged-in user so every screen can access it
 * (e.g. an owner sees only their own warehouses).
 */
public final class UserSession {

    private static User currentUser;

    private UserSession() {
    }

    public static void login(User user) {
        currentUser = user;
    }

    public static void logout() {
        currentUser = null;
    }

    public static User getCurrentUser() {
        return currentUser;
    }

    public static boolean isLoggedIn() {
        return currentUser != null;
    }
}
