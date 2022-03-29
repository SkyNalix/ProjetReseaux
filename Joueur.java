import java.io.*;
import java.net.*;
import java.util.ArrayList;

public class Joueur {
    private String pseudo;
    private Socket socketAssocie;
    private Partie enJeu;
    private boolean isReady;

    public Joueur(String pseudo,Socket socketAssocie,Partie enJeu,boolean isReady){
        //condition des 8 caractères ?
        this.pseudo = pseudo;
        this.socketAssocie = socketAssocie;
        this.enJeu = enJeu;
        this.isReady = isReady;
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

    public boolean getReady(){
        return this.isReady;
    }

    public void setReady(boolean nouveau){
        this.isReady = nouveau;
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
        //on decrypte le str
        String res = "";
        int compteur = 0;
        for(int i = 6;str.charAt(i) != ' ';i++){
                res += Character.toString(str.charAt(i));
                compteur = i;
        }
        String id = res;
        res = "";
        System.out.println(compteur);

        for(int j = compteur+2;str.charAt(j) != ' ';j++){
            res += Character.toString(str.charAt(j));
            compteur = j;
        }
        String port = res;
        res = "";
        System.out.println(port);
        

        for(int k = compteur+2; k < str.length()-3;k ++){
            res += Character.toString(str.charAt(k)); 
        }
        System.out.println(compteur);
        int m = Integer.valueOf(res);

        ArrayList<Joueur> listeJoueur = new ArrayList<>();
        Partie partie = new Partie(listeJoueur, port, id, false, 2,m);
        partie.ajouterJoueur(this);
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
        res = "";

        for(int j = compteur +2;str.charAt(j) != ' ';j++){
            res += Character.toString(str.charAt(j));
            compteur = j;
        }
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
}
