package warehouse.controller;

import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.stage.Stage;
import javafx.util.StringConverter;
import warehouse.dao.AgentDao;
import warehouse.dao.NotificationDao;
import warehouse.dao.RatingDao;
import warehouse.dao.RentalContractDao;
import warehouse.dao.WarehouseDao;
import warehouse.model.*;
import warehouse.service.NotificationService;
import warehouse.service.RatingService;
import warehouse.service.WarehouseService;
import warehouse.util.UserSession;

import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Collectors;

public class OwnerController {

    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm");

    @FXML private Label welcomeLabel;

    @FXML private TableView<Warehouse> warehousesTable;
    @FXML private TableColumn<Warehouse, String> addressCol;
    @FXML private TableColumn<Warehouse, Double> areaCol;
    @FXML private TableColumn<Warehouse, ClimateCondition> climateCol;
    @FXML private TableColumn<Warehouse, WarehouseCategory> categoryCol;
    @FXML private TableColumn<Warehouse, WarehouseStatus> statusCol;
    @FXML private TableColumn<Warehouse, Double> priceCol;
    @FXML private TableColumn<Warehouse, String> agentsCol;
    @FXML private ComboBox<Agent> agentCombo;

    @FXML private TextField addressField;
    @FXML private TextField areaField;
    @FXML private TextField heightField;
    @FXML private ComboBox<ClimateCondition> climateCombo;
    @FXML private TextField goodsTypeField;
    @FXML private ComboBox<WarehouseCategory> categoryCombo;
    @FXML private TextField priceField;
    @FXML private Label warehouseMessageLabel;

    @FXML private TableView<Agent> agentsTable;
    @FXML private TableColumn<Agent, String> agentNameCol;
    @FXML private TableColumn<Agent, Double> agentFeeCol;
    @FXML private TableColumn<Agent, String> agentRatingCol;
    @FXML private ComboBox<Agent> rateAgentCombo;
    @FXML private Spinner<Integer> scoreSpinner;
    @FXML private TextArea commentArea;
    @FXML private Label ratingMessageLabel;

    @FXML private Tab notificationsTab;
    @FXML private TableView<Notification> notificationsTable;
    @FXML private TableColumn<Notification, String> notifStatusCol;
    @FXML private TableColumn<Notification, String> notifDateCol;
    @FXML private TableColumn<Notification, String> notifTypeCol;
    @FXML private TableColumn<Notification, String> notifMessageCol;

    private final WarehouseService warehouseService = new WarehouseService(new WarehouseDao());
    private final RatingService ratingService = new RatingService(new RatingDao());
    private final NotificationService notificationService =
            new NotificationService(new NotificationDao(), new RentalContractDao());
    private final AgentDao agentDao = new AgentDao();

    private Owner owner;

    @FXML
    private void initialize() {
        owner = (Owner) UserSession.getCurrentUser();
        welcomeLabel.setText("Welcome, " + owner.getFullName());

        addressCol.setCellValueFactory(new PropertyValueFactory<>("address"));
        areaCol.setCellValueFactory(new PropertyValueFactory<>("area"));
        climateCol.setCellValueFactory(new PropertyValueFactory<>("climateCondition"));
        categoryCol.setCellValueFactory(new PropertyValueFactory<>("category"));
        statusCol.setCellValueFactory(new PropertyValueFactory<>("status"));
        priceCol.setCellValueFactory(new PropertyValueFactory<>("pricePerMonth"));
        agentsCol.setCellValueFactory(cell -> new SimpleStringProperty(
                cell.getValue().getAgents().stream()
                        .map(Agent::getFullName)
                        .collect(Collectors.joining(", "))));

        agentNameCol.setCellValueFactory(new PropertyValueFactory<>("fullName"));
        agentFeeCol.setCellValueFactory(new PropertyValueFactory<>("fee"));
        agentRatingCol.setCellValueFactory(cell -> new SimpleStringProperty(
                String.format("%.1f", ratingService.getAverageRating(cell.getValue()))));

        notifStatusCol.setCellValueFactory(cell -> new SimpleStringProperty(
                cell.getValue().isRead() ? "" : "NEW"));
        notifDateCol.setCellValueFactory(cell -> new SimpleStringProperty(
                cell.getValue().getCreatedAt().format(DATE_FORMAT)));
        notifTypeCol.setCellValueFactory(cell -> new SimpleStringProperty(
                typeName(cell.getValue().getType())));
        notifMessageCol.setCellValueFactory(new PropertyValueFactory<>("message"));
        notificationsTable.setRowFactory(table -> new TableRow<>() {
            @Override
            protected void updateItem(Notification n, boolean empty) {
                super.updateItem(n, empty);
                setStyle(n != null && !n.isRead() ? "-fx-font-weight: bold;" : "");
            }
        });

        climateCombo.setItems(FXCollections.observableArrayList(ClimateCondition.values()));
        categoryCombo.setItems(FXCollections.observableArrayList(WarehouseCategory.values()));
        agentCombo.setConverter(agentNames());
        rateAgentCombo.setConverter(agentNames());

        warehousesTable.getSelectionModel().selectedItemProperty()
                .addListener((obs, old, selected) -> fillForm(selected));

        notificationService.checkExpiringContracts();

        refresh();
        refreshNotifications();
    }

    @FXML
    private void onAddWarehouse() {
        try {
            warehouseService.addWarehouse(owner,
                    addressField.getText(),
                    parseNumber(areaField.getText(), "Area"),
                    parseNumber(heightField.getText(), "Height"),
                    climateCombo.getValue(),
                    goodsTypeField.getText(),
                    categoryCombo.getValue(),
                    parseNumber(priceField.getText(), "Price"));
            showSuccess(warehouseMessageLabel, "Warehouse added");
            onClearForm();
            refresh();
        } catch (IllegalArgumentException e) {
            showError(warehouseMessageLabel, e.getMessage());
        }
    }

