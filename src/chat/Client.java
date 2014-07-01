package chat;

import com.sun.org.apache.xml.internal.security.exceptions.Base64DecodingException;
import com.sun.org.apache.xml.internal.security.utils.Base64;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import javax.sound.sampled.*;
import javax.swing.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.*;
import java.net.Socket;
import java.util.Iterator;
import java.util.List;

public class Client {
    UItemp ut;
    AudioFormat audioformat;
    boolean stopcapture = false;
    static TargetDataLine tdl;
    private Mixer mixer;
    byte[]buff;
    Socket socket;
    SourceDataLine sdl;
    AudioInputStream ais;
    PrintWriter pw;
    DefaultListModel defaultListModel;
    JList<String> connectionList;
    public Client() {
        ut = new UItemp();
        ut.setVisible(true);
        defaultListModel=new DefaultListModel();
        connectionList=new JList(defaultListModel);
        ut.connectionScroller.setViewportView(connectionList);

    }

    public static void main(String[] args) {

        final Client cl = new Client();
        cl.ut.connectbutton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                try {
                    cl.socket=new Socket(cl.ut.ip.getText(),Integer.parseInt(cl.ut.port.getText()));
                    PrintWriter pw=new PrintWriter(cl.socket.getOutputStream());
                    JSONObject jsonObject=new JSONObject();
                    jsonObject.put("Name",cl.ut.username.getText());
                    pw.println(jsonObject.toJSONString());
                    pw.flush();
                } catch (IOException e1) {
                    JOptionPane.showMessageDialog(cl.ut, "Check The IP and Port", "Error Connecting", JOptionPane.ERROR_MESSAGE);
                }
                cl.ut.connectbutton.setEnabled(false);
                cl.ut.ip.setEnabled(false);
                cl.ut.port.setEnabled(false);
                cl.ut.username.setEnabled(false);

            }
        });
        cl.ut.send.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                cl.stopcapture=true;
                tdl.stop();
                tdl.close();
                cl.ut.start.setEnabled(true);

            }
        });
        new Thread(){
            @Override
            public void run() {
                try {
                    cl.decide();
                } catch (IOException e) {
                    e.printStackTrace();
                } catch (ParseException e) {
                    e.printStackTrace();
                } catch (Base64DecodingException e) {
                    e.printStackTrace();
                } catch (LineUnavailableException e) {
                    e.printStackTrace();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }.start();
        cl.ut.start.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (cl.connectionList.getSelectedValuesList().size() == 0) {
                    JOptionPane.showMessageDialog(cl.ut, "Select From The List The People You Want To Chat ", "Select!!", JOptionPane.INFORMATION_MESSAGE);
                } else{
                    Thread t = new Thread() {
                        @Override
                        public void run() {
                            try {
                                cl.captureAudio();
                                System.out.println("started");
                            } catch (LineUnavailableException e1) {
                                e1.printStackTrace();
                            }
                        }
                    };
                t.start();
                cl.ut.start.setEnabled(false);
                }

            }
        });

        cl.ut.sendButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
               if(cl.connectionList.getSelectedValuesList().size()==0){
                   JOptionPane.showMessageDialog(cl.ut, "Select From The List The People You Want To Chat ", "Select!!", JOptionPane.INFORMATION_MESSAGE);
               } else{
                    try {
                        PrintWriter pw=new PrintWriter(cl.socket.getOutputStream());
                        JSONObject jsonObject=new JSONObject();
                        jsonObject.put("Name",cl.ut.username.getText());
                        jsonObject.put("Mode","Text");
                        jsonObject.put("Message", cl.ut.text.getText());
                        jsonObject.put("Length",cl.ut.text.getText().length());
                        List obj=cl.connectionList.getSelectedValuesList();
                        Iterator it=obj.iterator();
                        String data="";
                        while(it.hasNext()){
                            data=data+it.next()+",";
                        }
                        jsonObject.put("List",data);
                        String str=(String)jsonObject.get("List");
                        str=str.substring(0,str.length()-1);
                        if(!str.contains((String)jsonObject.get("Name"))) {
                            cl.ut.common.append(jsonObject.get("Name") + "------->" + str + "::" + jsonObject.get("Message") + "\n");
                        }
                        cl.ut.text.setText("");
                        pw.println(jsonObject.toJSONString());
                        pw.flush();

                    } catch (IOException e1) {
                        e1.printStackTrace();
                    }
               }
            }
        });

    }
    public void decide() throws IOException, ParseException, Base64DecodingException, LineUnavailableException, InterruptedException {
        while(true) {
            try {
                BufferedReader br = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                String message;

                while ((message = br.readLine()) != null) {
                    JSONObject jsonObject = (JSONObject) new JSONParser().parse(message);

                    if(jsonObject.get("Mode").equals("Connections") ){
                        System.out.println("insode connection mode");
                        putlist((String) jsonObject.get("ConnectionsList"));
                    }
                    else if (jsonObject.get("Mode").equals("Sound")) {

                        String s = (String) jsonObject.get("Message");

                        playedAudio(Base64.decode(s), Integer.parseInt(jsonObject.get("Length").toString()));

                    } else if (jsonObject.get("Mode").equals("Text")) {
                        String str=(String)jsonObject.get("List");
                        str=str.substring(0,str.length()-1);
                        ut.common.append(jsonObject.get("Name")+"------->"+str+"::"+ jsonObject.get("Message")+"\n");
                    }
                }
            } catch (Exception e) {

            }
        }

    }
    public void putlist(final String message){
        new Thread(){
            @Override
            public void run() {
                String []names=message.split(",");

                for (int i = 0; i < names.length; i++) {
                    if(!defaultListModel.contains(names[i])) {
                        defaultListModel.addElement(names[i]);
                    }

                }
            }
        }.start();
    }


    public void captureAudio() throws LineUnavailableException {
        Mixer.Info[] inf = AudioSystem.getMixerInfo();
        audioformat = new AudioFormat(8000.0f, 16, 1, true, false);
        mixer = AudioSystem.getMixer(inf[2]);
        DataLine.Info infor = new DataLine.Info(TargetDataLine.class, audioformat);

        tdl = (TargetDataLine) mixer.getLine(infor);
        tdl.open(audioformat);
        tdl.start();
        captureThread ct=new captureThread();
        ct.start();

    }
    public void playedAudio(byte[] b,int c) throws IOException, LineUnavailableException, InterruptedException {

        ByteArrayInputStream bais;
        DataLine.Info info=new DataLine.Info(SourceDataLine.class,audioformat);
        sdl=(SourceDataLine)AudioSystem.getLine(info);
        sdl.open(audioformat);
        bais=new ByteArrayInputStream(b,0,c);
        ais=new AudioInputStream(bais,audioformat,buff.length/audioformat.getFrameSize());
        sdl.start();
        playThread pt=new playThread();

        pt.start();

        pt.join();
    }
    private class playThread extends Thread{
        byte []tempbuff=new byte[tdl.getBufferSize()/2];
        @Override
        public void run() {
            int c;
            try {
                while ((c = ais.read(tempbuff, 0, tempbuff.length)) != -1) {
                    System.out.println("Playing Sound");
                    sdl.write(tempbuff,0,c);
                }
                sdl.drain();
            }catch(Exception e){
                e.printStackTrace();
            }
        }
    }
    private class captureThread extends Thread {

        public captureThread() {
        }

        @Override
        public void run() {
            buff=new byte[tdl.getBufferSize()/2];
            stopcapture=false;
            try {
                pw=new PrintWriter(socket.getOutputStream());


                while(stopcapture==false) {
                    int c = tdl.read(buff, 0, buff.length);
                    String s= Base64.encode(buff);
                    JSONObject jsonObject=new JSONObject();
                    jsonObject.put("Name",ut.username.getText());
                    jsonObject.put("Mode","Sound");
                    jsonObject.put("Message", s);
                    jsonObject.put("Length",c);
                    List obj=connectionList.getSelectedValuesList();
                    Iterator it=obj.iterator();
                    String data="";
                    while(it.hasNext()){
                        data=data+it.next()+",";
                    }
                    jsonObject.put("List",data);

                    pw.println(jsonObject.toJSONString());
                    pw.flush();
                }


            } catch (IOException e) {
                e.printStackTrace();
            }






        }
    }
}
