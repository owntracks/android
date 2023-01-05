#!/bin/bash

 docker run --rm --name mosquitto \
	 -p 1883:1883 \
	 -p 8883:8883 \
	 -p 8080:8080 \
	 -v $(pwd)/mosquitto.conf:/mosquitto/config/mosquitto.conf:ro \
	 -v $(pwd)/mosquitto.password:/mosquitto/config/mosquitto.password:ro \
	 -v $(pwd)/rootCA.pem:/mosquitto/config/rootCA.pem:ro \
	 -v $(pwd)/_wildcard.docker.local-key.pem:/mosquitto/config/key.pem:ro \
	 -v $(pwd)/_wildcard.docker.local.pem:/mosquitto/config/cert.pem:ro \
	 eclipse-mosquitto:2.0.15-openssl
