#! /bin/bash

openssl req \
	-new \
	-x509 \
	-nodes \
	-days 365 \
	-subj "$1" \
	-keyout $2/ca-key.pem \
	-out $2/ca-cert.pem

