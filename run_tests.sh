#!/bin/bash

# Recompile to ensure we are running the latest version of the code
javac *.java

# Helper function to pipe a list of commands into the Java client
run_test() {
    USER_ID=$1
    COMMANDS=$2
    echo ">>> Testing User: $USER_ID"
    # The echo -e allows us to pass newlines (\n) to simulate pressing "Enter"
    echo -e "$COMMANDS" | java HospitalClient $USER_ID
    echo "---------------------------------------------------------"
    # Pause briefly to allow the server's AuditLogger thread to finish writing
    sleep 1
}

# SCENARIO 1: DOCTOR
# DrBob treats Alice (101) and Charlie (102) in Cardiology.
# Proves: Read/Write access for treating doctor, Create access for existing patient, blocks cross-division reading (103), blocks Delete.
CMDS_DOCTOR="READ 101\nWRITE 101 DrBob_Updated_This\nCREATE Alice NurseEve New_Xray_Data\nREAD 103\nDELETE 101\nEXIT"
run_test "doctor_DrBob" "$CMDS_DOCTOR"

# SCENARIO 2: NURSE
# NurseEve is in Cardiology and treats Alice (101). 
# Proves: Read/Write for treating nurse, blocks Create (only doctors can), blocks Delete.
CMDS_NURSE="READ 101\nWRITE 101 NurseEve_Updated_This\nCREATE DrBob NurseEve Flu\nDELETE 101\nEXIT"
run_test "nurse_NurseEve" "$CMDS_NURSE"

# SCENARIO 3: PATIENT
# Alice is a patient.
# Proves: The LIST command works, she can read her own record (101), is blocked from others (102), and blocked from Delete.
CMDS_PATIENT="LIST\nREAD 101\nREAD 102\nDELETE 101\nEXIT"
run_test "patient_Alice" "$CMDS_PATIENT"

# SCENARIO 4: GOVERNMENT AGENCY
# GovOrg is the agency.
# Proves: They have the ultimate authority to delete records.
CMDS_AGENCY="DELETE 101\nEXIT"
run_test "agency_GovOrg" "$CMDS_AGENCY"

echo "--- AUDIT LOG ---"
# Display the bottom of the log to prove all ALLOWED and DENIED actions were recorded
tail -n 15 audit_log.txt