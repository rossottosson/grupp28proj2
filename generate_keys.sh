#!/bin/bash

# Hardcoded password for the sake of the project demo
PW="password"

# Clean up any old keys from previous runs so we start fresh
rm *.jks *.crt *.csr *.key *.srl ca-cert.pem ca-key.pem 2>/dev/null

echo "--- Generating Keys ---"

# 1. CREATE THE CERTIFICATE AUTHORITY (CA)
# This acts as our trusted third party. It will sign all other certificates.
openssl req -new -x509 -keyout ca-key.pem -out ca-cert.pem -days 365 -nodes -subj "/CN=HospitalCA"

# 2. CREATE TRUSTSTORES
# Truststores contain the certificates of entities we trust. 
# Both the server and clients need to trust our CA to verify each other's signed certificates.
keytool -import -alias CA -file ca-cert.pem -keystore servertruststore.jks -storepass $PW -noprompt
keytool -import -alias CA -file ca-cert.pem -keystore clienttruststore.jks -storepass $PW -noprompt

# 3. GENERATE SERVER KEYSTORE
# Generate the server's keypair, create a Certificate Signing Request (CSR), and have the CA sign it.
keytool -genkeypair -alias server -keyalg RSA -keysize 2048 -keystore server.jks -dname "CN=localhost, OU=Hospital, O=LundUniversity, L=Lund, S=Skane, C=SE" -storepass $PW -keypass $PW
keytool -certreq -alias server -keystore server.jks -file server.csr -storepass $PW
openssl x509 -req -in server.csr -CA ca-cert.pem -CAkey ca-key.pem -CAcreateserial -out server-signed.crt -days 365

# Import the chain of trust: First the CA, then the signed server cert
keytool -import -alias CA -file ca-cert.pem -keystore server.jks -storepass $PW -noprompt
keytool -import -alias server -file server-signed.crt -keystore server.jks -storepass $PW -noprompt

# 4. GENERATE CLIENT KEYSTORES (Our Two-Factor Auth mechanism)
# We embed the role and name into the Common Name (CN) so the server can parse it upon connection.
generate_client() {
    ROLE=$1
    NAME=$2
    FILENAME="${ROLE}_${NAME}"
    DN="CN=${FILENAME}, OU=${ROLE}, O=Hospital, L=Lund, C=SE"

    keytool -genkeypair -alias $FILENAME -keyalg RSA -keysize 2048 -keystore $FILENAME.jks -dname "$DN" -storepass $PW -keypass $PW
    keytool -certreq -alias $FILENAME -keystore $FILENAME.jks -file $FILENAME.csr -storepass $PW
    openssl x509 -req -in $FILENAME.csr -CA ca-cert.pem -CAkey ca-key.pem -out $FILENAME-signed.crt -days 365
    
    keytool -import -alias CA -file ca-cert.pem -keystore $FILENAME.jks -storepass $PW -noprompt
    keytool -import -alias $FILENAME -file $FILENAME-signed.crt -keystore $FILENAME.jks -storepass $PW -noprompt
}

# Generate the specific users needed for our test scenarios
generate_client "doctor" "DrBob"
generate_client "nurse" "NurseEve"
generate_client "patient" "Alice"
generate_client "agency" "GovOrg"

echo "--- Keys Generated ---"