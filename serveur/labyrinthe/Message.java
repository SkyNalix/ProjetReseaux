package serveur.labyrinthe;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetSocketAddress;

public class Message {
    public void sendUDP(InetSocketAddress ia,String mess) throws IOException {
        DatagramSocket sock=new DatagramSocket();
        byte[] data=mess.getBytes();
        DatagramPacket paquet=new DatagramPacket(data,data.length,ia);
        sock.send(paquet);
        sock.close();
    }
}
