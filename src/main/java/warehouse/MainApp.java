package warehouse;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import warehouse.util.DataInitializer;

public class MainApp extends Application {

    @Override
    public void start(Stage stage) throws Exception {
        DataInitializer.ensureAdminExists();
        Parent root = FXMLLoader.load(getClass().getResource("/warehouse/view/login.fxml"));
        stage.setTitle("Warehouse Management System - Login");
        stage.setScene(new Scene(root));
        stage.setResizable(false);
        stage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
