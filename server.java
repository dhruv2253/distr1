import java.io.*;
import java.net.*;

public class server {
    public static void main(String[] args) {
        if (args.length != 1) {
            System.exit(1);
        }

        int port = Integer.parseInt(args[0]);

        try {
            ServerSocket serverSocket = new ServerSocket(port);
            System.out.println("Server started. Listening on port " + port);

            // multithreading
            while (true) {
                // accept client
                Socket clientSocket = serverSocket.accept();
                System.out.println("New client connected: " + clientSocket.getInetAddress());

                // create new thread
                ClientHandler thread = new ClientHandler(clientSocket);
                thread.start();
            }
        } catch (IOException e) {
            System.err.println("Could not start server on port " + port);
            e.printStackTrace();
        }
    }
}

class ClientHandler extends Thread {
    private Socket clientSocket;

    public ClientHandler(Socket socket) {
        this.clientSocket = socket;
    }

    @Override
    public void run() {
        try {
            BufferedReader in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
            PrintWriter out = new PrintWriter(clientSocket.getOutputStream(), true);
            out.println("Welcome to MyFTP Server!");

            String command;
            while ((command = in.readLine()) != null) {
                System.out.println("Received from " + clientSocket.getInetAddress() + ": " + command);
                if (command.equalsIgnoreCase("quit")) {
                    out.println("Terminating");
                    break;
                }
                out.println("Command received: " + command);
            }

            System.out.println("Client disconnected: " + clientSocket.getInetAddress());
            clientSocket.close();

        } catch (IOException e) {
            System.err.println("Client error: " + e.getMessage());
        }
    }
}
