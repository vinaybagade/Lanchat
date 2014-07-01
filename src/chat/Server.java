package chat;

import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

public class Server {
    Map<String,Socket> rel=new HashMap<String, Socket>();

    PrintWriter pw;
    private class socketThread extends Thread{
        BufferedReader br;
        Socket sock;
        private socketThread(Socket socket) throws IOException {
            sock=socket;
            br=new BufferedReader(new InputStreamReader(socket.getInputStream()));
        }

        @Override
        public void run() {
            try {
                String s;
                while((s=br.readLine())!=null) {
                    JSONObject jsonObject = (JSONObject) new JSONParser().parse(s);
                    String listString = (String) jsonObject.get("List");
                    String[] list = listString.split(",");
                    telllist(list, jsonObject);

                }
            } catch (IOException e) {
                e.printStackTrace();
            } catch (ParseException e) {
                e.printStackTrace();
            }
        }
    }
    public void go() throws IOException, InterruptedException, ParseException {
        ServerSocket serverSocket=new ServerSocket(5000);
        while (true){
            Socket socket=serverSocket.accept();
            BufferedReader br=new BufferedReader(new InputStreamReader(socket.getInputStream()));
            JSONObject jsonObject = (JSONObject) new JSONParser().parse(br.readLine());
            rel.put((String) jsonObject.get("Name"), socket);
            telleveryone();
            socketThread st=new socketThread(socket);
            st.start();



        }

    }
    public void telleveryone() throws IOException {

        JSONObject jsonObject=new JSONObject();
        Iterator iterator=rel.keySet().iterator();
        String list="";
        while (iterator.hasNext()){
            list=list+iterator.next()+",";
        }
        System.out.println(list);
        Iterator tell=rel.keySet().iterator();
        jsonObject.put("Mode","Connections");
        jsonObject.put("ConnectionsList",list);
        System.out.println(jsonObject.toJSONString());
        while(tell.hasNext()) {
            Socket sock=rel.get(tell.next());
            pw = new PrintWriter(sock.getOutputStream());
            pw.println(jsonObject.toJSONString());
            pw.flush();
        }

    }
    public void telllist(String []args,JSONObject jsonObject) throws IOException {

        for (int i = 0; i < args.length; i++) {
            Socket sock=rel.get(args[i]);
            pw=new PrintWriter(sock.getOutputStream());
            pw.println(jsonObject.toJSONString());
            pw.flush();
        }

    }
    public static void main(String[] args) throws IOException, InterruptedException, ParseException {
        Server server=new Server();
        server.go();
    }

}
