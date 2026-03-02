import java.net.*;
import java.io.*;
import java.util.*;
public class ClientMulti {
    public static void main(String [] args){

        try{
            Socket socket = new Socket("localhost",3000);
            PrintWriter out= new PrintWriter(socket.getOutputStream());
            BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()) );
            Scanner sc=new Scanner(System.in);
            String line=null;
            while(!"exit".equals(line)){
                line=sc.nextLine();
                out.println(line);
                out.flush();
                System.out.println("Server Replied "+in.readLine());

            }
            sc.close();
            socket.close();

        }catch(IOException e){
            e.printStackTrace();
        }
    }

}
