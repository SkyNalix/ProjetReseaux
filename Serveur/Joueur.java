
import java.io.*;
import java.net.*;
import java.util.ArrayList;

import labyrinthe.Labyrinthe;
import labyrinthe.Personne;


public class Joueur extends Personne {
    private String pseudo;
    private Socket socketAssocie;
    private Partie enJeu;
    private boolean isReady;
    private DatagramSocket port;
    private int score=0;
    
    public Joueur(String pseudo,Socket socketAssocie,Partie enJeu,boolean isReady,DatagramSocket port){
        //condition des 8 caractères ?
        super(pseudo);
        this.pseudo = pseudo;
        this.socketAssocie = socketAssocie;
        this.enJeu = enJeu;
        this.isReady = isReady;
        this.port = port;
    }

    public String getPseudo(){
        return this.pseudo;
    }

    public DatagramSocket getPort(){
        return this.port;
    }

    public Socket getSocket(){
        return this.socketAssocie;
    }

    public Partie getPartie(){
        return this.enJeu;
    }

    public void setPartie(Partie nouveau){
        this.enJeu = nouveau;
    }

    public boolean getReady(){
        return this.isReady;
    }

    public void setReady(boolean nouveau){
        this.isReady = nouveau;
    }

    public int getScore() {
        return score;
    }

    public void setScore(int score) {
        this.score = score;
    }

    @Override
    public boolean isFantome(){
        return false;
    }

    public void setPort(String nouveau){
        try{
            DatagramSocket sock = new DatagramSocket(Integer.parseInt(nouveau));
            this.port = sock;
        }catch(Exception e){
            e.printStackTrace();
        }
    }



    public boolean portUnique(String port){
        Serveur.listeJoueur.remove(this);
        for(int i = 0; i < Serveur.listeJoueur.size();i++){
            if(Serveur.listeJoueur.get(i).getPort().getLocalPort() == Integer.parseInt(port) 
            && Serveur.listeJoueur.get(i).getPseudo().equals(this.getPseudo()) == false){
                Serveur.listeJoueur.add(this);
                return false;
            }
        }
        Serveur.listeJoueur.add(this);
        return true;
    }

    public boolean pseudoUnique(String pseudo){
        Serveur.listeJoueur.remove(this);
        for(int i = 0; i < Serveur.listeJoueur.size();i++){
            if(Serveur.listeJoueur.get(i).getPseudo().equals(pseudo)){ 
                Serveur.listeJoueur.add(this);
                return false;
            }
        }
        Serveur.listeJoueur.add(this);
        return true;
    }

