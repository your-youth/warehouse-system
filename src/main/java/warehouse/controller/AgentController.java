package warehouse.controller;

import javafx.beans.property.SimpleStringProperty;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.Tab;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.stage.Modality;
import javafx.stage.Stage;
import warehouse.dao.NotificationDao;
import warehouse.dao.RentalContractDao;
import warehouse.dao.TenantDao;
import warehouse.dao.WarehouseDao;
import warehouse.model.Agent;
import warehouse.model.ContractStatus;
import warehouse.model.Notification;
import warehouse.model.RentalContract;
import warehouse.model.Warehouse;
import warehouse.service.NotificationService;
import warehouse.service.RentalService;
import warehouse.util.UserSession;

import java.io.IOException;
import java.time.format.DateTimeFormatter;
import java.util.function.Function;

public class AgentController {

    private static final String VIEW_PATH = "/warehouse/view/";
    private static final DateTimeFormatter DATE_TIME = DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm");

    @FXML private Label welcomeLabel;

    @FXML private TableView<Warehouse> warehouseTable;
    @FXML private TableColumn<Warehouse, String> whAddressColumn;
    @FXML private TableColumn<Warehouse, String> whAreaColumn;
    @FXML private TableColumn<Warehouse, String> whHeightColumn;
    @FXML private TableColumn<Warehouse, String> whClimateColumn;
    @FXML private TableColumn<Warehouse, String> whCategoryColumn;
    @FXML private TableColumn<Warehouse, String> whPriceColumn;
    @FXML private TableColumn<Warehouse, String> whOwnerColumn;
    @FXML private Label warehouseMessage;

    @FXML private TableView<RentalContract> contractTable;
    @FXML private TableColumn<RentalContract, String> cWarehouseColumn;
    @FXML private TableColumn<RentalContract, String> cTenantColumn;
    @FXML private TableColumn<RentalContract, String> cStartColumn;
    @FXML private TableColumn<RentalContract, String> cEndColumn;
    @FXML private TableColumn<RentalContract, String> cTotalColumn;
    @FXML private TableColumn<RentalContract, String> cCommissionColumn;
    @FXML private TableColumn<RentalContract, String> cStatusColumn;
    @FXML private Label contractMessage;

    @FXML private Tab notificationsTab;
    @FXML private ListView<Notification> notificationList;

    private final RentalService rentalService =
            new RentalService(new RentalContractDao(), new TenantDao(), new WarehouseDao());
    private final NotificationService notificationService =
            new NotificationService(new NotificationDao(), new RentalContractDao());
    private Agent agent;

    @FXML
    private void initialize() {
        agent = (Agent) UserSession.getCurrentUser();
        welcomeLabel.setText("Welcome, " + agent.getFullName());

        column(whAddressColumn, Warehouse::getAddress);
        column(whAreaColumn, w -> w.getArea());
        column(whHeightColumn, w -> w.getHeight());
        column(whClimateColumn, Warehouse::getClimateCondition);
        column(whCategoryColumn, Warehouse::getCategory);
        column(whPriceColumn, w -> money(w.getPricePerMonth()));
        column(whOwnerColumn, w -> w.getOwner().getFullName());

        column(cWarehouseColumn, c -> c.getWarehouse().getAddress());
        column(cTenantColumn, c -> c.getTenant().getFullName());
        column(cStartColumn, RentalContract::getStartDate);
        column(cEndColumn, RentalContract::getEndDate);
        column(cTotalColumn, c -> money(c.getTotalPrice()));
        column(cCommissionColumn, c -> money(c.getAgentCommission()));
        column(cStatusColumn, RentalContract::getStatus);

        notificationList.setCellFactory(list -> new ListCell<>() {
            @Override
            protected void updateItem(Notification n, boolean empty) {
                super.updateItem(n, empty);
                getStyleClass().remove("unread");
                if (empty || n == null) {
                    setText(null);
                    return;
                }
                setText(n.getCreatedAt().format(DATE_TIME) + "   " + n.getMessage());
                setWrapText(true);
                if (!n.isRead()) {
                    getStyleClass().add("unread");
                }
            }
        });

        try {
            rentalService.expireFinishedContracts();
            notificationService.checkExpiringContracts();
        } catch (RuntimeException e) {
            e.printStackTrace();
        }
        refresh();
    }

