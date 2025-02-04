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
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
