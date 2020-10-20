///A Simple Web Server (WebServer.java)

package http.server;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

public class WebServer {

  enum HeaderType {
    ERROR,
    GET,
    HEAD,
    POST,
    PUT,
    DELETE,
    OPTIONS
  }

  /**
   * WebServer constructor.
   */
  protected void start(int port) {
    ServerSocket s;

    System.out.println("Webserver starting up on port "+port);
    System.out.println("(press ctrl-c to exit)");
    try {
      // create the main server socket
      s = new ServerSocket(port);
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
        BufferedReader in = new BufferedReader(new InputStreamReader(
                remote.getInputStream()));
        System.out.println("Connection, sending data.");
        // read the data sent.
        // stop reading once a blank line is hit. This
        // blank line signals the end of the client HTTP
        // headers.
        String str = ".";
        StringBuilder stringBuilder = new StringBuilder();
        String headerLength = "0";
        while (!(str=in.readLine()).isBlank()){
            stringBuilder.append(str).append("\n");
            if(str.startsWith("Content-Length:")){
              headerLength = str.substring(16);
           }
        }

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

        String request = stringBuilder.toString();
        System.out.println("Request :\n"+request);
        System.out.println("Body :\n"+body);

        handleClientRequest(remote, request, body);

        // close the connection
        remote.close();
      } catch (Exception e) {
        System.out.println("Error line "+e.getStackTrace()[0].getLineNumber()+" : " + e);
        e.printStackTrace();
      }
    }
  }

  private void handleClientRequest(Socket client, String request,String body) {
    //Parse the request
    String[] linesFromRequest = request.split("\n");
    String[] singleLineRequest = linesFromRequest[0].split(" ");
    String method = singleLineRequest[0];
    String path = singleLineRequest[1];
    String version = singleLineRequest[2];
    String host = linesFromRequest[1].split(" ")[1];
    
    List<String> headers = new ArrayList<>();
    for (int h = 2; h < linesFromRequest.length; h++) {
      String header = linesFromRequest[h];
      headers.add(header);
    }

    try {
      if (!version.equals("HTTP/1.1")) {
        sendResponse(client, StatusCode.CODE_505, null, null, HeaderType.ERROR);
      } else {
        switch (method) {
          case "GET":
            handleGET(client, path);
            break;
          case "HEAD":
            handleHEAD(client, path);
            break;
          case "DELETE":
            handleDELETE(client, path);
            break;
          case "PUT":
            handlePUT(client, path, body);
            break;
          case "POST":
            handlePOST(client, path, body);
            break;
          case "OPTIONS":
            handleOPTION(client);
            break;
          default:
            sendResponse(client, StatusCode.CODE_501, null, null, HeaderType.ERROR);
        }
      }
    } catch (Exception e){
      System.out.println("Error line "+e.getStackTrace()[0].getLineNumber()+" : " + e);
      e.printStackTrace();
      try{
        sendResponse(client,StatusCode.CODE_500,null,null,HeaderType.ERROR);
      } catch (IOException ioException) {
        System.out.println("Error at sending "+StatusCode.CODE_500);
        ioException.printStackTrace();
      }
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

  private void handleHEAD(Socket client,String path) throws IOException {
    Path filePath = getFilePath(path);
    // Vérification de l'existence de la ressource demandée
    if(Files.exists(filePath)) {
      String contentType = Files.probeContentType(filePath);
      sendResponse(client, StatusCode.CODE_200, contentType,Files.readAllBytes(filePath),HeaderType.HEAD);
    } else {
      sendResponse(client, StatusCode.CODE_404, null, null, HeaderType.ERROR);
    }
  }

  private void handleDELETE(Socket client, String path) throws IOException {
    Path filePath = getFilePath(path);
    if(Files.exists(filePath)){
        System.out.println("DELETED file : "+filePath);
        Files.delete(filePath);
        sendResponse(client, StatusCode.CODE_200, null, null, HeaderType.DELETE);
    } else {
      System.out.println("File not found : "+filePath);
      sendResponse(client, StatusCode.CODE_404, null, null, HeaderType.ERROR);
    }
  }
  
  private void handlePOST(Socket client,String path, String body) throws IOException {

    if(path.endsWith(".txt")){
      appendToFile(path, client, body);
    }
    else if(path.endsWith(".java")){
      try{
        String chemin ="java -classpath /media/corentin/Data/Cours/PR-Reseaux/TP/Serveur_HTTP/Reseaux_HTTPServer/bin source."+path.substring(1, path.length()-5) + " " + body;
        String r = executeCommand(new String[]{"/bin/bash", "-c", chemin });
        System.out.println("exécution du fichier externe:" + r);
        byte[] successfulPOST = ("<h1> The script has executed correctly </h1>").getBytes(StandardCharsets.UTF_8);
        sendResponse(client, StatusCode.CODE_200,"text/html",successfulPOST,HeaderType.POST);
      }catch(Exception e){
        e.printStackTrace();
      }
    }else{
      StatusCode statusCode;
        if(Files.exists(getFilePath(path))) {
          statusCode = StatusCode.CODE_403;
        } else {
          statusCode = StatusCode.CODE_404;
        }
        sendResponse(client, statusCode, null, null, HeaderType.ERROR);
    }
  }

  private void appendToFile(String path, Socket client,String body) throws IOException {
    if(body != null){
      File resource = new File(path);
      boolean appendToFile = resource.exists();
      BufferedOutputStream fileOut = new BufferedOutputStream(new FileOutputStream(resource,appendToFile));
      String[] parameters = body.split("&",10);

      for(String param : parameters){
          byte[] byteParam = (param + ";").getBytes();
          fileOut.write(byteParam, 0, byteParam.length);
      }
      byte[] endLine = ("\r\n").getBytes();
      fileOut.write(endLine, 0, endLine.length);
      fileOut.flush();
      fileOut.close();
      byte[] successfulPOST = ("<h1>The file " + path +" has been modified </h1>").getBytes(StandardCharsets.UTF_8);
      sendResponse(client, StatusCode.CODE_200,"text/html",successfulPOST,HeaderType.POST);
    }else{
      sendResponse(client, StatusCode.CODE_404,null,null,HeaderType.ERROR);
    }
  }

  public String executeCommand(String[] cmd) {
    StringBuffer theRun = null;
    try {
        Process process = Runtime.getRuntime().exec(cmd);

        BufferedReader reader = new BufferedReader(
                new InputStreamReader(process.getInputStream()));
        int read;
        char[] buffer = new char[4096];
        StringBuffer output = new StringBuffer();
        while ((read = reader.read(buffer)) > 0) {
            theRun = output.append(buffer, 0, read);
        }
        reader.close();
        process.waitFor();

    } catch (IOException e) {
      System.out.println(e);
        throw new RuntimeException(e);
    } catch (InterruptedException e) {
      System.out.println(e);
        throw new RuntimeException(e);
    }
        return theRun.toString().trim();
}

  private void handlePUT(Socket client, String path, String body) throws IOException {
    Path filePath = getFilePath(path);
    File file = filePath.toFile();
    StatusCode statusCode;
    if(file.createNewFile()){
      System.out.println("File created : "+filePath);
      statusCode = StatusCode.CODE_201;
    } else {
      System.out.println("File content replaced : "+filePath);
      statusCode = StatusCode.CODE_200;
    }
    BufferedWriter writer = new BufferedWriter((new FileWriter(file,false)));
    if(body==null) body="";
    writer.write(body);
    writer.flush();
    writer.close();

    sendResponse(client, statusCode, null, null, HeaderType.PUT);
  }

  private void handleOPTION(Socket client) throws IOException {
    sendResponse(client,StatusCode.CODE_200,null,null,HeaderType.OPTIONS);
  }

  private void sendResponse(Socket client, StatusCode status, String contentType, byte[] content, HeaderType type) throws IOException {
    PrintWriter out = new PrintWriter(client.getOutputStream());
    BufferedOutputStream binaryDataOutput = new BufferedOutputStream(client.getOutputStream());
    Date date = new Date();

    System.out.println("Response with status code : "+status+" for header type : "+type);

    out.println("HTTP/1.1 "+status);
    out.println("Date: "+date);

    if(type.equals(HeaderType.GET) || type.equals(HeaderType.HEAD)) {
      out.println("Content-Type: " + contentType);
      out.println("Content-Encoding: UTF-8");
    }

    if(type.equals(HeaderType.OPTIONS)){
      out.print("Allow: OPTIONS, GET, HEAD, POST, PUT, DELETE");
    }

    if(content!=null){
      out.println("Content-Length: " + content.length);
    } else {
      out.println("Content-Length: 0");
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
   * @param args the port used to start the WebServer
   */
  public static void main(String args[]) {

    if(args.length!=1){
      System.out.println("Usage: java WebServer <port>");
      System.exit(1);
    }
    int port = Integer.parseInt(args[0]);
    WebServer ws = new WebServer();
    ws.start(port);
  }
}
