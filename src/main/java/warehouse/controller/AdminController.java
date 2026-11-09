package warehouse.controller;

import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.stage.Stage;
import warehouse.dao.AgentDao;
import warehouse.dao.OwnerDao;
import warehouse.dao.UserDao;
import warehouse.model.Agent;
import warehouse.model.Owner;
import warehouse.service.UserService;

public class AdminController {

    @FXML private TableView<Owner> ownersTable;
    @FXML private TableColumn<Owner, String> ownerUsernameCol;
    @FXML private TableColumn<Owner, String> ownerNameCol;
    @FXML private TableColumn<Owner, String> ownerPhoneCol;
    @FXML private TableColumn<Owner, String> ownerEmailCol;

    @FXML private TextField ownerUsernameField;
    @FXML private PasswordField ownerPasswordField;
    @FXML private TextField ownerFirstNameField;
    @FXML private TextField ownerLastNameField;
    @FXML private TextField ownerPhoneField;
    @FXML private TextField ownerEmailField;
    @FXML private Label ownerMessageLabel;

    @FXML private TableView<Agent> agentsTable;
    @FXML private TableColumn<Agent, String> agentUsernameCol;
    @FXML private TableColumn<Agent, String> agentNameCol;
    @FXML private TableColumn<Agent, String> agentPhoneCol;
    @FXML private TableColumn<Agent, String> agentEmailCol;
    @FXML private TableColumn<Agent, Double> agentFeeCol;

    @FXML private TextField agentUsernameField;
    @FXML private PasswordField agentPasswordField;
    @FXML private TextField agentFirstNameField;
    @FXML private TextField agentLastNameField;
    @FXML private TextField agentPhoneField;
    @FXML private TextField agentEmailField;
    @FXML private TextField agentFeeField;
    @FXML private Label agentMessageLabel;

    private final UserService userService =
            new UserService(new UserDao(), new OwnerDao(), new AgentDao());


    @FXML
    private void initialize() {
        ownerUsernameCol.setCellValueFactory(new PropertyValueFactory<>("username"));
        ownerNameCol.setCellValueFactory(new PropertyValueFactory<>("fullName"));
        ownerPhoneCol.setCellValueFactory(new PropertyValueFactory<>("phone"));
        ownerEmailCol.setCellValueFactory(new PropertyValueFactory<>("email"));

        agentUsernameCol.setCellValueFactory(new PropertyValueFactory<>("username"));
        agentNameCol.setCellValueFactory(new PropertyValueFactory<>("fullName"));
        agentPhoneCol.setCellValueFactory(new PropertyValueFactory<>("phone"));
        agentEmailCol.setCellValueFactory(new PropertyValueFactory<>("email"));
        agentFeeCol.setCellValueFactory(new PropertyValueFactory<>("fee"));

        refreshTables();
    }

    @FXML
    private void onCreateOwner() {
        try {
            userService.createOwner(
                    ownerUsernameField.getText(), ownerPasswordField.getText(),
                    ownerFirstNameField.getText(), ownerLastNameField.getText(),
                    ownerPhoneField.getText(), ownerEmailField.getText());
            showSuccess(ownerMessageLabel, "Owner created");
            clear(ownerUsernameField, ownerPasswordField, ownerFirstNameField,
                    ownerLastNameField, ownerPhoneField, ownerEmailField);
            refreshTables();
        } catch (IllegalArgumentException e) {
            showError(ownerMessageLabel, e.getMessage());
        }
    }

    @FXML
    private void onCreateAgent() {
        try {
            double fee = parseFee(agentFeeField.getText());
            userService.createAgent(
                    agentUsernameField.getText(), agentPasswordField.getText(),
                    agentFirstNameField.getText(), agentLastNameField.getText(),
                    agentPhoneField.getText(), agentEmailField.getText(), fee);
            showSuccess(agentMessageLabel, "Agent created");
            clear(agentUsernameField, agentPasswordField, agentFirstNameField,
                    agentLastNameField, agentPhoneField, agentEmailField, agentFeeField);
            refreshTables();
        } catch (IllegalArgumentException e) {
            showError(agentMessageLabel, e.getMessage());
        }
    }

    @FXML
    private void onLogout() {
        try {
            Parent root = FXMLLoader.load(getClass().getResource("/warehouse/view/login.fxml"));
            Stage stage = (Stage) ownersTable.getScene().getWindow();
            stage.setScene(new Scene(root));
            stage.setTitle("Warehouse Management System - Login");
        } catch (Exception e) {
            showError(ownerMessageLabel, "Cannot open login screen: " + e.getMessage());
        }
    }


    private void refreshTables() {
        ownersTable.setItems(FXCollections.observableArrayList(userService.getAllOwners()));
        agentsTable.setItems(FXCollections.observableArrayList(userService.getAllAgents()));
    }

    private double parseFee(String text) {
        if (text == null || text.isBlank()) {
            return 0;
        }
        try {
            return Double.parseDouble(text.trim().replace(',', '.'));
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Fee must be a number");
        }
    }

    private void showSuccess(Label label, String text) {
        label.setStyle("-fx-text-fill: #2e7d32;");
        label.setText(text);
    }

    private void showError(Label label, String text) {
        label.setStyle("-fx-text-fill: #c62828;");
        label.setText(text);
    }

    private void clear(TextInputControl... fields) {
        for (TextInputControl f : fields) {
            f.clear();
        }
    }
}