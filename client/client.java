import java.net.*;
import java.io.*;

public class client {
    public static void main(String[] args) {
        if (args.length != 2) {
            System.exit(1);
        }
        String server = args[0];
        int port = Integer.parseInt(args[1]);

        try {
            Socket socket = new Socket(server, port);
            BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            BufferedReader userIn = new BufferedReader(new InputStreamReader(System.in));
            PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
            System.out.println("Connected to a server");

            String response;
            while ((response = in.readLine()) != null) {
                System.out.println(response);

                if (response.equals("Terminating")) {
                    socket.close();
                    break;
                }

                System.out.print("myftp> ");
                String command = userIn.readLine();

                out.println(command);

                // Handle file commands (get, put)
                if (command.startsWith("get ")) {
                    String fileName = command.substring(4);
                    receiveFile(fileName, socket);
                } else if (command.startsWith("put ")) {
                    String fileName = command.substring(4);
                    sendFile(fileName, socket);
                } else if (command.equals("quit")) {
                    break;
                } else if (command.startsWith("cd ")) {
                    String directory = command.substring(3); // Get the directory from the command
                    out.println(command); // Send the cd command to the server
                    response = in.readLine(); // Wait for server's response
                    System.out.println(response); // Display the server's response (whether the directory was changed or not)
                }
                else if (command.startsWith("mkdir ")) {
                    String dirName = command.substring(6);
                    System.out.println("Attempting to create directory: " + dirName);
                    response = in.readLine(); // Read the server response
                    System.out.println("Server response: " + response);
                }
            }

        
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static void receiveFile(String fileName, Socket socket) throws IOException {
        BufferedInputStream bin = new BufferedInputStream(socket.getInputStream());
        FileOutputStream fout = new FileOutputStream(fileName);
        byte[] buffer = new byte[4096];
        int bytesRead;
        StringBuilder sb = new StringBuilder();
    
        while ((bytesRead = bin.read(buffer)) > 0) {
            String chunk = new String(buffer, 0, bytesRead);
            sb.append(chunk);
    
            int endOfFileIndex = sb.indexOf("END_OF_FILE");
            if (endOfFileIndex != -1) {
                fout.write(sb.substring(0, endOfFileIndex).getBytes());
                break;
            } else {
                fout.write(buffer, 0, bytesRead);
            }
        }
        
        fout.close();
        System.out.println("File " + fileName + " received.");
    }
    // Method to send a file to the server
   
private static void sendFile(String fileName, Socket socket) throws IOException {
    File file = new File(fileName);
    if (!file.exists()) {
        System.out.println("File not found: " + fileName);
        return;
    }
    
    // Create a file input stream to read the file
    FileInputStream fin = new FileInputStream(file);
    BufferedOutputStream bout = new BufferedOutputStream(socket.getOutputStream()); // Get output stream to send the file
    byte[] buffer = new byte[4096];
    int bytesRead;

    // Send the file content to the server in chunks
    while ((bytesRead = fin.read(buffer)) != -1) {
        bout.write(buffer, 0, bytesRead);
    }

    bout.flush();  // Ensure the data is sent
    fin.close();
    System.out.println("File " + fileName + " sent.");
}

}
