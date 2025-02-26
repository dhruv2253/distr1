import java.io.*;
import java.net.*;

public class client {
    private static String server;
    private static int port;
    private static int terminatePort;

    public static void main(String[] args) {
        if (args.length != 3) {
            System.out.println("Usage: java FTPClient <server> <port> <terminatePort>");
            System.exit(1);
        }

        server = args[0];
        port = Integer.parseInt(args[1]);
        terminatePort = Integer.parseInt(args[2]);

        try (Socket socket = new Socket(server, port);
                BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                BufferedReader userIn = new BufferedReader(new InputStreamReader(System.in));
                PrintWriter out = new PrintWriter(socket.getOutputStream(), true)) {

            System.out.println("Connected to the server");
            String response;

            while (true) {
                System.out.print("mytftp> ");
                String command = userIn.readLine();
                if (command == null)
                    break;

                boolean background = command.endsWith("&");
                if (background)
                    command = command.substring(0, command.length() - 1).trim();

                out.println(command);
                response = in.readLine();
                System.out.println(response);

                if (command.startsWith("get ")) {
                    String fileName = command.substring(4);
                    Runnable task = () -> receiveFile(fileName);
                    new Thread(task).start();
                } else if (command.startsWith("put ")) {
                    String fileName = command.substring(4);
                    Runnable task = () -> sendFile(fileName);
                    new Thread(task).start();
                } else if (command.equals("quit")) {
                    break;
                } else if (command.startsWith("terminate ")) {
                    int commandId = Integer.parseInt(command.split(" ")[1]);
                    terminateCommand(commandId);
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static void receiveFile(String fileName) {
        try (Socket fileSocket = new Socket(server, port);
                InputStream in = fileSocket.getInputStream();
                FileOutputStream fout = new FileOutputStream(fileName)) {

            byte[] buffer = new byte[4096];
            int bytesRead;
            while ((bytesRead = in.read(buffer)) != -1) {
                fout.write(buffer, 0, bytesRead);
            }
            System.out.println("File " + fileName + " received.");
        } catch (IOException e) {
            System.out.println("Error receiving file: " + e.getMessage());
        }
    }

    private static void sendFile(String fileName) {
        try (Socket fileSocket = new Socket(server, port);
                FileInputStream fin = new FileInputStream(fileName);
                OutputStream out = fileSocket.getOutputStream()) {

            byte[] buffer = new byte[4096];
            int bytesRead;
            while ((bytesRead = fin.read(buffer)) != -1) {
                out.write(buffer, 0, bytesRead);
            }
            System.out.println("File " + fileName + " sent.");
        } catch (IOException e) {
            System.out.println("Error sending file: " + e.getMessage());
        }
    }

    private static void terminateCommand(int commandId) {
        try (Socket terminateSocket = new Socket(server, terminatePort);
                PrintWriter out = new PrintWriter(terminateSocket.getOutputStream(), true)) {

            out.println("terminate " + commandId);
            System.out.println("Terminate request sent for command " + commandId);
        } catch (IOException e) {
            System.out.println("Error sending terminate command: " + e.getMessage());
        }
    }
}
