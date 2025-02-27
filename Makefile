.PHONY: client server
clean:
	rm -f server/test2.txt
	rm -f client/test1.txt
client:
	make clean
	javac client/client.java
	java -cp client/ client 127.0.0.1 1 2
server:
	make clean
	javac server/server.java
	java -cp server/ server 1 2