    public void envoyerMessage(String msg){
        try {
            PrintWriter pw = new PrintWriter(this.socketAssocie.getOutputStream());
            pw.write(msg);
            pw.flush();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void creationProfile(){
        try {
            BufferedReader br = new BufferedReader(new InputStreamReader(this.socketAssocie.getInputStream()));
            PrintWriter pw = new PrintWriter(this.socketAssocie.getOutputStream());
            while(true){ 
                if(this.getPseudo().length() == 8){
                    return ;
                }else{
                    pw.write("Pseudo invalide veuillez recommencer");
                    pw.flush();
                    this.setPseudo(br.readLine());
                }
            }
        }catch(Exception e){
            e.printStackTrace();
        }
    }

    public Partie creerPartie(String str){
        //on decrypte le str de la Forme NEWPL id port***
        String res = "";
        int compteur = 0;
        for(int i = 6;str.charAt(i) != ' ';i++){
                res += Character.toString(str.charAt(i));
                compteur = i;
        }
        String id = res; res = "";

        for(int j = compteur+2;j < str.length()-3;j++){
            res += Character.toString(str.charAt(j));
            compteur = j;
        }
        String port = res;
        //int t = 0;
        if(id.length() != 8 || port.length() !=4 || portUnique(port) == false || pseudoUnique(id) == false ){return null;}
        this.setPseudo(id);
        if(this.getPort().getLocalPort() != Integer.parseInt(port)){this.setPort(port);}

        ArrayList<Joueur> listeJoueur = new ArrayList<>();
        Partie partie = new Partie(listeJoueur, port, id, false,32,Utilitaire.RandomM(256));
        partie.ajouterJoueur(this);
        Labyrinthe lab = new Labyrinthe(10, 10,partie.getArrayJoueur());
        Game g = new Game(partie.getArrayJoueur(),lab,false);
        partie.setGame(g);
        this.enJeu = partie;
        return partie;
    }

    public boolean connexionSimple(String nomPartie){
        if(this.getPartie() != null){return false;}
        String res = "";
        for(int i = 8;i < nomPartie.length()-3;i++){
                res += Character.toString(nomPartie.charAt(i));
        }
        for(int i =0; i < Serveur.listePartie.size();i++){
            if(Serveur.listePartie.get(i).getID().equals(res)){
                if(Serveur.listePartie.get(i).ajouterJoueur(this)){
                    this.enJeu = Serveur.listePartie.get(i);
                    return true;
                }
            }
        }
        return false;
    }

    public boolean rejoindrePartie(String str){
        if(this.getPartie() != null){return false;}
        String res = "";
        int compteur = 0;
        for(int i = 6;str.charAt(i) != ' ';i++){
                res += Character.toString(str.charAt(i));
                compteur = i;
        }
        if(res.length() != 8 || pseudoUnique(res) == false ){
            return false;
        }
        this.setPseudo(res);
        res = "";

        for(int j = compteur +2;str.charAt(j) != ' ';j++){
            res += Character.toString(str.charAt(j));
            compteur = j;
        }
        if(res.length() != 4 || portUnique(res) == false){
            return false;
        }
        this.setPort(res);    
        res = "";

        for(int k = compteur +2 ; k < str.length()-3;k++){
            res += Character.toString(str.charAt(k));
        }
        int m = Integer.valueOf(res);
        for(int l =0; l < Serveur.listePartie.size();l++){
            if(Serveur.listePartie.get(l).getM() == m){
                if(Serveur.listePartie.get(l).ajouterJoueur(this)){
                    this.enJeu = Serveur.listePartie.get(l);
                    return true;
                }
            }
        }
        return false;    
    }

    public void inviterPersonne(String pseudo){
        if(this.getPartie() != null){
            
        }
    }

    public void listePartie(String x,PrintWriter pw){
        String res="";
        for(int i = 6; i <  x.length()-3;i++){
            res += Character.toString(x.charAt(i));
        }
        int m = Integer.valueOf(res);
        
        for(int j =0; j < Serveur.listePartie.size();j++){
            if(Serveur.listePartie.get(j).getM() == m){
                Serveur.listePartie.get(j).listePartie(pw);
                return;
            }
        }
        pw.write("DUNNO***");pw.flush();
    }

    

    public void chatter(String str){
        String id ="";
        for(int i = 6; i < str.length()-3;i++){
            id += str.charAt(i);
        }
        System.out.println("id:" + id);
        for(int i =0;i < Serveur.listeJoueur.size();i++){
            if(Serveur.listeJoueur.get(i).getPseudo().equals(id)){
                try {
                    
                    String s = "Envoi";
                    byte[] data = s.getBytes();
                    DatagramPacket paquet = new DatagramPacket(data,data.length,
                        Serveur.listeJoueur.get(i).socketAssocie.getInetAddress(),
                        Serveur.listeJoueur.get(i).socketAssocie.getPort()
                    );

                    this.getPort().send(paquet);
                    
                }catch(Exception e){
                    e.printStackTrace();
                }
            }
        }
    }

    public void recevoirMsg(){
        try{
            byte[] data = new byte[100];
            DatagramPacket x = new DatagramPacket(data,data.length);
            this.port.receive(x);
        }catch(Exception e){e.printStackTrace();}
    }

}
