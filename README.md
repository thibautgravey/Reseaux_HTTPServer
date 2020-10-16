# Reseaux_HTTPServer
Building HTTP servers with Java Synchronous network communication systems at INSA Lyon (4th) 

to compile a java file that depend on another: e.g EchoServerMultiThread is using ClientThread, you need to specify the classes folder with -d, and compile them both at once. ex: javac -d ./classes src/stream/ClientThread.java src/stream/EchoServerMultiThreaded.java

to run a java file that doesn't have its class file in the same folder, you need to specify the class path (don't forget to specify the package name too) ex: sudo java -cp ../../classes server.EchoServerMultiThreaded 80 (here, the package name is stream, the file name EchoServerMultiThread) (don't know why, but if it doesn't work, i.e java.lang.ClassNotFoundException: EchoClient, try: java EchoClient.java) java -cp ../../classes client.EchoClient 127.0.0.1 80
