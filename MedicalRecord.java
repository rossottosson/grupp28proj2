
import java.io.Serializable;

public class MedicalRecord implements Serializable {
    private static final long serialVersionUID = 1L;
    
    private String id;
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

    // Getters and Setters
    public String getId() { return id; }
    public String getPatientName() { return patientName; }
    public String getDoctorName() { return doctorName; }
    public String getNurseName() { return nurseName; }
    public String getDivision() { return division; }
    public String getMedicalData() { return medicalData; }
    
    public void setMedicalData(String data) { this.medicalData = data; }

    @Override
    public String toString() {
        return "Record[" + id + "]: Patient=" + patientName + ", Doc=" + doctorName + 
               ", Nurse=" + nurseName + ", Div=" + division + ", Data=" + medicalData;
    }
}