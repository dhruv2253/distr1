import java.io.*;
import java.net.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

public class server {
    private static final ConcurrentHashMap<Integer, Boolean> commandStatus = new ConcurrentHashMap<>();
    private static final AtomicInteger commandCounter = new AtomicInteger(0);
    private static final Object fileLock = new Object(); // Lock for synchronizing file operations

    public static void main(String[] args) {
        if (args.length != 2) {
            System.out.println("Usage: java server <nport> <tport>");
            System.exit(1);
        }

        int nport = Integer.parseInt(args[0]);
        int tport = Integer.parseInt(args[1]);

        try {
            ServerSocket normalServerSocket = new ServerSocket(nport);
            ServerSocket terminateServerSocket = new ServerSocket(tport);

            System.out.println("Server started. Listening on normal port " + nport + " and terminate port " + tport);

            // Thread to handle client connections
            new Thread(() -> {
                while (true) {
                    try {
                        Socket clientSocket = normalServerSocket.accept();
                        System.out.println("New client connected: " + clientSocket.getInetAddress());
                        new ClientHandler(clientSocket).start();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }).start();

            // Thread to handle termination commands
            new Thread(() -> {
                while (true) {
                    try {
                        Socket terminateSocket = terminateServerSocket.accept();
                        BufferedReader in = new BufferedReader(new InputStreamReader(terminateSocket.getInputStream()));
                        String command = in.readLine();

                        if (command.startsWith("terminate ")) {
                            int commandId = Integer.parseInt(command.substring(10));
                            commandStatus.put(commandId, true);
                            System.out.println("Termination request received for command ID: " + commandId);
                        }

                        terminateSocket.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }).start();

        } catch (IOException e) {
            System.err.println("Could not start server.");
            e.printStackTrace();
        }
    }

    static class ClientHandler extends Thread {
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

                String command;
                while ((command = in.readLine()) != null) {
                    System.out.println("Received: " + command);
                    if (command.equalsIgnoreCase("quit")) {
                        out.println("Terminating");
                        break;
                    }
                    if (command.startsWith("get ")) {
                        int commandId = commandCounter.incrementAndGet();
                        out.println("Command ID: " + commandId);
                        sendFile(command.substring(4), out, commandId);
                    } else if (command.startsWith("put ")) {
                        int commandId = commandCounter.incrementAndGet();
                        out.println("Command ID: " + commandId);
                        receiveFile(command.substring(4),
                                new BufferedReader(new InputStreamReader(clientSocket.getInputStream())), commandId);

                    } else if (command.startsWith("delete ")) {
                        deleteFile(command.substring(7), out);
                    } else if (command.equals("ls")) {
                        listFiles(out);
                    } else if (command.startsWith("cd ")) {
                        changeDirectory(command.substring(3), out);
                    } else if (command.startsWith("mkdir ")) {
                        makeDirectory(command.substring(6), out);
                    } else if (command.equals("pwd")) {
                        printWorkingDirectory(out);
                    } else {
                        out.println("Invalid command.");
                    }
                }

                clientSocket.close();

            } catch (IOException e) {
                System.err.println("Client error: " + e.getMessage());
            }
        }

        private void sendFile(String fileName, PrintWriter out, int commandId) throws IOException {
            File file = new File(currentDir, fileName);
            if (file.exists() && !file.isDirectory()) {
                BufferedInputStream fin = new BufferedInputStream(new FileInputStream(file));
                byte[] buffer = new byte[1024];
                int bytesRead;

                while ((bytesRead = fin.read(buffer)) != -1) {
                    if (commandStatus.getOrDefault(commandId, false)) {
                        System.out.println("Terminating command ID: " + commandId);
                        return;
                    }
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

        private void receiveFile(String fileName, BufferedReader in, int commandId) throws IOException {
            char[] buffer = new char[1024];
            StringBuilder fileContent = new StringBuilder();
            int charsRead;
            File file = new File(currentDir, fileName);
            FileOutputStream fout = new FileOutputStream(file);

            while ((charsRead = in.read(buffer)) != -1) {
                fileContent.append(buffer, 0, charsRead);
                System.out.println(commandStatus.getOrDefault(commandId, false));
                if (commandStatus.getOrDefault(commandId, false)) {
                    System.out.println("Terminating command ID: " + commandId);
                    fout.close();
                    file.delete();
                    System.out.println("file dle");
                    return;
                }
                int markerIndex = fileContent.indexOf("END_OF_FILE");
                if (markerIndex != -1) {
                    fout.write(fileContent.substring(0, markerIndex).getBytes());
                    break;
                } else {
                    fout.write(fileContent.toString().getBytes());
                    fileContent.setLength(0);
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
            out.flush();
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
            out.flush();
        }

        private void changeDirectory(String dir, PrintWriter out) {
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
            out.flush();
        }

        private void makeDirectory(String dir, PrintWriter out) {
            File newDir = new File(currentDir, dir);
            if (newDir.mkdir()) {
                out.println("Directory created: " + dir);
            } else {
                out.println("Failed to create directory: " + dir);
            }
            out.flush();
        }

        private void printWorkingDirectory(PrintWriter out) {
            out.println("Current directory: " + currentDir.getAbsolutePath());
            out.flush();
        }
    }
}
