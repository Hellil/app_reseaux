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

public class ServerCentralPush {
    private final List<String> document = Collections.synchronizedList(new ArrayList<>());
    private final List<PrintWriter> clients = Collections.synchronizedList(new ArrayList<>());
    private final List<PrintWriter> servers = Collections.synchronizedList(new ArrayList<>());
    private final int port;

    public ServerCentralPush(int port) {
        this.port = port;
        document.add("Première ligne du document partagé.");
    }

    public void start() {
        try (ServerSocket serverSocket = new ServerSocket(port)) {
            System.out.println("ServerCentralPush démarré sur le port " + port);
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

            // ajouter client à la liste
            synchronized (clients) { clients.add(out); }

            // envoyer l'état complet du document au nouveau client
            synchronized (document) {
                for (int i = 0; i < document.size(); i++) {
                    out.println("LINE " + (i + 1) + " " + document.get(i));
                }
            }
            out.println("DONE");

            String request;
            while ((request = in.readLine()) != null) {
                String[] parts = request.split(" ", 3);
                String cmd = parts[0];

                synchronized (document) {
                    switch (cmd) {
                        case "MDFL":
                            int idx = Integer.parseInt(parts[1]) - 1;
                            document.set(idx, parts[2]);
                            broadcast("MDFL " + (idx + 1) + " " + parts[2]);
                            break;

                        case "ADDL":
                            int addIdx = Integer.parseInt(parts[1]) - 1;
                            document.add(Math.min(addIdx, document.size()), parts[2]);
                            broadcast("LINE " + (addIdx + 1) + " " + parts[2]);
                            break;

                        case "RMVL":
                            int rmIdx = Integer.parseInt(parts[1]) - 1;
                            if (rmIdx >= 0 && rmIdx < document.size()) {
                                document.remove(rmIdx);
                                broadcast("DELL " + (rmIdx + 1));
                            }
                            break;

                        case "LINK":
                            String host = parts[1];
                            int port = Integer.parseInt(parts[2]);
                            connectToServer(host, port);
                            break;

                        default:
                            out.println("ERRL Commande inconnue");
                    }
                }
            }
        } catch (IOException e) {
            System.err.println("Client déconnecté.");
        } finally {
            // retirer client
            synchronized (clients) { clients.removeIf(p -> p.checkError()); }
        }
    }

    private void connectToServer(String host, int port) {
        new Thread(() -> {
            try {
                Socket socket = new Socket(host, port);

                PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
                BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));

                synchronized (servers) {
                    servers.add(out);
                }

                System.out.println("Connecté à serveur : " + host + ":" + port);

                String line;
                while ((line = in.readLine()) != null) {
                    handleServerMessage(line);
                }

            } catch (IOException e) {
                System.err.println("Erreur connexion serveur distant");
            }
        }).start();
    }

    private void handleServerMessage(String msg) {
        String[] parts = msg.split(" ", 3);

        synchronized (document) {
            switch (parts[0]) {

                case "MDFL":
                    int idx = Integer.parseInt(parts[1]) - 1;
                    document.set(idx, parts[2]);
                    broadcast(msg);
                    break;

                case "LINE":
                    int addIdx = Integer.parseInt(parts[1]) - 1;
                    document.add(Math.min(addIdx, document.size()), parts[2]);
                    broadcast(msg);
                    break;

                case "DELL":
                    int rmIdx = Integer.parseInt(parts[1]) - 1;
                    if (rmIdx >= 0 && rmIdx < document.size()) {
                        document.remove(rmIdx);
                        broadcast(msg);
                    }
                    break;
            }
        }
    }

    private void broadcast(String message) {
        synchronized (clients) {
            for (PrintWriter client : clients) {
                client.println(message);
            }
        }

        synchronized (servers) {
            for (PrintWriter server : servers) {
                server.println(message);
            }
        }
    }

    public static void main(String[] args) {
        new ServerCentralPush(ClientController.tryParse(args[0])).start();
    }
}