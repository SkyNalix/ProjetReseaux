package serveur;

import java.net.*;

public class ServeurUDP implements Runnable {
    int portServ;
    public ServeurUDP(int portServ){
        this.portServ = portServ;
    }

    @Override
    public void run(){
        try{
            byte[] data = new byte[200];
            DatagramSocket sock = new DatagramSocket(this.portServ);
            while(true){
                DatagramPacket paquet = new DatagramPacket(data, data.length);
                sock.receive(paquet);
                String str =new String(paquet.getData(),0,paquet.getLength());

                String id = ""; int n = 0; String msg = "";
                for( int i = 2; str.charAt(i) != ' '; i++ ) {
                    id += str.charAt(i);
                    n = i;
                }
                for(int j = n+2;j < str.length()-8;j++){
                    msg += str.charAt(j);
                }

                String envoyeur ="";
                for(int k = str.length()-8;k < str.length();k++){
                    envoyeur += str.charAt(k);
                }
                

                for(int i =0; i < Serveur.listePartie.size();i++){
                    for(int j = 0; j < Serveur.listePartie.get(i).getListeJoueur().size();j++){
                        Joueur joueur = Serveur.listePartie.get(i).getListeJoueur().get(j);
                        if(joueur.getPseudo().equals(id)){
                            byte[] data2 = (envoyeur + ": " + msg).getBytes();
                            DatagramPacket paquet2 = new DatagramPacket(data2,data2.length,InetAddress.getByName("localhost"),joueur.getPort());
                            DatagramSocket envoiUDP = new DatagramSocket();
                            envoiUDP.send(paquet2);
                            envoiUDP.close();
                            System.out.println(msg);
                        }
                    }
                }
            }
        }catch(Exception e){e.printStackTrace();}
    }

}
