package warehouse.service;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import warehouse.dao.NotificationDao;
import warehouse.dao.RentalContractDao;
import warehouse.model.Notification;
import warehouse.model.NotificationType;
import warehouse.model.RentalContract;
import warehouse.model.User;

import java.time.LocalDate;
import java.util.List;

public class NotificationService {

    public static final int EXPIRING_DAYS = 7;

    private static final Logger log = LogManager.getLogger(NotificationService.class);

    private final NotificationDao notificationDao;
    private final RentalContractDao contractDao;

    public NotificationService(NotificationDao notificationDao, RentalContractDao contractDao) {
        this.notificationDao = notificationDao;
        this.contractDao = contractDao;
    }

    public void send(User recipient, NotificationType type, String message, RentalContract contract) {
        notificationDao.save(new Notification(recipient, type, message, contract));
        log.info("Notification {} sent to {}", type, recipient.getUsername());
    }

    public List<Notification> getFor(User user) {
        return notificationDao.findByRecipient(user);
    }

    public long countUnread(User user) {
        return notificationDao.countUnread(user);
    }

    public void markAllRead(User user) {
        notificationDao.markAllRead(user);
        log.info("All notifications marked as read by {}", user.getUsername());
    }

    public void checkExpiringContracts() {
        LocalDate limit = LocalDate.now().plusDays(EXPIRING_DAYS);
        for (RentalContract contract : contractDao.findActiveEndingBefore(limit)) {
            String message = "Contract for " + contract.getWarehouse().getAddress()
                    + " (" + contract.getTenant().getFullName() + ") ends on " + contract.getEndDate();
            sendOnce(contract.getAgent(), contract, message);
            sendOnce(contract.getWarehouse().getOwner(), contract, message);
        }
    }

    private void sendOnce(User recipient, RentalContract contract, String message) {
        if (!notificationDao.exists(recipient, NotificationType.CONTRACT_EXPIRING, contract)) {
            send(recipient, NotificationType.CONTRACT_EXPIRING, message, contract);
        }
    }
}
