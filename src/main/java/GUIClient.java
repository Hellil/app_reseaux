import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

public class GUIClient extends Application {
   @Override
   public void start(Stage primaryStage) throws Exception {
      FXMLLoader loader = new FXMLLoader(getClass().getResource("/clientView.fxml"));
      Parent root = loader.load();
      ClientController controller = loader.getController();

      // connexion serveur
      controller.setupNetwork("localhost", 12347); // TODO: changer ici pour changer port

      // no more refresh
      primaryStage.setTitle("collaborative editor");
      primaryStage.setScene(new Scene(root));
      primaryStage.show();
   }

   public static void main(String[] args) { launch(args); }
}