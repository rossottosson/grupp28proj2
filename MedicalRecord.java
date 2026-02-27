import java.io.Serializable;


// Represents a single medical record in the hospital's database.
// Implements Serializable in case we ever want to save the database to a file or send objects over the network.
public class MedicalRecord implements Serializable {
    private static final long serialVersionUID = 1L;
    
    private String id;

    // These fields are crucial for our Role-Based Access Control (RBAC).
    // The Reference Monitor uses them to determine who is the treating doctor/nurse,
    // and which division the record belongs to.
    private String patientName;
    private String doctorName;
    private String nurseName;
    private String division;
    private String medicalData;

    public MedicalRecord(String id, String patientName, String doctorName, String nurseName, String division, String medicalData) {
        this.id = id;
        this.patientName = patientName;
        this.doctorName = doctorName;
        this.nurseName = nurseName;
        this.division = division;
        this.medicalData = medicalData;
    }

    //Getters used by the HospitalServer's hasAccess() method
    public String getId() { return id; }
    public String getPatientName() { return patientName; }
    public String getDoctorName() { return doctorName; }
    public String getNurseName() { return nurseName; }
    public String getDivision() { return division; }
    public String getMedicalData() { return medicalData; }
    
    // Only the medical data can be modified after creation
    public void setMedicalData(String data) { this.medicalData = data; }

    @Override
    public String toString() {
        return "Record[" + id + "]: Patient=" + patientName + ", Doc=" + doctorName + 
               ", Nurse=" + nurseName + ", Div=" + division + ", Data=" + medicalData;
    }
}