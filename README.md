# Secure Hospital System (Group 28)

This is a secure, client-server medical record system implementing Mutual TLS, Two-Factor Authentication, and Role-Based Access Control.

## Prerequisites
* Java (JDK 11 or higher)
* `openssl` and `keytool` 

## Quick Start Guide

1. Generate the Certificates and Keystores
Before compiling or running the code, you must generate the CA, certificates, and keystores. Run the included bash script:
```bash
bash generate_keys.sh

(Note: The password for all generated keystores is password)

2. Compile the Source Code

Bash
javac *.java

3. Start the Server
In your first terminal window, start the central server (runs on port 9876):
Bash
java HospitalServer

4. Start the Client
In a second terminal window, launch the interactive client:
Bash
java HospitalClient

Available Demo Users (Password for all is password):

doctor_DrBob

nurse_NurseEve

patient_Alice

agency_GovOrg