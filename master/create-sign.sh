#! /bin/bash





openssl genrsa -out $2/key.pem 2048

openssl req -new -key $2/key.pem -subj "$1" -out $2/temp.csr

openssl x509 -req -in $2/temp.csr -CA $3/ca-cert.pem -CAkey $3/ca-key.pem -CAcreateserial -days 365 -out $2/cert.pem

rm -rf $2/temp.csr
