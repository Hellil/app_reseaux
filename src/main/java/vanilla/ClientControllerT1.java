import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;

import javafx.fxml.FXML;
import javafx.scene.control.MenuItem;
import javafx.scene.control.TextField;

public class ClientControllerT1 {
    @FXML private javafx.scene.control.ListView<String> listView;
    @FXML private TextField textField;
    @FXML private MenuItem deleteLineMenuItem;
    private PrintWriter out;
    private BufferedReader in;

    public void setupNetwork(String host, int port) throws IOException {
        Socket socket = new Socket(host, port);
        this.out = new PrintWriter(socket.getOutputStream(), true);
        this.in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
    }

    @FXML
    public void initialize() {
        handleRefresh(); // get last version of the document

        // delete line when line selected
        listView.getSelectionModel().selectedItemProperty().addListener((obs, oldValue, newValue) -> {
            deleteLineMenuItem.setDisable(newValue == null);
            if (newValue != null && !textField.isFocused()) {
                textField.setText(newValue);
            }
        });
    }

    // refresh toutes les 0.5s
    @FXML
    public void handleRefresh() {
        if (out == null) return;
        try {
            out.println("GETD");
            List<String> newLines = new ArrayList<>();
            String response;
            while (!(response = in.readLine()).equals("DONE")) {
                if (response.startsWith("LINE ")) {
                    newLines.add(response.split(" ", 3)[2]);
                }
            }
            int selectedIndex = listView.getSelectionModel().getSelectedIndex();
            listView.getItems().setAll(newLines);
            if (selectedIndex >= 0 && selectedIndex < newLines.size()) {
                listView.getSelectionModel().select(selectedIndex);
            }
        } catch (IOException e) { e.printStackTrace(); }
    }

    // methodes clic droit
    @FXML
    public void handleTextFieldUpdate() {
        int index = listView.getSelectionModel().getSelectedIndex();
        String text = textField.getText();
        out.println("MDFL " + (index + 1) + " " + text);
    }

    @FXML
    public void handleAddLine() {
        int index = listView.getSelectionModel().getSelectedIndex();
        String text = "Nouvelle ligne";
        out.println("ADDL " + (index + 1) + " " + text);
    }

    @FXML
    public void handleDeleteLine() {
        int index = listView.getSelectionModel().getSelectedIndex();
        out.println("RMVL " + (index + 1));
    }
}