package app;

import app.controllers.ReceiptController;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Modality;
import javafx.stage.Stage;

import java.io.IOException;

public class SceneNavigator {

    private static Stage primaryStage;

    public static void setPrimaryStage(Stage stage) {
        primaryStage = stage;
    }

    public static Stage getPrimaryStage() {
        return primaryStage;
    }

    /**
     * Replaces the current scene on the primary stage.
     */
    public static void navigateTo(String fxmlFile, String title) {
        try {
            FXMLLoader loader = new FXMLLoader(
                SceneNavigator.class.getResource("/app/views/" + fxmlFile)
            );
            Parent root  = loader.load();
            Scene  scene = new Scene(root);
            String css   = SceneNavigator.class
                    .getResource("/app/views/styles.css").toExternalForm();
            scene.getStylesheets().add(css);
            primaryStage.setTitle(title);
            primaryStage.setScene(scene);
            primaryStage.centerOnScreen();
            primaryStage.show();
        } catch (IOException e) {
            e.printStackTrace();
            AlertHelper.showError("Navigation Error", "Unable to load: " + fxmlFile);
        }
    }

    /**
     * Opens the receipt view in a new modal window for the given transaction ID.
     */
    public static void openReceiptWindow(String transactionId) {
        try {
            FXMLLoader loader = new FXMLLoader(
                SceneNavigator.class.getResource("/app/views/receipt_view.fxml")
            );
            Parent root  = loader.load();

            ReceiptController ctrl = loader.getController();
            ctrl.loadReceipt(transactionId);

            Scene scene = new Scene(root);
            String css  = SceneNavigator.class
                    .getResource("/app/views/styles.css").toExternalForm();
            scene.getStylesheets().add(css);

            Stage receiptStage = new Stage();
            receiptStage.setTitle("Receipt – " + transactionId);
            receiptStage.setScene(scene);
            receiptStage.initOwner(primaryStage);
            receiptStage.initModality(Modality.WINDOW_MODAL);
            receiptStage.setResizable(true);
            receiptStage.setMinWidth(680);
            receiptStage.setMinHeight(600);
            receiptStage.centerOnScreen();
            receiptStage.show();

        } catch (IOException e) {
            e.printStackTrace();
            AlertHelper.showError("Receipt Error", "Unable to open receipt window.");
        }
    }
}
