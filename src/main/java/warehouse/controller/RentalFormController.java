package warehouse.controller;

import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.GridPane;
import javafx.stage.Stage;
import warehouse.dao.RentalContractDao;
import warehouse.dao.TenantDao;
import warehouse.dao.WarehouseDao;
import warehouse.model.Agent;
import warehouse.model.RentalContract;
import warehouse.model.Tenant;
import warehouse.model.Warehouse;
import warehouse.service.RentalService;

import java.time.LocalDate;

public class RentalFormController {

    @FXML private Label warehouseLabel;
    @FXML private Label warehouseDetailsLabel;
    @FXML private TextField tenantSearchField;
    @FXML private ComboBox<Tenant> tenantCombo;
    @FXML private CheckBox newTenantCheck;
    @FXML private GridPane newTenantGrid;
    @FXML private TextField firstNameField;
    @FXML private TextField lastNameField;
    @FXML private TextField phoneField;
    @FXML private TextField emailField;
    @FXML private TextField companyField;
    @FXML private DatePicker startDatePicker;
    @FXML private DatePicker endDatePicker;
    @FXML private Label totalLabel;
    @FXML private Label totalDetailsLabel;
    @FXML private Label errorLabel;

    private final RentalService rentalService =
            new RentalService(new RentalContractDao(), new TenantDao(), new WarehouseDao());
    private Warehouse warehouse;
    private Agent agent;

    public void init(Warehouse warehouse, Agent agent) {
        this.warehouse = warehouse;
        this.agent = agent;

        warehouseLabel.setText(warehouse.getAddress());
        warehouseDetailsLabel.setText(warehouse.getArea() + " m²  ·  "
                + (warehouse.getClimateCondition() == null ? "-" : warehouse.getClimateCondition()) + "  ·  "
                + AgentController.money(warehouse.getPricePerMonth()) + " per month  ·  owner: "
                + warehouse.getOwner().getFullName());

        startDatePicker.setValue(LocalDate.now());
        endDatePicker.setValue(LocalDate.now().plusMonths(1));
        startDatePicker.valueProperty().addListener((obs, old, value) -> updateTotal());
        endDatePicker.valueProperty().addListener((obs, old, value) -> updateTotal());
        tenantSearchField.textProperty().addListener((obs, old, value) -> loadTenants(value));

        loadTenants("");
        updateTotal();
    }

    @FXML
    private void handleNewTenantToggle() {
        boolean isNew = newTenantCheck.isSelected();
        newTenantGrid.setDisable(!isNew);
        tenantCombo.setDisable(isNew);
        tenantSearchField.setDisable(isNew);
    }

    @FXML
    private void handleSave() {
        hideError();

        Tenant tenant;
        if (newTenantCheck.isSelected()) {
            if (firstNameField.getText().isBlank() || lastNameField.getText().isBlank()
                    || phoneField.getText().isBlank()) {
                showError("Please fill in the tenant's first name, last name and phone.");
                return;
            }
            tenant = new Tenant(firstNameField.getText().trim(), lastNameField.getText().trim(),
                    phoneField.getText().trim());
            tenant.setEmail(blankToNull(emailField.getText()));
            tenant.setCompanyName(blankToNull(companyField.getText()));
        } else {
            tenant = tenantCombo.getValue();
            if (tenant == null) {
                showError("Please choose a tenant or tick \"New tenant\".");
                return;
            }
        }

        try {
            RentalContract contract = rentalService.createRequest(warehouse, tenant, agent,
                    startDatePicker.getValue(), endDatePicker.getValue());

            Alert done = new Alert(Alert.AlertType.INFORMATION,
                    "Rental request created.\nTotal: " + AgentController.money(contract.getTotalPrice())
                            + "\nYour commission: " + AgentController.money(contract.getAgentCommission())
                            + "\n\nConfirm it in \"My contracts\" to rent the warehouse.");
            done.setHeaderText(null);
            done.showAndWait();
            close();
        } catch (IllegalArgumentException | IllegalStateException e) {
            showError(e.getMessage());
        } catch (RuntimeException e) {
            showError("Could not save the contract.");
            e.printStackTrace();
        }
    }

    @FXML
    private void handleCancel() {
        close();
    }

    private void loadTenants(String text) {
        try {
            tenantCombo.getItems().setAll(rentalService.searchTenants(text));
        } catch (RuntimeException e) {
            showError("Could not load tenants.");
            e.printStackTrace();
        }
    }

    private void updateTotal() {
        LocalDate start = startDatePicker.getValue();
        LocalDate end = endDatePicker.getValue();
        if (start == null || end == null || !end.isAfter(start)) {
            totalLabel.setText("-");
            totalDetailsLabel.setText("Choose a valid period");
            return;
        }
        RentalContract preview = new RentalContract(warehouse, null, agent, start, end);
        totalLabel.setText(AgentController.money(preview.getTotalPrice()));
        totalDetailsLabel.setText(preview.getMonths() + " month(s) × "
                + AgentController.money(preview.getPricePerMonth())
                + "  ·  your commission: " + AgentController.money(preview.getAgentCommission()));
    }

    private void close() {
        ((Stage) warehouseLabel.getScene().getWindow()).close();
    }

    private static String blankToNull(String text) {
        return text == null || text.isBlank() ? null : text.trim();
    }

    private void showError(String message) {
        errorLabel.setText(message);
        errorLabel.setVisible(true);
        errorLabel.setManaged(true);
    }

    private void hideError() {
        errorLabel.setVisible(false);
        errorLabel.setManaged(false);
    }
}
