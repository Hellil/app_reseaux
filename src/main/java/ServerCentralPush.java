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
    private final List<PrintWriter> localClients = Collections.synchronizedList(new ArrayList<>());
    
    private final int localPort;
    private final String masterHost;
    private final int masterPort;
    
    private PrintWriter masterOut;

    public ServerCentralPush(int localPort, String masterHost, int masterPort) {
        this.localPort = localPort;
        this.masterHost = masterHost;
        this.masterPort = masterPort;
    }

    public void start() {
        connectToMaster();

        try (ServerSocket serverSocket = new ServerSocket(localPort)) {
            System.out.println("Serveur secondaire lance sur le port " + localPort + " (lie au maitre " + masterHost + ":" + masterPort + ")");

            while (true) {
                Socket socket = serverSocket.accept();
                new Thread(() -> handleLocalClient(socket)).start();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void connectToMaster() {
        try {
            Socket masterSocket = new Socket(masterHost, masterPort);
            masterOut = new PrintWriter(masterSocket.getOutputStream(), true);
            BufferedReader masterIn = new BufferedReader(new InputStreamReader(masterSocket.getInputStream()));

            System.out.println("Connecte au serveur maitre");

            new Thread(() -> {
                try {
                    String msg;
                    while ((msg = masterIn.readLine()) != null) {
                        handleMasterMessage(msg);
                    }
                } catch (IOException e) {
                    System.out.println("Connexion au serveur maitre perdue");
                }
            }).start();

        } catch (IOException e) {
            System.err.println("Impossible de se connecter au serveur maitre");
            System.exit(1);
        }
    }

    private void handleMasterMessage(String msg) {
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
                broadcastToLocalClients(msg);

            } else if (msg.startsWith("MDFL ")) {
                String[] parts = msg.split(" ", 3);
                int index = Integer.parseInt(parts[1]) - 1;
                String text = parts[2];

                if (index < document.size()) {
                    document.set(index, text);
                }
                broadcastToLocalClients(msg);

            } else if (msg.startsWith("DELL ")) {
                int index = Integer.parseInt(msg.split(" ")[1]) - 1;
                if (index >= 0 && index < document.size()) {
                    document.remove(index);
                }
                broadcastToLocalClients(msg);
            } else if (msg.equals("DONE")) {
                // fin sync
            }
        }
    }

    private void handleLocalClient(Socket socket) {
        try {
            BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            PrintWriter out = new PrintWriter(socket.getOutputStream(), true);

            synchronized (localClients) { localClients.add(out); }

            synchronized (document) {
                for (int i = 0; i < document.size(); i++) {
                    out.println("LINE " + (i + 1) + " " + document.get(i));
                }
            }
            out.println("DONE");

            String request;
            while ((request = in.readLine()) != null) {
                if (request.startsWith("MDFL") || request.startsWith("ADDL") || request.startsWith("RMVL")) {
                    if (masterOut != null) {
                        masterOut.println(request);
                    }
                }
            }

        } catch (IOException e) {
            System.out.println("Client local déconnecté.");
        }
    }

    private void broadcastToLocalClients(String message) {
        synchronized (localClients) {
            for (PrintWriter client : localClients) {
                client.println(message);
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
        int localPort = tryParse(args[0]);
        String masterHost = "localhost";
        int masterPort = tryParse(args[1]);

        new ServerCentralPush(localPort, masterHost, masterPort).start();
    }
}