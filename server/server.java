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
                if (command.startsWith("get ")) {
                    String fileName = command.substring(4);
                    sendFile(fileName, out);
                } else if (command.startsWith("put ")) {
                    String fileName = command.substring(4);
                    receiveFile(fileName, in);
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
        File file = new File(fileName);
        if (file.exists() && !file.isDirectory()) {
            BufferedInputStream fin = new BufferedInputStream(new FileInputStream(file));
            byte[] buffer = new byte[4096];
            int bytesRead;

            while ((bytesRead = fin.read(buffer)) != -1) {
                out.write(new String(buffer, 0, bytesRead));
                // test print
                System.out.println("in loop 2");
            }

            out.flush();
            fin.close();
            System.out.println("File sent: " + fileName);
        } else {
            out.println("File not found: " + fileName);
        }
    }

    private void receiveFile(String fileName, BufferedReader in) throws IOException {
        BufferedOutputStream fout = new BufferedOutputStream(new FileOutputStream(fileName));
        char[] buffer = new char[4096];  // This buffer should be byte-based for binary data
        int bytesRead;
    
        while ((bytesRead = in.read(buffer)) != -1) {
            fout.write(new String(buffer, 0, bytesRead).getBytes());  // Write data as bytes
        }
    
        fout.close();
        System.out.println("File received: " + fileName);
    }
    

    private void deleteFile(String fileName, PrintWriter out) {
        File file = new File(fileName);
        if (file.delete()) {
            out.println("File deleted: " + fileName);
        } else {
            out.println("File not found: " + fileName);
        }
    }

    private void listFiles(PrintWriter out) {
        File currentDir = new File(".");
        File[] files = currentDir.listFiles();
        
        if (files != null && files.length > 0) {
            StringBuilder fileList = new StringBuilder();
            for (File file : files) {
                fileList.append(file.getName()).append(" ");  // Add file name to the list with space delimiter
            }
            out.println(fileList.toString().trim());  // Send all files at once
        } else {
            out.println("No files found.");
        }
    }

    private void changeDirectory(String dir, PrintWriter out) {
        // Print the current working directory for debugging
        System.out.println("Current working directory: " + System.getProperty("user.dir"));
    
        File newDir;
    
        // Check for "cd .." to move up one directory
        if (dir.equals("..")) {
            // Get the absolute path and go up one directory
            newDir = new File(System.getProperty("user.dir")).getParentFile();
            
            // Check if the parent exists and is a valid directory
            if (newDir != null && newDir.exists()) {
                System.setProperty("user.dir", newDir.getAbsolutePath()); // Change the working directory
                out.println("Changed directory to " + newDir.getAbsolutePath());
            } else {
                out.println("Already at the root directory.");
            }
        } else {
            // Handle other cases (absolute or relative path)
            newDir = new File(System.getProperty("user.dir"), dir); // Resolve relative paths
            
            // Ensure that the directory exists and is valid
            if (newDir.exists() && newDir.isDirectory()) {
                System.setProperty("user.dir", newDir.getAbsolutePath()); // Change the working directory
                out.println("Changed directory to " + newDir.getAbsolutePath());
            } else {
                out.println("Directory not found: " + dir);
            }
        }
    
        // Print the updated working directory for debugging
        System.out.println("Updated working directory: " + System.getProperty("user.dir"));
    }
    
    

    private void makeDirectory(String dir, PrintWriter out) {
        File newDir = new File(dir);
        if (newDir.mkdir()) {
            out.println("Directory created: " + dir);
        } else {
            out.println("Failed to create directory: " + dir);
        }
    }

    private void printWorkingDirectory(PrintWriter out) {
        out.println("Current directory: " + System.getProperty("user.dir"));
    }
}
