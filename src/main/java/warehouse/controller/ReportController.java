package warehouse.controller;

import javafx.fxml.FXML;
import javafx.scene.control.ComboBox;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.util.StringConverter;
import warehouse.dao.OwnerDao;
import warehouse.dao.RatingDao;
import warehouse.dao.RentalContractDao;
import warehouse.dao.WarehouseDao;
import warehouse.model.Agent;
import warehouse.model.Owner;
import warehouse.model.RentalContract;
import warehouse.model.User;
import warehouse.model.Warehouse;
import warehouse.service.RatingService;
import warehouse.service.ReportService;
import warehouse.service.ReportService.AgentReport;
import warehouse.service.ReportService.Summary;
import warehouse.service.ReportService.WarehouseReport;
import warehouse.util.UserSession;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.temporal.TemporalAdjusters;
import java.util.List;
import java.util.stream.Collectors;

import static warehouse.controller.AgentController.column;
import static warehouse.controller.AgentController.money;

public class ReportController {

    private static final String THIS_MONTH = "This month";
    private static final String LAST_MONTH = "Last month";
    private static final String THIS_QUARTER = "This quarter";
    private static final String THIS_YEAR = "This year";
    private static final String CUSTOM = "Custom";
    private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("dd.MM.yyyy");

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
    @FXML private TableColumn<AgentReport, String> aPhoneColumn;
    @FXML private TableColumn<AgentReport, String> aEmailColumn;
    @FXML private TableColumn<AgentReport, String> aFeeColumn;
    @FXML private TableColumn<AgentReport, String> aRatingColumn;
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

    @FXML private TableView<Warehouse> availableTable;
    @FXML private TableColumn<Warehouse, String> avDateColumn;
    @FXML private TableColumn<Warehouse, String> avStatusColumn;
    @FXML private TableColumn<Warehouse, String> avAddressColumn;
    @FXML private TableColumn<Warehouse, String> avTypeColumn;
    @FXML private TableColumn<Warehouse, String> avAreaColumn;
    @FXML private TableColumn<Warehouse, String> avPriceColumn;
    @FXML private TableColumn<Warehouse, String> avOwnerColumn;

    @FXML private ComboBox<Owner> ownerCombo;
    @FXML private TableView<Warehouse> ownerWarehouseTable;
    @FXML private TableColumn<Warehouse, String> owAddressColumn;
    @FXML private TableColumn<Warehouse, String> owTypeColumn;
    @FXML private TableColumn<Warehouse, String> owStatusColumn;
    @FXML private TableColumn<Warehouse, String> owAreaColumn;
    @FXML private TableColumn<Warehouse, String> owPriceColumn;
    @FXML private TableColumn<Warehouse, String> owAgentsColumn;

    private final ReportService reportService = new ReportService(
            new RentalContractDao(), new WarehouseDao(), new OwnerDao(), new RatingService(new RatingDao()));

    @FXML
    private void initialize() {
        column(aNameColumn, AgentReport::agentName);
        column(aPhoneColumn, AgentReport::phone);
        column(aEmailColumn, AgentReport::email);
        column(aFeeColumn, AgentReport::fee);
        column(aRatingColumn, r -> r.rating() == 0 ? "-" : String.format("%.1f", r.rating()));
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

        column(avDateColumn, w -> LocalDate.now().format(DATE));
        column(avStatusColumn, Warehouse::getStatus);
        column(avAddressColumn, Warehouse::getAddress);
        column(avTypeColumn, ReportController::typeOf);
        column(avAreaColumn, Warehouse::getArea);
        column(avPriceColumn, w -> money(w.getPricePerMonth()));
        column(avOwnerColumn, w -> w.getOwner().getFullName());

        column(owAddressColumn, Warehouse::getAddress);
        column(owTypeColumn, ReportController::typeOf);
        column(owStatusColumn, Warehouse::getStatus);
        column(owAreaColumn, Warehouse::getArea);
        column(owPriceColumn, w -> money(w.getPricePerMonth()));
        column(owAgentsColumn, w -> w.getAgents().stream().map(Agent::getFullName).collect(Collectors.joining(", ")));

        ownerCombo.setConverter(new StringConverter<>() {
            @Override public String toString(Owner o) { return o == null ? "" : o.getFullName(); }
            @Override public Owner fromString(String s) { return null; }
        });
        ownerCombo.valueProperty().addListener((obs, old, owner) -> showOwnerWarehouses(owner));

        periodCombo.getItems().setAll(THIS_MONTH, LAST_MONTH, THIS_QUARTER, THIS_YEAR, CUSTOM);
        periodCombo.valueProperty().addListener((obs, old, value) -> applyPeriod(value));
        periodCombo.setValue(THIS_MONTH);

        handleShow();
    }

    private void showOwnerWarehouses(Owner owner) {
        try {
            ownerWarehouseTable.getItems().setAll(owner == null ? List.of() : reportService.getWarehousesOf(owner));
        } catch (RuntimeException e) {
            errorLabel.setText("Could not load the owner's warehouses.");
            e.printStackTrace();
        }
    }

    private static String typeOf(Warehouse w) {
        String category = w.getCategory() == null ? "-" : "Category " + w.getCategory();
        String climate = w.getClimateCondition() == null ? "" : ", " + w.getClimateCondition();
        String goods = w.getGoodsType() == null || w.getGoodsType().isBlank() ? "" : ", " + w.getGoodsType();
        return category + climate + goods;
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

        User viewer = UserSession.getCurrentUser();
        try {
            Summary summary = reportService.getSummary(from, to, viewer);
            contractsLabel.setText(String.valueOf(summary.contracts()));
            revenueLabel.setText(money(summary.revenue()));
            commissionLabel.setText(money(summary.commission()));
            occupancyLabel.setText(String.format("%.0f%%", summary.occupancyPercent()));
            occupancyDetailsLabel.setText("Rented now: " + summary.rentedWarehouses()
                    + " of " + summary.totalWarehouses());

            agentTable.getItems().setAll(reportService.getAgentReport(from, to, viewer));
            warehouseTable.getItems().setAll(reportService.getWarehouseReport(from, to, viewer));
            contractTable.getItems().setAll(reportService.getContracts(from, to, viewer));
            availableTable.getItems().setAll(reportService.getAvailableWarehouses(viewer));

            Owner selected = ownerCombo.getValue();
            ownerCombo.getItems().setAll(reportService.getOwnersFor(viewer));
            ownerCombo.setDisable(viewer instanceof Owner);
            ownerCombo.setValue(viewer instanceof Owner ? ownerCombo.getItems().get(0)
                    : ownerCombo.getItems().stream().filter(o -> selected != null && o.getId().equals(selected.getId()))
                            .findFirst().orElse(null));
            showOwnerWarehouses(ownerCombo.getValue());
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
