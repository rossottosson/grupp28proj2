import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.time.LocalDateTime;

// Handles the regulatory requirement to keep an audit log of all access attempts.
public class AuditLogger {
    private static final String LOG_FILE = "audit_log.txt";

    // The synchronized keyword is critical here. 
    // Because our server uses multi-threading (handling multiple clients at once),
    // we must ensure two threads don't try to write to the file at the exact same millisecond and corrupt the log.
    public synchronized void log(String user, String operation, String target, boolean success) {
        try (PrintWriter out = new PrintWriter(new FileWriter(LOG_FILE, true))) {
            String timestamp = LocalDateTime.now().toString();
            String status = success ? "ALLOWED" : "DENIED";
            String entry = String.format("[%s] User: %s | Action: %s | Target: %s | Result: %s", 
                                         timestamp, user, operation, target, status);
            out.println(entry);
            System.out.println("SERVER LOG: " + entry);
        } catch (IOException e) {
            System.err.println("CRITICAL ERROR: Could not write to audit log.");
        }
    }
}