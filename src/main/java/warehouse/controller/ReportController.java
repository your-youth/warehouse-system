package warehouse.controller;

import javafx.fxml.FXML;
import javafx.scene.control.ComboBox;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import warehouse.model.RentalContract;
import warehouse.service.ReportService;
import warehouse.service.ReportService.AgentReport;
import warehouse.service.ReportService.Summary;
import warehouse.service.ReportService.WarehouseReport;

import java.time.LocalDate;
import java.time.temporal.TemporalAdjusters;

import static warehouse.controller.AgentController.column;
import static warehouse.controller.AgentController.money;

public class ReportController {

    private static final String THIS_MONTH = "This month";
    private static final String LAST_MONTH = "Last month";
    private static final String THIS_QUARTER = "This quarter";
    private static final String THIS_YEAR = "This year";
    private static final String CUSTOM = "Custom";

    @FXML private ComboBox<String> periodCombo;
    @FXML private DatePicker fromPicker;
    @FXML private DatePicker toPicker;
    @FXML private Label errorLabel;

    @FXML private Label contractsLabel;
    @FXML private Label revenueLabel;
    @FXML private Label commissionLabel;
    @FXML private Label occupancyLabel;
    @FXML private Label occupancyDetailsLabel;

    @FXML private TableView<AgentReport> agentTable;
    @FXML private TableColumn<AgentReport, String> aNameColumn;
    @FXML private TableColumn<AgentReport, String> aContractsColumn;
    @FXML private TableColumn<AgentReport, String> aRevenueColumn;
    @FXML private TableColumn<AgentReport, String> aCommissionColumn;

    @FXML private TableView<WarehouseReport> warehouseTable;
    @FXML private TableColumn<WarehouseReport, String> wAddressColumn;
    @FXML private TableColumn<WarehouseReport, String> wOwnerColumn;
    @FXML private TableColumn<WarehouseReport, String> wContractsColumn;
    @FXML private TableColumn<WarehouseReport, String> wRevenueColumn;

    @FXML private TableView<RentalContract> contractTable;
    @FXML private TableColumn<RentalContract, String> cWarehouseColumn;
    @FXML private TableColumn<RentalContract, String> cTenantColumn;
    @FXML private TableColumn<RentalContract, String> cAgentColumn;
    @FXML private TableColumn<RentalContract, String> cStartColumn;
    @FXML private TableColumn<RentalContract, String> cEndColumn;
    @FXML private TableColumn<RentalContract, String> cTotalColumn;
    @FXML private TableColumn<RentalContract, String> cStatusColumn;

    private final ReportService reportService = new ReportService();

    @FXML
    private void initialize() {
        column(aNameColumn, AgentReport::agentName);
        column(aContractsColumn, AgentReport::contracts);
        column(aRevenueColumn, r -> money(r.revenue()));
        column(aCommissionColumn, r -> money(r.commission()));

        column(wAddressColumn, WarehouseReport::address);
        column(wOwnerColumn, WarehouseReport::owner);
        column(wContractsColumn, WarehouseReport::contracts);
        column(wRevenueColumn, r -> money(r.revenue()));

        column(cWarehouseColumn, c -> c.getWarehouse().getAddress());
        column(cTenantColumn, c -> c.getTenant().getFullName());
        column(cAgentColumn, c -> c.getAgent().getFullName());
        column(cStartColumn, RentalContract::getStartDate);
        column(cEndColumn, RentalContract::getEndDate);
        column(cTotalColumn, c -> money(c.getTotalPrice()));
        column(cStatusColumn, RentalContract::getStatus);

        periodCombo.getItems().setAll(THIS_MONTH, LAST_MONTH, THIS_QUARTER, THIS_YEAR, CUSTOM);
        periodCombo.valueProperty().addListener((obs, old, value) -> applyPeriod(value));
        periodCombo.setValue(THIS_MONTH);

        handleShow();
    }

    @FXML
    private void handleShow() {
        errorLabel.setText("");
        LocalDate from = fromPicker.getValue();
        LocalDate to = toPicker.getValue();
        if (from == null || to == null || to.isBefore(from)) {
            errorLabel.setText("Please choose a valid period.");
            return;
        }

        try {
            Summary summary = reportService.getSummary(from, to);
            contractsLabel.setText(String.valueOf(summary.contracts()));
            revenueLabel.setText(money(summary.revenue()));
            commissionLabel.setText(money(summary.commission()));
            occupancyLabel.setText(String.format("%.0f%%", summary.occupancyPercent()));
            occupancyDetailsLabel.setText("Rented now: " + summary.rentedWarehouses()
                    + " of " + summary.totalWarehouses());

            agentTable.getItems().setAll(reportService.getAgentReport(from, to));
            warehouseTable.getItems().setAll(reportService.getWarehouseReport(from, to));
            contractTable.getItems().setAll(reportService.getContracts(from, to));
        } catch (RuntimeException e) {
            errorLabel.setText("Could not load the report.");
            e.printStackTrace();
        }
    }

    private void applyPeriod(String period) {
        LocalDate today = LocalDate.now();
        boolean custom = CUSTOM.equals(period);
        fromPicker.setDisable(!custom);
        toPicker.setDisable(!custom);

        switch (period) {
            case THIS_MONTH -> setRange(today.withDayOfMonth(1), today.with(TemporalAdjusters.lastDayOfMonth()));
            case LAST_MONTH -> {
                LocalDate last = today.minusMonths(1);
                setRange(last.withDayOfMonth(1), last.with(TemporalAdjusters.lastDayOfMonth()));
            }
            case THIS_QUARTER -> {
                int firstMonth = (today.getMonthValue() - 1) / 3 * 3 + 1;
                LocalDate start = LocalDate.of(today.getYear(), firstMonth, 1);
                setRange(start, start.plusMonths(3).minusDays(1));
            }
            case THIS_YEAR -> setRange(today.withDayOfYear(1), today.with(TemporalAdjusters.lastDayOfYear()));
            default -> {
            }
        }
    }

    private void setRange(LocalDate from, LocalDate to) {
        fromPicker.setValue(from);
        toPicker.setValue(to);
    }
}
