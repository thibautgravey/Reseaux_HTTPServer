package http.server;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.*;

/**
 * A simple HTTP Web Server in Java
 * Supported HTTP methode : OPTIONS, GET, HEAD, POST, PUT, DELETE
 *
 * @author Branchereau Corentin
 * @author Gravey Thibaut
 * @version 1.0
 */
public class WebServer {

  /**
   * Represent the HTTP method for the build of the response
   */
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
   *  Starting the WebServer in order to handle
   *  client's connection, their request and
   *  send an appropriate response. Finally, close the remote connection with the client.
   *
   * @param port the webserver port
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
        //System.out.println("Connection, sending data.");
        // read the data sent.
        // stop reading once a blank line is hit. This
        // blank line signals the end of the client HTTP
        // headers.
        String str = ".";
        StringBuilder stringBuilder = new StringBuilder();
        String headerLength = "0";
        while (((str=in.readLine())!=null) && !str.isBlank()){
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

        if(!request.isBlank()){
            handleClientRequest(remote, request, body);
        }

        // close the connection
        remote.close();
      } catch (Exception e) {
        System.out.println("Error line "+e.getStackTrace()[0].getLineNumber()+" : " + e);
        e.printStackTrace();
      }
    }
  }

  /**
   * Handle a typical client request and his body in order to call the right HTTP-method handler
   * It also verify that the HTTP Method requested is implemented and authorized.
   * @param client the client sending the request
   * @param request the client request (header)
   * @param body the client body if present
   */
  private void handleClientRequest(Socket client, String request,String body) {
    //log the request & body
    Date date = new Date();
    DateFormat dateFormat = new SimpleDateFormat("hh:mm:ss");
    System.out.println("["+dateFormat.format(date)+" from "+client.getInetAddress()+"]");
    System.out.println("Request :\n"+request);
    System.out.println("Body :\n"+body);

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
        sendResponse(client, StatusCode.CODE_505, null, null, null ,HeaderType.ERROR);
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
            sendResponse(client, StatusCode.CODE_501, null, null, null,HeaderType.ERROR);
        }
      }
    } catch (Exception e){
      System.out.println("Error line "+e.getStackTrace()[0].getLineNumber()+" : " + e);
      e.printStackTrace();
      try{
        sendResponse(client,StatusCode.CODE_500,null,null,null,HeaderType.ERROR);
      } catch (IOException ioException) {
        System.out.println("Error at sending "+StatusCode.CODE_500);
        ioException.printStackTrace();
      }
    }
  }

  /**
   * Method that handles GET method
   * @param client the client sending the request
   * @param path the path of the wanted resource
   * @throws IOException
   */
  private void handleGET(Socket client, String path) throws IOException {
    int paramIndex = path.indexOf("?");
    String pathWithoutParam = path;
    if(paramIndex != -1){
      pathWithoutParam = path.substring(0,paramIndex);
    }
    Path filePath = getFilePath(pathWithoutParam);
    System.out.println("FilePath after guess : "+filePath);
    if(filePath.toString().equals("./res/index.html")){ //Generate a structured index
      String response = generateIndex();
      sendResponse(client, StatusCode.CODE_200, "text/html", response.getBytes(),filePath.toString(),HeaderType.GET);
    } else if(filePath.toString().startsWith("./res/source")){ //Non-Authorized
      sendResponse(client, StatusCode.CODE_401,null,null, filePath.toString() ,HeaderType.ERROR);
    } else if (Files.exists(filePath)) { // if the file exist
      String contentType = Files.probeContentType(filePath);
      sendResponse(client, StatusCode.CODE_200, contentType, Files.readAllBytes(filePath),filePath.toString(),HeaderType.GET);
    } else { // Error 404 not found
      byte[] contentNotFound;
      if(Files.exists(getFilePath("/404NotFound.html"))){
        contentNotFound = Files.readAllBytes(getFilePath("/404NotFound.html"));
      } else {
        contentNotFound = "<h1>404 Not found :(</h1>".getBytes(StandardCharsets.UTF_8);
      }
      sendResponse(client, StatusCode.CODE_404, "text/html", contentNotFound,null,HeaderType.GET);
    }
  }

  /**
   * Method that handles HEAD method
   * @param client the client sending the request
   * @param path the path of the wanted resource
   * @throws IOException
   */
  private void handleHEAD(Socket client,String path) throws IOException {
    Path filePath = getFilePath(path);
    // Vérification de l'existence de la ressource demandée
    if(Files.exists(filePath)) {
      String contentType = Files.probeContentType(filePath);
      sendResponse(client, StatusCode.CODE_200, contentType,Files.readAllBytes(filePath),filePath.toString(),HeaderType.HEAD);
    } else {
      sendResponse(client, StatusCode.CODE_404, null, null,null, HeaderType.ERROR);
    }
  }

  /**
   * Method that handles DELETE method
   * @param client the client sending the request
   * @param path the path of the wanted resource
   * @throws IOException
   */
  private void handleDELETE(Socket client, String path) throws IOException {
    Path filePath = getFilePath(path);
    if(Files.exists(filePath)){
      System.out.println("DELETED file : "+filePath);
      Files.delete(filePath);
      sendResponse(client, StatusCode.CODE_200, null, null,filePath.toString(), HeaderType.DELETE);
    } else {
      System.out.println("File not found : "+filePath);
      sendResponse(client, StatusCode.CODE_404, null, null, null,HeaderType.ERROR);
    }
  }

  /**
   * Method that handles POST method
   * @param client the client sending the request
   * @param path the path of the wanted resource
   * @param body the body of the request
   * @throws IOException
   */
  private void handlePOST(Socket client,String path, String body) throws IOException {
    System.out.println(body);
    if(path.endsWith(".txt")){
      appendToFile(path, client, body);
    }
    else if(path.endsWith(".java")){
      executeDynamicScript(client,path,body);
    }else{
      StatusCode statusCode;
      String contentLocation = null;
      if(Files.exists(getFilePath(path))) {
        statusCode = StatusCode.CODE_403;
        contentLocation = getFilePath(path).toString();
      } else {
        statusCode = StatusCode.CODE_404;
      }
      sendResponse(client, statusCode, null, null,contentLocation, HeaderType.ERROR);
    }
  }

  /**
   * Generate an index.html for list available ressources on the WebServer
   * @return the generate HTML file
   */
  private String generateIndex(){
    StringBuilder response = new StringBuilder();
    response.append("<h1>Voici l'index de nos ressources disponibles :</h1>").append("\n");
    response.append("<ul>").append("\n");
    response.append("<li>Ressources :").append("\n");
    response.append("<ul>");
    File mainRessource = new File("./res");
    String[] files = mainRessource.list();
    for(String f : files){
      if(!f.equals("source")) {
        response.append("<li><a href=\"/").append(f).append("\">");
        response.append(f);
        response.append("</a></li>").append("\n");
      }
    }
    response.append("</ul>").append("\n");
    response.append("</li>").append("\n");
    response.append("<li>Source :").append("\n");
    response.append("<ul>");
    File mainSource = new File("./res/source");
    String[] sources = mainSource.list();
    for(String s : sources){
      response.append("<li><a href=\"/source/").append(s).append("\">");
      response.append(s);
      response.append("</li>").append("\n");
    }
    response.append("</ul>").append("\n");
    response.append("</li>").append("\n");
    response.append("</ul>").append("\n");
    return response.toString();
  }

  /**
   * Append the body in parameter to the resource located by the path in parameter. If the resource exist, just append
   * the body. If it is not, create the resource with the body for content.
   * @param path the path of the wanted resource
   * @param client the client sending the request
   * @param body the body of the request
   * @throws IOException
   */
  private void appendToFile(String path, Socket client,String body) throws IOException {
    if(body != null){
      Path resourcePath = getFilePath(path);
      File resource = resourcePath.toFile();
      boolean appendToFile = resource.exists();
      BufferedWriter fileOut = new BufferedWriter((new FileWriter(resource,appendToFile)));
      fileOut.write(body);
      String endLine = ("\r\n");
      fileOut.write(endLine);
      fileOut.flush();
      fileOut.close();
      byte[] successfulPOST = ("<h1>The file " + path +" has been modified </h1>").getBytes(StandardCharsets.UTF_8);
      sendResponse(client, StatusCode.CODE_200,"text/html",successfulPOST,resource.toString(),HeaderType.POST);
    }else{
      sendResponse(client, StatusCode.CODE_404,null,null,null,HeaderType.ERROR);
    }
  }

  /**
   * Execute a dynamic script on the WebServer
   * @param client the client sending the request
   * @param path the path of the dynamic script resource
   * @param body the body of the request
   */
  private void executeDynamicScript(Socket client,String path,String body){
    try{
      String[] parameters = body.split("&",10);
      String arguments = "";
      for(String param : parameters){
        arguments += param +" ";
      }
      if(!arguments.equals("")){
        String chemin ="java -classpath ./bin source."+path.substring(1, path.length()-5) + " " + arguments;
        String response = executeCommand(new String[]{"/bin/bash", "-c", chemin });
        byte[] successfulPOST = response.getBytes(StandardCharsets.UTF_8);
        sendResponse(client, StatusCode.CODE_200,"text/html",successfulPOST,getFilePath(path).toString(),HeaderType.POST);
      }else{
        sendResponse(client, StatusCode.CODE_204,null,null,getFilePath(path).toString(),HeaderType.POST);
      }
    }catch(Exception e){
      e.printStackTrace();
    }
  }

  /**
   * Execute a command on the server terminal on Runtime
   * @param cmd the cmd to execute
   * @return response from execution
   */
  private String executeCommand(String[] cmd) {
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

  /**
   * Method that handles PUT method
   * @param client the client sending the request
   * @param path the path of the wanted resource
   * @param body the body of the request
   * @throws IOException
   */
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

    sendResponse(client, statusCode, null, null,filePath.toString(), HeaderType.PUT);
  }

  /**
   * Method that handles PUT method
   * @param client the client sending the request
   * @throws IOException
   */
  private void handleOPTION(Socket client) throws IOException {
    sendResponse(client,StatusCode.CODE_200,null,null,null,HeaderType.OPTIONS);
  }

  /**
   * After a request, create an appropriate response for the client and send it depends on the HTTP method
   * and many other variables (resource availablity, right of access, implemented http method, etc...)
   * @param client the client sending the request and waiting for response
   * @param status the HTTP status code to return
   * @param contentType the content-type for header
   * @param content the content of the response
   * @param contentLocation the content-location for header
   * @param type the HeaderType depends on the http request method
   * @throws IOException
   * @see StatusCode
   */
  private void sendResponse(Socket client, StatusCode status, String contentType, byte[] content, String contentLocation, HeaderType type) throws IOException {
    PrintWriter out = new PrintWriter(client.getOutputStream());
    BufferedOutputStream binaryDataOutput = new BufferedOutputStream(client.getOutputStream());
    Date date = new Date();

    System.out.println("Response with status code : "+status+" for header type : "+type);

    out.println("HTTP/1.1 "+status);
    out.println("Date: "+date);

    if(contentLocation!=null && !contentLocation.isBlank()){
      out.println("Content-Location: "+contentLocation.substring(1));
    }

    if(type.equals(HeaderType.GET) || type.equals(HeaderType.HEAD)) {
      out.println("Content-Type: " + contentType+";charset=UTF-8");
    }

    if(type.equals(HeaderType.OPTIONS)){
      out.println("Allow: OPTIONS, GET, HEAD, POST, PUT, DELETE");
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

  /**
   * Get a file path from a resource file on the server
   * '/' gives the index from the page
   * @param path the file path to find (asked in the request)
   * @return Path on the server with prefix ./res
   */
  private Path getFilePath(String path) {
    if ("/".equals(path)) {
      path = "/index.html";
    }
    return Paths.get("./res", path);
  }

  /**
   * Main method starting the WebServer
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
