import javafx.application.Application;
import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

public class GUIClientT1 extends Application {
   @Override
   public void start(Stage primaryStage) throws Exception {
      FXMLLoader loader = new FXMLLoader(getClass().getResource("/clientView.fxml"));
      Parent root = loader.load();
      ClientController controller = loader.getController();

      // connexion serveur
      controller.setupNetwork("localhost", 12345);

      // refresh toutes les 0.5s
      Thread refreshThread = new Thread(() -> {
         while (true) {
            Platform.runLater(controller::handleRefresh);
            try { Thread.sleep(500); } catch (InterruptedException e) { break; }
         }
      });
      refreshThread.setDaemon(true);
      refreshThread.start();

      primaryStage.setTitle("collaborative editor");
      primaryStage.setScene(new Scene(root));
      primaryStage.show();
   }

   public static void main(String[] args) { launch(args); }
}