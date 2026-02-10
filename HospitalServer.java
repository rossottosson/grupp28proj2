import javax.net.ssl.*;
import java.io.*;
import java.security.cert.X509Certificate;
import java.util.HashMap;
import java.util.Map;

public class HospitalServer {
    private static final int PORT = 9876;
    private static Map<String, MedicalRecord> database = new HashMap<>();
    private static AuditLogger logger = new AuditLogger();

    public static void main(String[] args) {
        // 1. Setup Data
        database.put("101", new MedicalRecord("101", "Alice", "DrBob", "NurseEve", "Cardiology", "Heart looks good"));
        database.put("102", new MedicalRecord("102", "Charlie", "DrBob", "NurseEve", "Cardiology", "High blood pressure"));
        database.put("103", new MedicalRecord("103", "Alice", "DrWho", "NurseJoy", "Radiology", "X-Ray negative"));

        // 2. Setup TLS
        System.setProperty("javax.net.ssl.keyStore", "server.jks");
        System.setProperty("javax.net.ssl.keyStorePassword", "password");
        System.setProperty("javax.net.ssl.trustStore", "servertruststore.jks");
        System.setProperty("javax.net.ssl.trustStorePassword", "password");

        try {
            SSLServerSocketFactory ssf = (SSLServerSocketFactory) SSLServerSocketFactory.getDefault();
            SSLServerSocket ssocket = (SSLServerSocket) ssf.createServerSocket(PORT);
            ssocket.setNeedClientAuth(true); // Mutual Auth

            System.out.println("Hospital Server Started on port " + PORT);

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
                // Identify User
                SSLSession session = socket.getSession();
                X509Certificate cert = (X509Certificate) session.getPeerCertificates()[0];
                String dn = cert.getSubjectX500Principal().getName();
                parseIdentity(dn);

                out.println("Welcome " + userName + " (" + userRole + "). Connected securely.");

                String inputLine;
                while ((inputLine = in.readLine()) != null) {
                    String[] parts = inputLine.split(" ", 3);
                    String action = parts[0];
                    if (action.equalsIgnoreCase("EXIT")) break;

                    String response = handleRequest(action, parts);
                    out.println(response);
                }
            } catch (Exception e) {
                System.out.println("Client disconnected: " + e.getMessage());
            }
        }

        // ROBUST PARSER: Finds "CN=" anywhere in the string
        private void parseIdentity(String dn) {
            String cn = "";
            // Split by comma to handle standard DN format: "CN=doctor_DrBob, OU=..."
            String[] fields = dn.split(",");
            for (String field : fields) {
                if (field.trim().startsWith("CN=")) {
                    cn = field.trim().substring(3); // Remove "CN="
                    break;
                }
            }
            
            // Now parse the Role_Name format
            String[] split = cn.split("_");
            if (split.length >= 2) {
                userRole = split[0];
                userName = split[1];
            } else {
                userName = cn;
            }
        }

        private String handleRequest(String action, String[] parts) {
            if (parts.length < 2 && !action.equalsIgnoreCase("LIST")) return "ERROR: Missing arguments";
            
            String recordId = (parts.length > 1) ? parts[1] : "";
            String data = (parts.length > 2) ? parts[2] : "";

            if (!hasAccess(action, recordId)) {
                logger.log(userName, action, recordId, false);
                return "DENIED: You do not have permission.";
            }

            logger.log(userName, action, recordId, true);
            
            // Execute Action
            switch (action.toUpperCase()) {
                case "READ":   return database.get(recordId).toString();
                case "DELETE": 
                    database.remove(recordId); 
                    return "SUCCESS: Record deleted.";
                case "WRITE":
                    MedicalRecord r = database.get(recordId);
                    if (r != null) { r.setMedicalData(data); return "SUCCESS: Record updated."; }
                    return "ERROR: Not found.";
                case "CREATE":
                    String newId = String.valueOf(System.currentTimeMillis());
                    // Create: ID, Patient, Doctor(User), Nurse(Arg1), Div(Arg2), Data(Arg3)
                    MedicalRecord newRec = new MedicalRecord(newId, "NewPatient", userName, parts[1], "Div", data);
                    database.put(newId, newRec);
                    return "SUCCESS: Created record " + newId;
                default: return "ERROR: Unknown command";
            }
        }

        
        private boolean hasAccess(String action, String recordId) {
            MedicalRecord record = database.get(recordId);

            // Special case: Create is only for doctors
            if (action.equalsIgnoreCase("CREATE")) return userRole.equalsIgnoreCase("doctor");

            if (record == null) return false;

            switch (userRole.toLowerCase()) {
                case "agency":
                
                    return action.equalsIgnoreCase("READ") || action.equalsIgnoreCase("DELETE");

                case "doctor":
                    // 1. Never allow DELETE
                    if (action.equalsIgnoreCase("DELETE")) return false;
                    
           
                    if (record.getDoctorName().equals(userName)) return true; // (Write is implied allowed because Delete is blocked above)

   
                    if (action.equalsIgnoreCase("READ")) return true;
                    
                    return false;

                case "nurse":
                    // 1. Never allow DELETE (Fixing the security hole!)
                    if (action.equalsIgnoreCase("DELETE")) return false;
                    
                 
                    if (record.getNurseName().equals(userName)) return true;

                    
                    if (action.equalsIgnoreCase("READ")) return true;

                    return false;

                case "patient":
               
                    return action.equalsIgnoreCase("READ") && record.getPatientName().equals(userName);

                default:
                    return false;
            }
        }
    }
}