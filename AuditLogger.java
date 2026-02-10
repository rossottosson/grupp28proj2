

import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.time.LocalDateTime;

public class AuditLogger {
    private static final String LOG_FILE = "audit_log.txt";

    public synchronized void log(String user, String operation, String recordId, boolean success) {
        try (PrintWriter out = new PrintWriter(new FileWriter(LOG_FILE, true))) {
            String timestamp = LocalDateTime.now().toString();
            String status = success ? "ALLOWED" : "DENIED";
            String entry = String.format("[%s] User: %s | Action: %s | Record: %s | Result: %s", 
                                         timestamp, user, operation, recordId, status);
            out.println(entry);
            System.out.println("LOGGED: " + entry); // Print to console for debug
        } catch (IOException e) {
            System.err.println("CRITICAL ERROR: Could not write to audit log!");
            e.printStackTrace();
        }
    }
}
