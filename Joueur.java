import java.io.*;
import java.net.*;

public class Joueur {
    private String pseudo;
    private Socket socketAssocie;
    private Partie enJeu;

    public Joueur(String pseudo,Socket socketAssocie,Partie enJeu){
        //condition des 8 caractères ?
        this.pseudo = pseudo;
        this.socketAssocie = socketAssocie;
        this.enJeu = enJeu;
    }

    public String getPseudo(){
        return this.pseudo;
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

    public void setPseudo(String nouveau){
        this.pseudo = nouveau;
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
}
