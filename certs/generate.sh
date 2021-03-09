#!/bin/sh

set -Eeuo pipefail

# Request a certificate.
certbot certonly --manual --preferred-challenges dns --cert-name dev --config-dir certs/certbot/etc --work-dir certs/certbot/work --logs-dir certs/certbot/log

# Copy the raw cert and privkey (for use in the HTTPS service itself).
cp -f certs/certbot/etc/live/dev/fullchain.pem certs/dev-fullchain.pem
cp -f certs/certbot/etc/live/dev/privkey.pem certs/dev-privkey.pem

# Convert the cert/key to a Java Keystore (for use with the Shadow CLJS
# WebSocket).
openssl pkcs12 -export -in certs/dev-fullchain.pem -inkey certs/dev-privkey.pem -out certs/dev.pkcs12 -passout pass:password
keytool -importkeystore -deststorepass password -destkeypass password -destkeystore certs/dev-keystore.jks -srckeystore certs/dev.pkcs12 -srcstoretype PKCS12 -srcstorepass password
rm certs/dev.pkcs12
