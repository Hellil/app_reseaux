package main.java;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

public class ServerCentralPush {

    private final List<String> document = Collections.synchronizedList(new ArrayList<>());
    private final List<PrintWriter> clients = Collections.synchronizedList(new ArrayList<>());
    private final Set<String> seenMessages = Collections.synchronizedSet(new HashSet<>());

    private final int port;

    public ServerCentralPush(int port) {
        this.port = port;
        document.add("Première ligne du document partagé.");
    }

    public void start() {
        try (ServerSocket serverSocket = new ServerSocket(port)) {
            System.out.println("Serveur lancé sur port " + port);

            while (true) {
                Socket socket = serverSocket.accept();
                new Thread(() -> handleClient(socket)).start();
            }

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void handleClient(Socket socket) {
        try {
            BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            PrintWriter out = new PrintWriter(socket.getOutputStream(), true);

            synchronized (clients) { clients.add(out); }

            // envoi initial (SANS ID)
            synchronized (document) {
                for (int i = 0; i < document.size(); i++) {
                    out.println("LINE " + (i + 1) + " " + document.get(i));
                }
            }
            out.println("DONE");

            String request;
            while ((request = in.readLine()) != null) {

                String msgId;
                String realMsg;

                // message avec ID ?
                if (request.contains("|")) {
                    String[] split = request.split("\\|", 2);
                    msgId = split[0];
                    realMsg = split[1];

                    synchronized (seenMessages) {
                        if (seenMessages.contains(msgId)) continue;
                        seenMessages.add(msgId);
                    }

                } else {
                    // message venant d’un client → créer ID
                    msgId = UUID.randomUUID().toString();
                    realMsg = request;

                    synchronized (seenMessages) {
                        seenMessages.add(msgId);
                    }
                }

                String[] parts = realMsg.split(" ", 3);
                String cmd = parts[0];

                synchronized (document) {

                    switch (cmd) {

                        case "MDFL":
                            int idx = Integer.parseInt(parts[1]) - 1;
                            document.set(idx, parts[2]);
                            broadcast(msgId, "MDFL " + (idx + 1) + " " + parts[2]);
                            break;

                        case "ADDL":
                            int addIdx = Integer.parseInt(parts[1]) - 1;
                            document.add(Math.min(addIdx, document.size()), parts[2]);
                            broadcast(msgId, "LINE " + (addIdx + 1) + " " + parts[2]);
                            break;

                        case "RMVL":
                            int rmIdx = Integer.parseInt(parts[1]) - 1;
                            if (rmIdx >= 0 && rmIdx < document.size()) {
                                document.remove(rmIdx);
                                broadcast(msgId, "DELL " + (rmIdx + 1));
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
            System.out.println("Client déconnecté.");
        }
    }

    private void connectToServer(String host, int port) {
        new Thread(() -> {
            try {
                Socket socket = new Socket(host, port);
                BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));

                String line;
                while ((line = in.readLine()) != null) {
                    handleRemoteMessage(line);
                }

            } catch (IOException e) {
                System.out.println("Erreur connexion serveur distant");
            }
        }).start();
    }

    private void handleRemoteMessage(String fullMsg) {

        if (!fullMsg.contains("|")) return;

        String[] split = fullMsg.split("\\|", 2);
        String msgId = split[0];
        String msg = split[1];

        synchronized (seenMessages) {
            if (seenMessages.contains(msgId)) return;
            seenMessages.add(msgId);
        }

        synchronized (document) {

            if (msg.startsWith("LINE ")) {
                String[] parts = msg.split(" ", 3);
                int index = Integer.parseInt(parts[1]) - 1;
                String text = parts[2];

                if (index <= document.size()) {
                    document.add(index, text);
                } else {
                    document.add(text);
                }

            } else if (msg.startsWith("MDFL ")) {
                String[] parts = msg.split(" ", 3);
                int index = Integer.parseInt(parts[1]) - 1;
                String text = parts[2];

                if (index < document.size()) {
                    document.set(index, text);
                }

            } else if (msg.startsWith("DELL ")) {
                int index = Integer.parseInt(msg.split(" ")[1]) - 1;

                if (index >= 0 && index < document.size()) {
                    document.remove(index);
                }
            }
        }

        // propagation aux autres serveurs + clients
        broadcast(msgId, msg);
    }

    private void broadcast(String msgId, String message) {
        String fullMessage = msgId + "|" + message;

        synchronized (clients) {
            for (PrintWriter client : clients) {
                client.println(fullMessage);
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
        new ServerCentralPush(tryParse(args[0])).start();
    }
}