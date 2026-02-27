import javax.net.ssl.*;
import java.io.*;
import java.security.cert.X509Certificate;
import java.util.HashMap;
import java.util.Map;

public class HospitalServer {

    private static final int PORT = 9876;

    // In-memory database and division mapping for the demo
    private static Map<String, MedicalRecord> database = new HashMap<>();
    protected static Map<String, String> userDivisions = new HashMap<>();
    private static AuditLogger logger = new AuditLogger();

    public static void main(String[] args) {
        // Populate dummy records to test our access control rules
        database.put("101", new MedicalRecord("101", "Alice", "DrBob", "NurseEve", "Cardiology", "Heart looks good"));
        database.put("102", new MedicalRecord("102", "Charlie", "DrBob", "NurseEve", "Cardiology", "High blood pressure"));
        database.put("103", new MedicalRecord("103", "Alice", "DrWho", "NurseJoy", "Radiology", "X-Ray negative"));

        // Map hospital staff to their respective divisions
        userDivisions.put("DrBob", "Cardiology");
        userDivisions.put("NurseEve", "Cardiology");
        userDivisions.put("DrWho", "Radiology");
        userDivisions.put("NurseJoy", "Radiology");

        // Set up the server's keystore and truststore for TLS
        System.setProperty("javax.net.ssl.keyStore", "server.jks");
        System.setProperty("javax.net.ssl.keyStorePassword", "password");
        System.setProperty("javax.net.ssl.trustStore", "servertruststore.jks");
        System.setProperty("javax.net.ssl.trustStorePassword", "password");

        try {
            SSLServerSocketFactory ssf = (SSLServerSocketFactory) SSLServerSocketFactory.getDefault();
            SSLServerSocket ssocket = (SSLServerSocket) ssf.createServerSocket(PORT);

            // Force mutual authentication. Clients MUST provide a valid certificate
            ssocket.setNeedClientAuth(true);

            System.out.println("Hospital Server Started on port " + PORT);
            
            // Listen for incoming client connections and handle them in new threads
            while (true) {
                SSLSocket socket = (SSLSocket) ssocket.accept();
                new Thread(new ClientHandler(socket)).start();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static class ClientHandler implements Runnable {
        private SSLSocket socket;
        private String userRole = "unknown";
        private String userName = "unknown";

        public ClientHandler(SSLSocket socket) {
            this.socket = socket;
        }

        @Override
        public void run() {
            try (
                PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
                BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()))
            ) {
                // Extract the client's certificate to figure out who logged in
                SSLSession session = socket.getSession();
                X509Certificate cert = (X509Certificate) session.getPeerCertificates()[0];
                String dn = cert.getSubjectX500Principal().getName();
                parseIdentity(dn);

                out.println("Welcome " + userName + " (" + userRole + "). Connected securely.");

                // Process client commands
                String inputLine;
                while ((inputLine = in.readLine()) != null) {
                    if (inputLine.trim().equalsIgnoreCase("EXIT")) break;
                    String response = handleRequest(inputLine);
                    out.println(response);
                }
            } catch (Exception e) {
                System.out.println("Client disconnected.");
            }
        }

        // Helper to extract the Role and Name from the certificate's CN string (like CN=doctor_DrBob)
        private void parseIdentity(String dn) {
            String cn = "";
            String[] fields = dn.split(",");
            for (String field : fields) {
                if (field.trim().startsWith("CN=")) {
                    cn = field.trim().substring(3);
                    break;
                }
            }
            
            String[] split = cn.split("_");
            if (split.length >= 2) {
                userRole = split[0];
                userName = split[1];
            } else {
                userName = cn;
            }
        }

        // Parses the raw input string and executes the command if permitted
        private String handleRequest(String inputLine) {
            String[] parts = inputLine.split(" ", 4);
            String actionName = parts[0].toUpperCase();

            // Handle the LIST command separately since it doesn't target a specific record ID
            if (actionName.equals("LIST")) {
                logger.log(userName, "LIST", "ALL", true);
                StringBuilder sb = new StringBuilder("Your readable records: ");
                boolean foundAny = false;
                for (String id : database.keySet()) {
                    if (hasAccess("READ", id)) { 
                        sb.append(id).append(" ");
                        foundAny = true;
                    }
                }
                return foundAny ? sb.toString() : "You have no readable records.";
            }

            // Basic argument validation
            if (parts.length < 2) {
                return "ERROR: Missing target argument (Record ID or Patient Name).";
            }
            if (actionName.equals("CREATE") && parts.length < 4) {
                return "ERROR: Format is CREATE <PatientName> <NurseName> <Data>";
            }
            
            String target = parts[1]; 
            
            // Reference Monitor: Check if the action is allowed BEFORE doing anything
            if (!hasAccess(actionName, target)) {
                logger.log(userName, actionName, target, false); // Log denials
                return "DENIED: You do not have permission.";
            }

            logger.log(userName, actionName, target, true); // Log allowed actions
            
            // Execute the requested action
            switch (actionName) {
                case "READ":   
                    return database.get(target).toString();
                case "DELETE": 
                    database.remove(target); 
                    return "SUCCESS: Record deleted.";
                case "WRITE":
                    MedicalRecord r = database.get(target);
                    if (r != null) { 
                        String writeData = "";
                        if (parts.length > 2) writeData += parts[2];
                        if (parts.length > 3) writeData += " " + parts[3];
                        r.setMedicalData(writeData.trim()); 
                        return "SUCCESS: Record updated."; 
                    }
                    return "ERROR: Not found.";
                case "CREATE":
                    String nurseName = parts[2];
                    String createData = parts[3];
                    String newId = String.valueOf(System.currentTimeMillis());
                    String division = HospitalServer.userDivisions.getOrDefault(userName, "Unknown");
                    
                    MedicalRecord newRec = new MedicalRecord(newId, target, userName, nurseName, division, createData);
                    database.put(newId, newRec);
                    return "SUCCESS: Created record " + newId + " for patient " + target;
                default: 
                    return "ERROR: Unknown command";
            }
        }
        
        // This acts as our Reference Monitor, enforcing the strict access control rules
        private boolean hasAccess(String action, String argument) {
            // Rule: Doctors can only create records if they already treat the patient
            if (action.equalsIgnoreCase("CREATE")) {
                if (!userRole.equalsIgnoreCase("doctor")) return false;
                
                boolean isTreating = false;
                for (MedicalRecord r : database.values()) {
                    if (r.getPatientName().equalsIgnoreCase(argument) && r.getDoctorName().equals(userName)) {
                        isTreating = true;
                        break;
                    }
                }
                return isTreating;
            }

            MedicalRecord record = database.get(argument);
            if (record == null) return false;

            String myDivision = HospitalServer.userDivisions.get(userName);

            // Role-Based Access Control logic
            switch (userRole.toLowerCase()) {
                case "agency":
                    // Agencies can read and delete everything, but cannot write or create
                    return action.equalsIgnoreCase("READ") || action.equalsIgnoreCase("DELETE");
                case "doctor":
                    if (action.equalsIgnoreCase("DELETE")) return false;
                    // Full access if treating the patient
                    if (record.getDoctorName().equals(userName)) return true; 
                    // Read-only access if in the same division
                    if (action.equalsIgnoreCase("READ") && record.getDivision().equals(myDivision)) return true;
                    return false;
                case "nurse":
                    if (action.equalsIgnoreCase("DELETE") || action.equalsIgnoreCase("CREATE")) return false;
                    // Full access if treating the patient
                    if (record.getNurseName().equals(userName)) return true;
                    // Read-only access if in the same division
                    if (action.equalsIgnoreCase("READ") && record.getDivision().equals(myDivision)) return true; 
                    return false;
                case "patient":
                    // Patients can only read their own records
                    return action.equalsIgnoreCase("READ") && record.getPatientName().equals(userName);
                default:
                    // Default deny for unknown roles
                    return false;
            }
        }
    }
}