    @FXML
    private void onUpdateWarehouse() {
        Warehouse w = warehousesTable.getSelectionModel().getSelectedItem();
        if (w == null) {
            showError(warehouseMessageLabel, "Select a warehouse in the table");
            return;
        }
        try {
            w.setAddress(addressField.getText());
            w.setArea(parseNumber(areaField.getText(), "Area"));
            w.setHeight(parseNumber(heightField.getText(), "Height"));
            w.setClimateCondition(climateCombo.getValue());
            w.setGoodsType(goodsTypeField.getText());
            w.setCategory(categoryCombo.getValue());
            w.setPricePerMonth(parseNumber(priceField.getText(), "Price"));
            warehouseService.updateWarehouse(w);
            showSuccess(warehouseMessageLabel, "Changes saved");
            refresh();
        } catch (IllegalArgumentException e) {
            showError(warehouseMessageLabel, e.getMessage());
        }
    }

    @FXML
    private void onClearForm() {
        warehousesTable.getSelectionModel().clearSelection();
        addressField.clear();
        areaField.clear();
        heightField.clear();
        goodsTypeField.clear();
        priceField.clear();
        climateCombo.setValue(null);
        categoryCombo.setValue(null);
    }

    @FXML
    private void onAssignAgent() {
        Warehouse w = warehousesTable.getSelectionModel().getSelectedItem();
        Agent a = agentCombo.getValue();
        if (w == null || a == null) {
            showError(warehouseMessageLabel, "Select a warehouse and an agent");
            return;
        }
        warehouseService.assignAgent(w, a);
        showSuccess(warehouseMessageLabel, a.getFullName() + " assigned");
        refresh();
    }

    @FXML
    private void onRemoveAgent() {
        Warehouse w = warehousesTable.getSelectionModel().getSelectedItem();
        Agent a = agentCombo.getValue();
        if (w == null || a == null) {
            showError(warehouseMessageLabel, "Select a warehouse and an agent");
            return;
        }
        warehouseService.removeAgent(w, a);
        showSuccess(warehouseMessageLabel, a.getFullName() + " removed");
        refresh();
    }

    @FXML
    private void onRateAgent() {
        try {
            ratingService.rateAgent(owner, rateAgentCombo.getValue(),
                    scoreSpinner.getValue(), commentArea.getText());
            showSuccess(ratingMessageLabel, "Thank you, rating saved");
            commentArea.clear();
            refresh();
        } catch (IllegalArgumentException e) {
            showError(ratingMessageLabel, e.getMessage());
        }
    }

    @FXML
    private void onRefreshNotifications() {
        notificationService.checkExpiringContracts();
        refreshNotifications();
    }

    @FXML
    private void onMarkAllRead() {
        notificationService.markAllRead(owner);
        refreshNotifications();
    }

    @FXML
    private void onLogout() {
        try {
            UserSession.logout();
            Parent root = FXMLLoader.load(getClass().getResource("/warehouse/view/login.fxml"));
            Stage stage = (Stage) warehousesTable.getScene().getWindow();
            stage.setScene(new Scene(root));
            stage.setTitle("Warehouse Management System - Login");
        } catch (Exception e) {
            showError(warehouseMessageLabel, "Cannot open login screen: " + e.getMessage());
        }
    }

    private void refresh() {
        warehousesTable.setItems(FXCollections.observableArrayList(
                warehouseService.getWarehousesOf(owner)));
        List<Agent> agents = agentDao.findAll();
        agentCombo.setItems(FXCollections.observableArrayList(agents));
        rateAgentCombo.setItems(FXCollections.observableArrayList(agents));
        agentsTable.setItems(FXCollections.observableArrayList(agents));
    }

    private void refreshNotifications() {
        notificationsTable.setItems(FXCollections.observableArrayList(
                notificationService.getFor(owner)));
        long unread = notificationService.countUnread(owner);
        notificationsTab.setText(unread > 0 ? "Notifications (" + unread + ")" : "Notifications");
    }

    private String typeName(NotificationType type) {
        if (type == null) {
            return "";
        }
        return switch (type) {
            case NEW_RENTAL_REQUEST -> "New rental request";
            case WAREHOUSE_RENTED -> "Warehouse rented";
            case CONTRACT_EXPIRING -> "Contract expiring";
        };
    }

    private void fillForm(Warehouse w) {
        if (w == null) {
            return;
        }
        addressField.setText(w.getAddress());
        areaField.setText(String.valueOf(w.getArea()));
        heightField.setText(String.valueOf(w.getHeight()));
        climateCombo.setValue(w.getClimateCondition());
        goodsTypeField.setText(w.getGoodsType());
        categoryCombo.setValue(w.getCategory());
        priceField.setText(String.valueOf(w.getPricePerMonth()));
    }

    private double parseNumber(String text, String field) {
        if (text == null || text.isBlank()) {
            return 0;
        }
        try {
            return Double.parseDouble(text.trim().replace(',', '.'));
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException(field + " must be a number");
        }
    }

    private StringConverter<Agent> agentNames() {
        return new StringConverter<>() {
            @Override public String toString(Agent a) { return a == null ? "" : a.getFullName(); }
            @Override public Agent fromString(String s) { return null; }
        };
    }

    private void showSuccess(Label label, String text) {
        label.setStyle("-fx-text-fill: #2e7d32;");
        label.setText(text);
    }

    private void showError(Label label, String text) {
        label.setStyle("-fx-text-fill: #c62828;");
        label.setText(text);
    }
}