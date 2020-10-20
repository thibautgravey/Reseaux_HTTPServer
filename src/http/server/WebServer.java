///A Simple Web Server (WebServer.java)

package http.server;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Date;
import java.util.HashMap;
import java.util.List;

public class WebServer {

  enum HeaderType {
    ERROR,
    GET,
    HEAD,
    POST
  }

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
        System.out.println("Error here : " + e);
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
     // System.out.println("Request :\n"+request);

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
    System.out.println("-----Header-----");
    String headerLength = "0";
    for (int h = 2; h < linesFromRequest.length; h++) {
      String header = linesFromRequest[h];
      headers.add(header);
      System.out.println(header);
      if(header.startsWith("Content-Length:")){
         headerLength = header.substring(16);
      }
    }
    System.out.println("---Fin Header---");

    Integer bufferLength = Integer.parseInt(headerLength);
    char[] buffer = null;
    if(bufferLength > 0 ){
      buffer = new char [bufferLength];
      in.read(buffer, 0 ,bufferLength);
    }
    String body = null;
    if(buffer!=null){
       body = String.valueOf(buffer);
    }
    System.out.println("body : " + body);

    switch(method){
      case "GET" :
        handleGET(client,path);
        break;
      case "HEAD" :
        handleHEAD(client,path);
        break;
      case "POST" :
        handlePOST(client, path, body);
        break;
      default:
        sendResponse(client, StatusCode.CODE_503,null,null,HeaderType.ERROR);
    }
  }

  private void handleGET(Socket client, String path) throws IOException {
      Path filePath = getFilePath(path);
      System.out.println("FilePath after guess : "+filePath);
      if (Files.exists(filePath)) { // if the file exist
        String contentType = Files.probeContentType(filePath);
        sendResponse(client, StatusCode.CODE_200, contentType, Files.readAllBytes(filePath),HeaderType.GET);
      } else { // Error 404 not found
        byte[] contentNotFound = "<h1>404 Not found :(</h1>".getBytes(StandardCharsets.UTF_8);
        sendResponse(client, StatusCode.CODE_404, "text/html", contentNotFound,HeaderType.GET);
      }
  }

  private void handleHEAD(Socket client,String path) {
    if(path != null){
      try {
        // V�rification de l'existence de la ressource demand�e
        Path filePath = getFilePath(path);
        System.out.println(filePath);
        if(Files.exists(filePath)) {
          String contentType = Files.probeContentType(filePath);
          sendResponse(client, StatusCode.CODE_200, contentType,Files.readAllBytes(filePath),HeaderType.HEAD);
        } else {
          sendResponse(client, StatusCode.CODE_404,null,null,HeaderType.ERROR);
        }

      } catch (IOException e) {
        e.printStackTrace();
        try {
          sendResponse(client,StatusCode.CODE_500, null,null,HeaderType.ERROR);
        } catch (Exception e2) {
          e2.printStackTrace();
        };
      }
    }
  }
  
  private void handlePOST(Socket client,String path, String body) throws IOException {

    switch (path){
      case "/signup":
          signup(client,body);
          break;
      default:
          sendResponse(client, StatusCode.CODE_404,null,null,HeaderType.ERROR);
          break;
    }
  }

  private void signup(Socket client,String body) throws IOException {
    if(body != null){
      File resource = new File("./res/user.txt");
      BufferedOutputStream fileOut = new BufferedOutputStream(new FileOutputStream(resource,true));
      String[] parameters = body.split("&",10);

      for(String param : parameters){
          byte[] byteParam = (param + ";").getBytes();
          fileOut.write(byteParam, 0, byteParam.length);
      }
      byte[] endLine = ("\r\n").getBytes();
      fileOut.write(endLine, 0, endLine.length);
      fileOut.flush();
      fileOut.close();
      byte[] successfulSignup = "<script>alert('Your account has been created');window.location.href = '/index.html';</script>".getBytes(StandardCharsets.UTF_8);
      sendResponse(client, StatusCode.CODE_200,"text/html",successfulSignup,HeaderType.POST);
    }else{
      sendResponse(client, StatusCode.CODE_404,null,null,HeaderType.ERROR);
    }
  }

  private void sendResponse(Socket client, StatusCode status, String contentType, byte[] content, HeaderType type) throws IOException {
    PrintWriter out = new PrintWriter(client.getOutputStream());
    BufferedOutputStream binaryDataOutput = new BufferedOutputStream(client.getOutputStream());
    Date date = new Date();
  
    out.println("HTTP/1.1 "+status);
    out.println("Date: "+date);

    if(type.equals(HeaderType.GET) || type.equals(HeaderType.HEAD)) {
      out.println("Content-Type: " + contentType);
      out.println("Content-Encoding: UTF-8");
      out.println("Content-Length: " + content.length);
    }

    out.println("Server: Java Web Server (Unix) (H4411 B01)");
    out.println("Connection: close");
    out.println("");
    out.flush();

    if(type.equals(HeaderType.GET) || type.equals(HeaderType.POST)){
      binaryDataOutput.write(content,0,content.length);
      binaryDataOutput.flush();
    }
  }

  private Path getFilePath(String path) {
    if ("/".equals(path)) {
      path = "/index.html";
    }
    return Paths.get("./res", path);
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
