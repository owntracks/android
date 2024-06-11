mosquitto_pub -h localhost -p 1883 -t owntracks/test/emu64xa/cmd -m '{"_type":"cmd", "action":"clearWaypoints"}' -P test -u test -i cli-pub
