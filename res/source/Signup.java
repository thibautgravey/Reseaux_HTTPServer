package source;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.io.*;

public class Signup {
    public static void main(String[] args){
        String login = "";
        if(args.length <1){
            System.out.println("<h1>Une erreur est survenue, merci de réessayer</h1> <script> setTimeout(function(){ window.location = '/signup.html'; },2000); </script> ");
        }
        try{
            File resource = new File("./res/source/user.txt");
            BufferedOutputStream fileOut = new BufferedOutputStream(new FileOutputStream(resource,true));

            for(int i=0;i<args.length;i++){    
                 try{
                     String key = ((String) args[i]).split("=")[0];
                     if(key.equals("login")){
                         login = ((String) args[i]).split("=")[1];
                     }
                 }catch(Exception e){
                     login = "";
                 }
                byte[] byteParam = (args[i] + ";").getBytes();
                fileOut.write(byteParam, 0, byteParam.length);
                
            }
            byte[] endLine = ("\r\n").getBytes();
            fileOut.write(endLine, 0, endLine.length);
            fileOut.flush();
            fileOut.close();
        }catch(Exception e){
            e.printStackTrace();
            System.out.println("<h1>Une erreur est survenue, merci de réessayer</h1> <script> setTimeout(function(){ window.location = '/signup.html'; },2000); </script> ");

        }
        System.out.println(" <h1 style='text-align:center'> You signed up successfully ! </h1> <script> setTimeout(function(){ window.location = '/dashboard.html?name="+ login +"'; },2000); </script>");
    }
}