    @FXML
    private void handleRefresh() {
        refresh();
    }

    @FXML
    private void handleRent() {
        warehouseMessage.setText("");
        Warehouse warehouse = warehouseTable.getSelectionModel().getSelectedItem();
        if (warehouse == null) {
            warehouseMessage.setText("Please select a warehouse first.");
            return;
        }

        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource(VIEW_PATH + "rental_form.fxml"));
            Parent root = loader.load();
            RentalFormController controller = loader.getController();
            controller.init(warehouse, agent);

            Stage dialog = new Stage();
            dialog.initModality(Modality.APPLICATION_MODAL);
            dialog.initOwner(warehouseTable.getScene().getWindow());
            dialog.setTitle("Rent warehouse");
            dialog.setScene(new Scene(root));
            dialog.setResizable(false);
            dialog.showAndWait();
        } catch (IOException e) {
            warehouseMessage.setText("Could not open the rental form.");
            e.printStackTrace();
        }
        refresh();
    }

    @FXML
    private void handleCancelContract() {
        contractMessage.setText("");
        RentalContract contract = contractTable.getSelectionModel().getSelectedItem();
        if (contract == null) {
            contractMessage.setText("Please select a contract first.");
            return;
        }
        if (contract.getStatus() != ContractStatus.ACTIVE && contract.getStatus() != ContractStatus.PENDING) {
            contractMessage.setText("Only pending requests and active contracts can be cancelled.");
            return;
        }

        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION,
                "Cancel the " + (contract.getStatus() == ContractStatus.PENDING ? "request" : "contract")
                        + " for " + contract.getWarehouse().getAddress() + "?",
                ButtonType.YES, ButtonType.NO);
        confirm.setHeaderText(null);
        if (confirm.showAndWait().orElse(ButtonType.NO) == ButtonType.YES) {
            rentalService.cancel(contract);
            refresh();
        }
    }

    @FXML
    private void handleConfirmRequest() {
        contractMessage.setText("");
        RentalContract request = contractTable.getSelectionModel().getSelectedItem();
        if (request == null) {
            contractMessage.setText("Please select a request first.");
            return;
        }
        if (request.getStatus() != ContractStatus.PENDING) {
            contractMessage.setText("Only pending requests can be confirmed.");
            return;
        }

        try {
            rentalService.confirm(request, agent);
            Alert done = new Alert(Alert.AlertType.INFORMATION,
                    "Contract confirmed. The warehouse at " + request.getWarehouse().getAddress()
                            + " is now rented and the owner has been notified.");
            done.setHeaderText(null);
            done.showAndWait();
        } catch (IllegalStateException e) {
            contractMessage.setText(e.getMessage());
        } catch (RuntimeException e) {
            contractMessage.setText("Could not confirm the request.");
            e.printStackTrace();
        }
        refresh();
    }

    @FXML
    private void handleMarkAllRead() {
        notificationService.markAllRead(agent);
        refresh();
    }

    @FXML
    private void handleLogout() {
        UserSession.logout();
        try {
            Parent root = FXMLLoader.load(getClass().getResource(VIEW_PATH + "login.fxml"));
            Stage stage = (Stage) welcomeLabel.getScene().getWindow();
            stage.setScene(new Scene(root));
            stage.setTitle("Warehouse Management System - Login");
            stage.centerOnScreen();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void refresh() {
        try {
            warehouseTable.getItems().setAll(rentalService.getFreeWarehousesOf(agent));
            contractTable.getItems().setAll(rentalService.getContractsOf(agent));
            notificationList.getItems().setAll(notificationService.getFor(agent));
            long unread = notificationService.countUnread(agent);
            notificationsTab.setText(unread > 0 ? "Notifications (" + unread + ")" : "Notifications");
        } catch (RuntimeException e) {
            warehouseMessage.setText("Could not load data from the database.");
            e.printStackTrace();
        }
    }

    static <S> void column(TableColumn<S, String> column, Function<S, Object> value) {
        column.setCellValueFactory(cell -> {
            Object v = value.apply(cell.getValue());
            return new SimpleStringProperty(v == null ? "" : v.toString());
        });
    }

    static String money(double amount) {
        return String.format("%,.2f", amount);
    }
}
