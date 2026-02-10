#!/bin/bash

# PASSWORD for everything (for simplicity in this project)
PW="password"

# Cleanup old files
rm *.jks *.crt *.csr *.key *.srl

echo "--- 1. Generate CA (Certificate Authority) ---"
# We use OpenSSL to create a self-signed CA certificate
openssl req -new -x509 -keyout ca-key.pem -out ca-cert.pem -days 365 -nodes -subj "/CN=HospitalCA"

echo "--- 2. Create Truststores ---"
# Server needs to trust clients signed by CA
keytool -import -alias CA -file ca-cert.pem -keystore servertruststore.jks -storepass $PW -noprompt
# Clients need to trust the Server signed by CA
keytool -import -alias CA -file ca-cert.pem -keystore clienttruststore.jks -storepass $PW -noprompt

echo "--- 3. Generate SERVER Keystore & Certificate ---"
# Generate Keypair
keytool -genkeypair -alias server -keyalg RSA -keysize 2048 -keystore server.jks -dname "CN=localhost, OU=Hospital, O=LundUniversity, L=Lund, S=Skane, C=SE" -storepass $PW -keypass $PW
# Generate CSR
keytool -certreq -alias server -keystore server.jks -file server.csr -storepass $PW
# Sign with CA (OpenSSL)
openssl x509 -req -in server.csr -CA ca-cert.pem -CAkey ca-key.pem -CAcreateserial -out server-signed.crt -days 365
# Import CA to keystore (Chain of trust)
keytool -import -alias CA -file ca-cert.pem -keystore server.jks -storepass $PW -noprompt
# Import Signed Cert
keytool -import -alias server -file server-signed.crt -keystore server.jks -storepass $PW -noprompt


# Function to generate Client Certificates
generate_client() {
    ROLE=$1
    NAME=$2
    FILENAME="${ROLE}_${NAME}" # e.g. doctor_DrBob
    DN="CN=${FILENAME}, OU=${ROLE}, O=Hospital, L=Lund, C=SE"

    echo "--- Generating Client: $FILENAME ---"
    
    # Generate Keypair
    keytool -genkeypair -alias $FILENAME -keyalg RSA -keysize 2048 -keystore $FILENAME.jks -dname "$DN" -storepass $PW -keypass $PW
    # Generate CSR
    keytool -certreq -alias $FILENAME -keystore $FILENAME.jks -file $FILENAME.csr -storepass $PW
    # Sign with CA
    openssl x509 -req -in $FILENAME.csr -CA ca-cert.pem -CAkey ca-key.pem -out $FILENAME-signed.crt -days 365
    # Import CA
    keytool -import -alias CA -file ca-cert.pem -keystore $FILENAME.jks -storepass $PW -noprompt
    # Import Signed Cert
    keytool -import -alias $FILENAME -file $FILENAME-signed.crt -keystore $FILENAME.jks -storepass $PW -noprompt
}

echo "--- 4. Generate Users ---"
# Create the specific users our Java code expects
generate_client "doctor" "DrBob"
generate_client "nurse" "NurseEve"
generate_client "patient" "Alice"
generate_client "agency" "GovOrg"

echo "--- DONE! Keys are ready. ---"