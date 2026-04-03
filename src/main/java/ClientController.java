import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.MenuItem;
import javafx.scene.control.TextField;

public class ClientController {
    @FXML private javafx.scene.control.ListView<String> listView;
    @FXML private TextField textField;
    @FXML private MenuItem deleteLineMenuItem;

    private PrintWriter out;
    private BufferedReader in;

    public void setupNetwork(String host, int port) throws IOException {
        Socket socket = new Socket(host, port);
        this.out = new PrintWriter(socket.getOutputStream(), true);
        this.in = new BufferedReader(new InputStreamReader(socket.getInputStream()));

        new Thread(() -> {
            try {
                String line;
                while ((line = in.readLine()) != null) {
                    String finalLine = line;
                    Platform.runLater(() -> handleServerMessage(finalLine));
                }
            } catch (IOException e) { e.printStackTrace(); }
        }).start();
    }

    private void handleServerMessage(String msg) {
        if (msg.startsWith("FWD ")) {
            msg = msg.substring(4);
        }

        if (msg.startsWith("LINE ")) {
            String[] parts = msg.split(" ", 3);
            int index = Integer.parseInt(parts[1]) - 1;
            String text = parts[2];

            if (index >= 0 && index <= listView.getItems().size()) {
                listView.getItems().add(index, text);
            } else {
                listView.getItems().add(text);
            }
        } else if (msg.startsWith("MDFL ")) {
            String[] parts = msg.split(" ", 3);
            int index = Integer.parseInt(parts[1]) - 1;
            String text = parts[2];

            if (index < listView.getItems().size()) {
                listView.getItems().set(index, text);
            } else {
                listView.getItems().add(text);
            }

        } else if (msg.startsWith("DELL ")) {
            int index = Integer.parseInt(msg.split(" ")[1]) - 1;
            if (index >= 0 && index < listView.getItems().size()) {
                listView.getItems().remove(index);
            }

        } else if (msg.equals("DONE")) {
            // fin init
        }
    }

    @FXML
    public void initialize() {
        listView.getSelectionModel().selectedItemProperty().addListener((obs, oldValue, newValue) -> {
            deleteLineMenuItem.setDisable(newValue == null);
            if (newValue != null && !textField.isFocused()) {
                textField.setText(newValue);
            }
        });
    }

    public static Integer tryParse(String text) {
        try {
            return Integer.parseInt(text);
        } catch (NumberFormatException e) {
            return 1234;
        }
    }

    @FXML
    private void handleLinkServer() {
        String host = "localhost";
        int port = tryParse(textField.getText());
        out.println("LINK " + host + " " + port);
    }

    @FXML
    public void handleTextFieldUpdate() {
        int index = listView.getSelectionModel().getSelectedIndex();
        String text = textField.getText();
        out.println("MDFL " + (index + 1) + " " + text);
    }

    @FXML
    public void handleAddLine() {
        int index = listView.getSelectionModel().getSelectedIndex();

        if (index < 0) {
            index = listView.getItems().size();
        }

        String text = "Nouvelle ligne";
        out.println("ADDL " + (index + 1) + " " + text);
    }

    @FXML
    public void handleDeleteLine() {
        int index = listView.getSelectionModel().getSelectedIndex();
        out.println("RMVL " + (index + 1));
    }
}