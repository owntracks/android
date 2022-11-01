#!/usr/bin/bash

mkcert -client -pkcs12 test
mkcert 10.0.2.2
chmod 444 *.pem
