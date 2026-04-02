package main.java;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class AutoClient {
    private List<String> document = new ArrayList<>();

    public void start(String host, int port, int id) throws Exception {
        Socket socket = new Socket(host, port);
        PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
        BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));

        // thread réception (push)
        new Thread(() -> {
            try {
                String msg;
                while ((msg = in.readLine()) != null) {
                    handleMessage(msg);
                }
            } catch (IOException e) { }
        }).start();

        Random rand = new Random();

        // effectuer des modifications automatiquement
        for (int i = 0; i < 5; i++) {
            Thread.sleep(500 + rand.nextInt(500));

            int action = rand.nextInt(3);

            if (action == 0 && !document.isEmpty()) {
                // modifier ligne
                int idx = rand.nextInt(document.size());
                String text = "Client" + id + "_modif_" + i;
                out.println("MDFL " + (idx + 1) + " " + text);
            } else if (action == 1) {
                // ajouter ligne
                int idx = document.size();
                String text = "Client" + id + "_add_" + i;
                out.println("ADDL " + (idx + 1) + " " + text);
            } else if (action == 2 && !document.isEmpty()) {
                // supprimer ligne
                int idx = rand.nextInt(document.size());
                out.println("RMVL " + (idx + 1));
            }
        }

        // attendre propagation
        Thread.sleep(2000);

        System.out.println("Client " + id + " document final:");
        for (String line : document) {
            System.out.println(line);
        }

        socket.close();
    }

    private void handleMessage(String msg) {
        if (msg.startsWith("LINE ")) {
            String[] parts = msg.split(" ", 3);
            int index = Integer.parseInt(parts[1]) - 1;
            String text = parts[2];
            if (index <= document.size()) {
                document.add(index, text);
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
            if (index < document.size()) {
                document.remove(index);
            }
        }
    }

    public static void main(String[] args) throws Exception {
        int id = Integer.parseInt(args[0]);
        new AutoClient().start("localhost", 12345, id);
    }
}