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

public class MasterServer {

    private final List<String> document = Collections.synchronizedList(new ArrayList<>());
    private final List<PrintWriter> connections = Collections.synchronizedList(new ArrayList<>());
    private final int port;

    public MasterServer(int port) {
        this.port = port;
        document.add("Première ligne du document partagé (MASTER).");
    }

    public void start() {
        try (ServerSocket serverSocket = new ServerSocket(port)) {
            System.out.println("Serveur MAÎTRE lancé sur le port " + port);

            while (true) {
                Socket socket = serverSocket.accept();
                new Thread(() -> handleConnection(socket)).start();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void handleConnection(Socket socket) {
        try {
            BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            PrintWriter out = new PrintWriter(socket.getOutputStream(), true);

            synchronized (connections) { connections.add(out); }

            // Envoi initial du document à la nouvelle connexion (Client ou Réplique)
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
                            if (idx >= 0 && idx < document.size()) {
                                document.set(idx, parts[2]);
                                broadcast("MDFL " + (idx + 1) + " " + parts[2]);
                            }
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
                    }
                }
            }
        } catch (IOException e) {
            System.out.println("Connexion perdue avec un nœud/client.");
        }
    }

    private void broadcast(String message) {
        synchronized (connections) {
            for (PrintWriter conn : connections) {
                conn.println(message);
            }
        }
    }

    public static Integer tryParse(String text) {
        try {
            return Integer.parseInt(text);
        } catch (NumberFormatException e) {
            return 1234;
        }
    }

    public static void main(String[] args) {
        new MasterServer(tryParse(args[0])).start();
    }
}