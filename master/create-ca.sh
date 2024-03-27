#! /bin/bash

mkdir test

openssl req \
	-new \
	-x509 \
	-nodes \
	-days 365 \
	-subj "$1" \
	-keyout test/ca-key.pem \
	-out test/ca-cert.pem

