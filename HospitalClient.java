import javax.net.ssl.*;
import java.io.*;
import java.util.Scanner;

public class HospitalClient {
    private static final String HOST = "localhost";
    private static final int PORT = 9876;

    public static void main(String[] args) {
        // Default to DrBob to make manual testing faster
        String user = "doctor_DrBob"; 
        
        // Allow passing a specific user via command line args
        if (args.length > 0) {
            user = args[0];
        }

        System.out.println("Connecting as: " + user);
        // Load the user's specific keystore to prove identity to the server
        // This acts as our two-factor authentication (possessing the file + knowing the password)
        System.setProperty("javax.net.ssl.keyStore", user + ".jks");
        System.setProperty("javax.net.ssl.keyStorePassword", "password");

        // Load the truststore so the client trusts the server's certificate
        System.setProperty("javax.net.ssl.trustStore", "clienttruststore.jks");
        System.setProperty("javax.net.ssl.trustStorePassword", "password");

        try {
            // Establish the secure TLS connection
            SSLSocketFactory factory = (SSLSocketFactory) SSLSocketFactory.getDefault();
            SSLSocket socket = (SSLSocket) factory.createSocket(HOST, PORT);
            socket.startHandshake();

            PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
            BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            Scanner console = new Scanner(System.in);

            // Read the welcome message from the server
            System.out.println(in.readLine());

            // Simple REPL loop to send commands to the server
            while (true) {
                System.out.print("CMD: ");
                String cmd = console.nextLine();
                out.println(cmd);
                if (cmd.equalsIgnoreCase("EXIT")) break;

                // Print the server's response (either the data, success msg, or denial)
                System.out.println("RESPONSE: " + in.readLine());
            }
            socket.close();
            console.close();
        } catch (Exception e) {
            System.out.println("Connection Failed! Verify " + user + ".jks exists.");
        }
    }
}