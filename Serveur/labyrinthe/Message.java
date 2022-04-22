package labyrinthe;

import java.io.IOException;
import java.io.PrintWriter;
import java.net.Socket;

import Pasgit.Joueur;

public class Message {
    public static void send(Joueur j,Position pos,int score){
        try {
            Socket sock = j.getSocket();
            PrintWriter pw = new PrintWriter(sock.getOutputStream());
            if(score==-1) {
                pw.write("Move! " + pos.getX() + " " + pos.getY() + " ***");
            }else{
                pw.write("Move! " + pos.getX() + " " + pos.getY() + " "+ score +" ***");
            }
            pw.flush();
        }catch (IOException e){

        }
    }
}
