#!/bin/bash

# Compile first to ensure we are testing the latest code
echo "--- Compiling Java Code ---"
javac *.java

# Function to run a test scenario
# Usage: run_test "UserIdentity" "List of Commands"
run_test() {
    USER_ID=$1
    COMMANDS=$2
    echo "---------------------------------------------------------"
    echo ">>> Testing User: $USER_ID"
    echo "---------------------------------------------------------"
    # pipe the commands into the java client
    echo -e "$COMMANDS" | java HospitalClient $USER_ID
    echo ""
    sleep 1 # pause briefly to let the server log write
}

# --- SCENARIO 1: DOCTOR (DrBob) ---
# Goal: Prove he can Read/Write/Create, but CANNOT Delete.
# Commands:
# 1. READ 101 (Success)
# 2. WRITE 101 (Success)
# 3. CREATE (Success)
# 4. DELETE 101 (FAIL - Critical Security Check)
CMDS_DOCTOR="READ 101\nWRITE 101 DrBob_Updated_This\nCREATE NurseEve Radiology Bob BrokenLeg\nDELETE 101\nEXIT"
run_test "doctor_DrBob" "$CMDS_DOCTOR"

# --- SCENARIO 2: NURSE (NurseEve) ---
# Goal: Prove she can Read/Write (treating), but CANNOT Delete or Create.
# Commands:
# 1. READ 101 (Success - Treating Nurse)
# 2. WRITE 101 (Success - Treating Nurse)
# 3. CREATE (FAIL - Nurses can't create)
# 4. DELETE 101 (FAIL - Nurses can't delete)
CMDS_NURSE="READ 101\nWRITE 101 NurseEve_Updated_This\nCREATE DrBob Radiology Bob Flu\nDELETE 101\nEXIT"
run_test "nurse_NurseEve" "$CMDS_NURSE"

# --- SCENARIO 3: PATIENT (Alice) ---
# Goal: Prove she can only read HER OWN record.
# Commands:
# 1. READ 101 (Success - Her record)
# 2. READ 102 (FAIL - Charlie's record)
# 3. DELETE 101 (FAIL)
CMDS_PATIENT="READ 101\nREAD 102\nDELETE 101\nEXIT"
run_test "patient_Alice" "$CMDS_PATIENT"

# --- SCENARIO 4: AGENCY (GovOrg) ---
# Goal: Prove they can DELETE records.
# Commands:
# 1. DELETE 101 (Success - Finally deleting the record)
CMDS_AGENCY="DELETE 101\nEXIT"
run_test "agency_GovOrg" "$CMDS_AGENCY"

echo "========================================================="
echo "       TESTING COMPLETE - DISPLAYING AUDIT LOG           "
echo "========================================================="
# Print the last 20 lines of the log file so you can see the results immediately
tail -n 20 audit_log.txt