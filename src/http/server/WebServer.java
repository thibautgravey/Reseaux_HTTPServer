///A Simple Web Server (WebServer.java)

package http.server;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

/**
 * Example program from Chapter 1 Programming Spiders, Bots and Aggregators in
 * Java Copyright 2001 by Jeff Heaton
 *
 * WebServer is a very simple web-server. Any request is responded with a very
 * simple web-page.
 *
 * @author Jeff Heaton
 * @version 1.0
 */
public class WebServer {

  /**
   * WebServer constructor.
   */
  protected void start() {
    ServerSocket s;

    System.out.println("Webserver starting up on port 3000");
    System.out.println("(press ctrl-c to exit)");
    try {
      // create the main server socket
      s = new ServerSocket(3000);
    } catch (Exception e) {
      System.out.println("Error: " + e);
      return;
    }

    System.out.println("Waiting for connection");
    for (;;) {
      try {
        // wait for a connection
        Socket remote = s.accept();
        // remote is now the connected socket
        handleClientRequest(remote);
        remote.close();
      } catch (Exception e) {
        System.out.println("Error: " + e);
      }
    }
  }

  private void handleClientRequest(Socket client) throws IOException {
    System.out.println("Connection, sending data.");
    BufferedReader in = new BufferedReader(new InputStreamReader(
            client.getInputStream()));

    // read the data sent.
    // stop reading once a blank line is hit. This
    // blank line signals the end of the client HTTP
    // headers.
    String line = ".";
    StringBuilder stringBuilder = new StringBuilder();
    while (line != null && !line.equals("")) {
      line = in.readLine();
      stringBuilder.append(line).append("\n");
    }

    //Handle the request
    String request = stringBuilder.toString();
    System.out.println("Request :\n"+request);

    //Parse the request
    String[] linesFromRequest = request.split("\n");
    String[] singleLineRequest = linesFromRequest[0].split(" ");
    String method = singleLineRequest[0];
    System.out.println("Method : "+method);
    String path = singleLineRequest[1];
    System.out.println("Path : "+path);
    String version = singleLineRequest[2];
    System.out.println("Version : "+version);
    String host = linesFromRequest[1].split(" ")[1];
    System.out.println("host : "+host);

    List<String> headers = new ArrayList<>();
    System.out.println("Header : ");
    for (int h = 2; h < linesFromRequest.length; h++) {
      String header = linesFromRequest[h];
      headers.add(header);
      System.out.println(header);
    }
    System.out.println("---Fin Header---");

    switch(method){
      case "GET" :
        handleGET(client,path);
        break;
    }
  }

  private static void handleGET(Socket client, String path) throws IOException {
      Path filePath = getFilePath(path);
      if (Files.exists(filePath)) { // if the file exist
        String contentType = guessContentType(filePath);
        sendResponse(client, "200 OK", contentType, Files.readAllBytes(filePath));
      } else { // Error 404 not found
        byte[] contentNotFound = "<h1>Not found :(</h1>".getBytes();
        sendResponse(client, "404 Not Found", "text/html", contentNotFound);
      }
  }

  private static void sendResponse(Socket client, String status, String contentType, byte[] content) throws IOException {
    PrintWriter out = new PrintWriter(client.getOutputStream());
    out.println(("HTTP/1.1 \r\n" + status).getBytes());
    out.println(("ContentType: " + contentType + "\r\n").getBytes());
    out.println("\r\n".getBytes());
    out.println(content);
    out.println("\r\n\r\n".getBytes());
    out.flush();
  }

  private static String guessContentType(Path filePath) throws IOException {
    return Files.probeContentType(filePath);
  }

  private static Path getFilePath(String path) {
    if ("/".equals(path)) {
      path = "/index.html";
    }
    return Paths.get("/doc", path);
  }

  /**
   * Start the application.
   *
   * @param args
   *            Command line parameters are not used.
   */
  public static void main(String args[]) {
    WebServer ws = new WebServer();
    ws.start();
  }
}
