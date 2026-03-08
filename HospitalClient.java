import javax.net.ssl.*;
import java.io.*;
import java.util.Scanner;

public class HospitalClient {
    private static final String HOST = "localhost";
    private static final int PORT = 9876;

    public static void main(String[] args) {
        Scanner console = new Scanner(System.in);

        // 1. Welcome Banner & Login Prompt
        System.out.println("=========================================");
        System.out.println("   Welcome to the Secure Hospital System ");
        System.out.println("=========================================");
        System.out.println("Available demo users: doctor_DrBob, nurse_NurseEve, patient_Alice, agency_GovOrg");
        System.out.print("Please enter your login (e.g., doctor_DrBob): ");
        
        String user = console.nextLine().trim();
        if (user.isEmpty()) {
            user = "doctor_DrBob"; // Default fallback
            System.out.println("No input detected. Defaulting to doctor_DrBob");
        }

        // 2. Two-Factor Authentication: Prompt for the keystore password
        System.out.print("Enter keystore password: ");
        String password = console.nextLine().trim();

        System.out.println("\n[System] Loading physical token (keystore) for: " + user + "...");
        
        // Load the user's specific keystore to prove identity to the server
        System.setProperty("javax.net.ssl.keyStore", user + ".jks");
        System.setProperty("javax.net.ssl.keyStorePassword", password);

        // Load the truststore so the client trusts the server's CA 
        System.setProperty("javax.net.ssl.trustStore", "clienttruststore.jks");
        System.setProperty("javax.net.ssl.trustStorePassword", "password");

        try {
            System.out.println("[System] Establishing secure TLS connection...\n");
            
            // Establish the secure TLS connection
            SSLSocketFactory factory = (SSLSocketFactory) SSLSocketFactory.getDefault();
            SSLSocket socket = (SSLSocket) factory.createSocket(HOST, PORT);
            socket.startHandshake();

            PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
            BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));

            // Read the welcome message from the server
            System.out.println(">> SERVER: " + in.readLine());

            // Show the user what they can do
            printMenu();

            // Interactive REPL loop
            while (true) {
                System.out.print("\nEnter command: ");
                String cmd = console.nextLine().trim();
                
                if (cmd.isEmpty()) continue; // Ignore accidental enter presses
                
                // Intercept local commands BEFORE sending to the server to prevent network desync
                if (cmd.equalsIgnoreCase("HELP")) {
                    printMenu();
                    continue; 
                }

                if (cmd.equalsIgnoreCase("EXIT")) {
                    out.println(cmd); // Tell the server we are leaving cleanly
                    System.out.println("Logging out. Goodbye!");
                    break;
                }
                
                // If it is a real command, send it to the server
                out.println(cmd);
                
                // Print the server's response
                System.out.println(">> RESPONSE: " + in.readLine());
            }
            socket.close();
        } catch (Exception e) {
            System.out.println("\n[!] Connection Failed!");
            System.out.println("Check that your username is correct ('" + user + ".jks' exists) and your password is valid.");
        } finally {
            console.close();
        }
    }

    // Helper method to display available actions clearly during the demo
    private static void printMenu() {
        System.out.println("\n--- Available Commands ---");
        System.out.println(" LIST                                              (View readable record IDs)");
        System.out.println(" READ <Record ID>                                  (View record details)");
        System.out.println(" WRITE <Record ID> <New Data>                      (Append data to a record)");
        System.out.println(" CREATE <Patient Name> <Nurse Name> <Medical Data> (Doctors only)");
        System.out.println(" DELETE <Record ID>                                (Agencies only)");
        System.out.println(" HELP                                              (Shows this menu again)");
        System.out.println(" EXIT                                              (Disconnect)");
        System.out.println("--------------------------");
    }
}