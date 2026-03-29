package app;

import javafx.application.Application;
import javafx.stage.Stage;

public class MainApp extends Application {

    @Override
    public void start(Stage primaryStage) {
        SceneNavigator.setPrimaryStage(primaryStage);
        primaryStage.setResizable(true);
        primaryStage.setMinWidth(480);
        primaryStage.setMinHeight(400);
        primaryStage.setTitle("Supermarket Automation System");
        SceneNavigator.navigateTo("login.fxml", "Supermarket Automation System – Login");
    }

    public static void main(String[] args) {
        launch(args);
    }
}
