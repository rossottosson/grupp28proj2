import javax.net.ssl.*;
import java.io.*;
import java.util.Scanner;

public class HospitalClient {
    private static final String HOST = "localhost";
    private static final int PORT = 9876;

    public static void main(String[] args) {
        // Default to DrBob if no argument is given
        String user = "doctor_DrBob"; 
        
        // Check if user passed a specific name (e.g., "java HospitalClient nurse_NurseEve")
        if (args.length > 0) {
            user = args[0];
        }

        System.out.println("Connecting as: " + user);
        
        // Dynamically load the correct keystore
        System.setProperty("javax.net.ssl.keyStore", user + ".jks");
        System.setProperty("javax.net.ssl.keyStorePassword", "password");
        System.setProperty("javax.net.ssl.trustStore", "clienttruststore.jks");
        System.setProperty("javax.net.ssl.trustStorePassword", "password");

        try {
            SSLSocketFactory factory = (SSLSocketFactory) SSLSocketFactory.getDefault();
            SSLSocket socket = (SSLSocket) factory.createSocket(HOST, PORT);

            socket.startHandshake();

            PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
            BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            Scanner console = new Scanner(System.in);

            System.out.println(in.readLine()); // Server Welcome

            while (true) {
                System.out.print("CMD (READ/WRITE/DELETE <ID>): ");
                String cmd = console.nextLine();
                out.println(cmd);
                if (cmd.equalsIgnoreCase("EXIT")) break;
                System.out.println("RESPONSE: " + in.readLine());
            }
            socket.close();
            console.close();
        } catch (Exception e) {
            System.out.println("Connection Failed! Make sure " + user + ".jks exists.");
            e.printStackTrace();
        }
    }
}