package main.java;

import java.io.*;
import java.net.*;
import java.util.*;

public class MasterServer {

    private static class ClientHandler {
        PrintWriter out;
        List<String> document = new ArrayList<>();
    }

    private final List<ClientHandler> clients = Collections.synchronizedList(new ArrayList<>());
    private final List<String> masterDocument = Collections.synchronizedList(new ArrayList<>());
    private final int port;

    public MasterServer(int port) {
        this.port = port;
        masterDocument.add("Première ligne du document partagé (MASTER).");
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

            ClientHandler client = new ClientHandler();
            client.out = out;

            // copie du document initial
            synchronized (masterDocument) {
                client.document.addAll(masterDocument);
            }

            clients.add(client);

            // envoi initial
            for (int i = 0; i < client.document.size(); i++) {
                out.println("LINE " + (i + 1) + " " + client.document.get(i));
            }
            out.println("DONE");

            String request;
            while ((request = in.readLine()) != null) {
                String[] parts = request.split(" ", 3);
                String cmd = parts[0];

                switch (cmd) {

                    case "MDFL": {
                        int idx = Integer.parseInt(parts[1]) - 1;
                        if (idx >= 0 && idx < client.document.size()) {
                            client.document.set(idx, parts[2]);
                            client.out.println("MDFL " + (idx + 1) + " " + parts[2]);
                        }
                        break;
                    }

                    case "ADDL": {
                        int idx = Integer.parseInt(parts[1]) - 1;

                        if (idx < 0) idx = 0;
                        if (idx > client.document.size()) idx = client.document.size();

                        client.document.add(idx, parts[2]);
                        client.out.println("LINE " + (idx + 1) + " " + parts[2]);
                        break;
}

                    case "RMVL": {
                        int idx = Integer.parseInt(parts[1]) - 1;
                        if (idx >= 0 && idx < client.document.size()) {
                            client.document.remove(idx);
                            client.out.println("DELL " + (idx + 1));
                        }
                        break;
                    }

                    case "LINK": {
                        String host = parts[1];
                        int port = Integer.parseInt(parts[2]);

                        try {
                            Socket linkSocket = new Socket(host, port);
                            PrintWriter linkOut = new PrintWriter(linkSocket.getOutputStream(), true);

                            synchronized (client.document) {
                                for (int i = 0; i < client.document.size(); i++) {
                                    linkOut.println("LINE " + (i + 1) + " " + client.document.get(i));
                                }
                            }
                            linkOut.println("DONE");

                        } catch (IOException e) {
                            client.out.println("ERROR LINK FAILED");
                        }
                        break;
                    }
                }
            }

        } catch (IOException e) {
            System.out.println("Connexion perdue avec un client.");
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