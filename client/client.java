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
                if (command.startsWith("terminate ")) {
                    int commandId = Integer.parseInt(command.split(" ")[1]);
                    terminateCommand(commandId);
                    continue;
                } else {
                    out.println(command);
                }

                response = in.readLine();
                System.out.println(response);

                if (command.startsWith("get ")) {
                    String fileName = command.substring(4);
                    if (background) {
                        Runnable task = () -> {
                            try {
                                receiveFile(fileName, socket); // Pass the socket here
                            } catch (IOException e) {
                                e.printStackTrace();
                            }
                        };
                        new Thread(task).start();
                    } else {
                        receiveFile(fileName, socket);
                    }
                } else if (command.startsWith("put ")) {
                    String fileName = command.substring(4);
                    if (background) {
                        Runnable task = () -> {
                            try {
                                sendFile(fileName, socket); // Pass the socket here
                            } catch (IOException e) {
                                e.printStackTrace();
                            }
                        };
                        new Thread(task).start();
                    } else {
                        sendFile(fileName, socket);
                    }
                } else if (command.equals("quit")) {
                    break;
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static void receiveFile(String fileName, Socket socket) throws IOException {
        BufferedInputStream bin = new BufferedInputStream(socket.getInputStream());
        byte[] buffer = new byte[1024];
        int bytesRead = bin.read(buffer);
        StringBuilder sb = new StringBuilder();

        String chunk = new String(buffer, 0, bytesRead);
        if (chunk.contains("File not found")) {
            System.out.println("File not found on the server.");
            return;
        }
        File file = new File(fileName);
        FileOutputStream fout = new FileOutputStream(file);
        boolean signal = false;
        while (bytesRead > 0) {
            chunk = new String(buffer, 0, bytesRead);
            sb.append(chunk);
            if (chunk.contains("termination_signal") || signal) {
                file.delete();
                signal = true;
                continue;
            }
            int endOfFileIndex = sb.indexOf("END_OF_FILE");
            if (endOfFileIndex != -1) {
                fout.write(sb.substring(0, endOfFileIndex).getBytes());
                break;
            } else {
                fout.write(buffer, 0, bytesRead);
            }
            bytesRead = bin.read(buffer);
        }
        fout.flush();
        fout.close();
    }

    private static void sendFile(String fileName, Socket socket) throws IOException {
        File file = new File(fileName);
        if (!file.exists()) {
            System.out.println("File not found: " + fileName);
            return;
        }

        FileInputStream fin = new FileInputStream(file);
        BufferedOutputStream bout = new BufferedOutputStream(socket.getOutputStream());

        byte[] buffer = new byte[1024];
        int bytesRead;

        while ((bytesRead = fin.read(buffer)) != -1) {
            bout.write(buffer, 0, bytesRead);
        }
        bout.write("END_OF_FILE".getBytes());

        bout.flush();
        fin.close();
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
