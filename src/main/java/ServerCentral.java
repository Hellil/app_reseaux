package main.java;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class ServerCentral {
    // liste de lignes dans le document
    private final List<String> document = Collections.synchronizedList(new ArrayList<>());
    private final int port;

    public ServerCentral(int port) {
        this.port = port;
        // premiere ligne temporaire
        document.add("Première ligne du document partagé.");
    }

    public void start() {
        try (ServerSocket serverSocket = new ServerSocket(port)) {
            System.out.println("Serveur centralisé démarré sur le port " + port);
            while (true) {
                Socket clientSocket = serverSocket.accept();
                new Thread(() -> handleClient(clientSocket)).start();
            }
        } catch (IOException e) { e.printStackTrace(); }
    }

    private void handleClient(Socket socket) {
        try {
            BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
            String request;
            while ((request = in.readLine()) != null) {
                String[] parts = request.split(" ", 3);
                String cmd = parts[0];

                try {
                    switch (cmd) {
                        case "GETD": // get document (en entier)
                            synchronized (document) {
                                for (int i = 0; i < document.size(); i++) {
                                    out.println("LINE " + (i + 1) + " " + document.get(i));
                                }
                            }
                            out.println("DONE");
                            break;

                        case "MDFL": // modify line
                            document.set(Integer.parseInt(parts[1]) - 1, parts[2]);
                            break;

                        case "ADDL": // add line
                            int addIdx = Integer.parseInt(parts[1]) - 1;
                            document.add(Math.min(addIdx, document.size()), parts[2]);
                            break;

                        case "RMVL": // remove line
                            int rmIdx = Integer.parseInt(parts[1]) - 1;
                            if (rmIdx >= 0 && rmIdx < document.size()) document.remove(rmIdx);
                            break;

                        default:
                            out.println("ERRL Commande inconnue"); // error line
                    }
                } catch (Exception e) {
                    out.println("ERRL Erreur de format : " + e.getMessage());
                }
            }
        } catch (IOException e) { System.err.println("Client déconnecté."); }
    }

    public static void main(String[] args) {
        new ServerCentral(12345).start();
    }
}