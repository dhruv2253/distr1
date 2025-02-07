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

            while (true) {
                Socket clientSocket = serverSocket.accept();
                System.out.println("New client connected: " + clientSocket.getInetAddress());

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
    private File currentDir;

    public ClientHandler(Socket socket) {
        this.clientSocket = socket;
        this.currentDir = new File(System.getProperty("user.dir"));
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
                if (command.startsWith("get ")) {
                    String fileName = command.substring(4);
                    sendFile(fileName, out);
                    out.println();
                    out.println();
                    out.println();
                    out.flush();

                } else if (command.startsWith("put ")) {
                    String fileName = command.substring(4);
                    receiveFile(fileName, in);
                    out.println();
                    out.println();
                    out.println();
                } else if (command.startsWith("delete ")) {
                    String fileName = command.substring(7);
                    deleteFile(fileName, out);
                } else if (command.equals("ls")) {
                    listFiles(out);
                } else if (command.startsWith("cd ")) {
                    String dir = command.substring(3);
                    changeDirectory(dir, out);
                } else if (command.startsWith("mkdir ")) {
                    String dir = command.substring(6);
                    makeDirectory(dir, out);
                } else if (command.equals("pwd")) {
                    printWorkingDirectory(out);
                } else {
                    out.println("Invalid command.");
                }
            }

            System.out.println("Client disconnected: " + clientSocket.getInetAddress());
            clientSocket.close();

        } catch (IOException e) {
            System.err.println("Client error: " + e.getMessage());
        }
    }

    private void sendFile(String fileName, PrintWriter out) throws IOException {
        File file = new File(currentDir, fileName); 
        if (file.exists() && !file.isDirectory()) {
            BufferedInputStream fin = new BufferedInputStream(new FileInputStream(file));
            byte[] buffer = new byte[4096];
            int bytesRead;

            while ((bytesRead = fin.read(buffer)) != -1) {
                out.write(new String(buffer, 0, bytesRead));
            }
            out.flush();
            out.write("END_OF_FILE");
            out.println();
            out.flush();
            fin.close();
            System.out.println("File sent: " + fileName);
        } else {
            out.println("File not found: " + fileName);
        }
    }

    private void receiveFile(String fileName, BufferedReader in) throws IOException {
        char[] buffer = new char[4096];
        StringBuilder fileContent = new StringBuilder();
        int charsRead = in.read(buffer);
        String chunk = new String(buffer, 0, charsRead);
        if (chunk.startsWith("File not found:")) {
            System.out.println("File not found on the client.");
            return;
        }
        FileOutputStream fout = new FileOutputStream(new File(currentDir, fileName));
    
        while (charsRead != -1) {
            fileContent.append(buffer, 0, charsRead);
    
            int markerIndex = fileContent.indexOf("END_OF_FILE");
            if (markerIndex != -1) {
                fout.write(fileContent.substring(0, markerIndex).getBytes());
                break;
            }
        }

        fout.close();
        System.out.println("File received: " + fileName);
    }

    private void deleteFile(String fileName, PrintWriter out) {
        File file = new File(currentDir, fileName);
        if (file.delete()) {
            out.println("File deleted: " + fileName);
        } else {
            out.println("File not found: " + fileName);
        }
    }

    private void listFiles(PrintWriter out) {
        File[] files = currentDir.listFiles(); 
        if (files != null && files.length > 0) {
            StringBuilder fileList = new StringBuilder();
            for (File file : files) {
                fileList.append(file.getName()).append(" ");
            }
            out.println(fileList.toString().trim());
        } else {
            out.println("No files found.");
        }
    }

    private void changeDirectory(String dir, PrintWriter out) {
        System.out.println("Current directory: " + currentDir.getAbsolutePath());

        File newDir;
        if (dir.equals("..")) {
            newDir = currentDir.getParentFile(); 
            if (newDir != null && newDir.exists()) {
                currentDir = newDir;
                out.println("Changed directory to " + currentDir.getAbsolutePath());
            } else {
                out.println("Already at the root directory.");
            }
        } else {
            newDir = new File(currentDir, dir); 
            if (newDir.exists() && newDir.isDirectory()) {
                currentDir = newDir;
                out.println("Changed directory to " + currentDir.getAbsolutePath());
            } else {
                out.println("Directory not found: " + dir);
            }
        }
        out.println();

        System.out.println("Updated directory: " + currentDir.getAbsolutePath());
    }

    private void makeDirectory(String dir, PrintWriter out) {
        File newDir = new File(currentDir, dir);
        if (newDir.mkdir()) {
            out.println("Directory created: " + dir);
        } else {
            out.println("Failed to create directory: " + dir);
        }
        out.println();
    }

    private void printWorkingDirectory(PrintWriter out) {
        out.println("Current directory: " + currentDir.getAbsolutePath());
    }
}