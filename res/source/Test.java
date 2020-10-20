package source;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.io.*;

public class Test {
    public static void main(String[] args){
        if(args.length <1){
            System.out.println("error two few arguments");
        }
        String body = args[0];
        try{
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
        }catch(Exception e){
            e.printStackTrace();
        }
        System.out.println("done");
    }
}
