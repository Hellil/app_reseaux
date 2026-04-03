package main.java;

import java.io.IOException;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;

public class ServerDispatch {

    private final int port;
    private final List<String> federationServers;
    private int currentIndex = 0;

    public ServerDispatch(int port) {
        this.port = port;
        this.federationServers = new ArrayList<>();
        
        federationServers.add("localhost:12346"); 
        federationServers.add("localhost:12347");
        federationServers.add("localhost:12348");
    }

    public void start() {
        try (ServerSocket serverSocket = new ServerSocket(port)) {
            System.out.println("Serveur Dispatch lancé sur le port " + port);

            while (true) {
                Socket clientSocket = serverSocket.accept();
                handleClient(clientSocket);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void handleClient(Socket clientSocket) {
        try (PrintWriter out = new PrintWriter(clientSocket.getOutputStream(), true)) {
            String targetServer;
            
            synchronized (federationServers) {
                targetServer = federationServers.get(currentIndex);
                currentIndex = (currentIndex + 1) % federationServers.size();
            }

            System.out.println("Client redirigé vers : " + targetServer);
            out.println(targetServer);

        } catch (IOException e) {
            System.out.println("Erreur lors de la redirection du client.");
        } finally {
            try {
                clientSocket.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public static void main(String[] args) {
        new ServerDispatch(10000).start();
    }
}