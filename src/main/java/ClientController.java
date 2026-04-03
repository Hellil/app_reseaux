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

    public void setupNetwork(String dispatchHost, int dispatchPort) throws IOException {
        Socket dispatchSocket = new Socket(dispatchHost, dispatchPort);
        BufferedReader dispatchIn = new BufferedReader(new InputStreamReader(dispatchSocket.getInputStream()));
        
        String serverAddress = dispatchIn.readLine();
        dispatchSocket.close();

        if (serverAddress == null || !serverAddress.contains(":")) {
            throw new IOException("Adresse du serveur invalide reçue par le Dispatcher.");
        }

        String[] parts = serverAddress.split(":");
        String host = parts[0];
        int port = Integer.parseInt(parts[1]);

        System.out.println("Connecté au serveur de la fédération : " + host + ":" + port);

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

            if (index < listView.getItems().size()) {
                if (listView.getItems().size() >= index + 1) {
                    listView.getItems().add(index, text);
                } else {
                    listView.getItems().set(index, text);
                }
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
            // fin d'envoi initial
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
        var port = tryParse(textField.getText());
        if (port == null){
            port = -1;
        }

        out.println("LINK " + host + " " + port);
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