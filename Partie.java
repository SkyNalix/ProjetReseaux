import java.io.*;
import java.util.ArrayList;

public class Partie {

    private ArrayList<Joueur> listeJoueur;
    private int maxJoueur;
    private String port;
    private String id;
    private boolean lancer;
    
    public Partie(ArrayList<Joueur> listeJoueur,String port,String id,boolean lancer,int maxJoueur){
        this.listeJoueur = listeJoueur;
        this.id = id;
        this.port = port;
        this.lancer = lancer;
        this.maxJoueur = maxJoueur;
    }

    public int getNbJoueur(){
        return this.listeJoueur.size();
    }

    public boolean getLancer(){
        return this.lancer;
    }

    public int getMaxJoueur(){
        return this.maxJoueur;
    }

    public String getPort(){
        return this.port;
    }

    public String getID(){
        return this.id;
    }

    public void setPort(String nouveau){
        this.port = nouveau;
    }

    public void setID(String nouveau){
        this.id = nouveau;
    }

    public void setLancer(boolean nouveau){
        this.lancer = nouveau;
    }

    public void setMaxJoueur(int nouveau){
        this.maxJoueur = nouveau;
    }

    public void ajouterJoueur(Joueur nouveau){
        if(this.listeJoueur.contains(nouveau) == false && this.getNbJoueur() < this.getMaxJoueur()){
            this.listeJoueur.add(nouveau);
        }else{
            System.out.println("Opération impossible");
        }
    }

    public void retirerJoueur(Joueur supr){
        if(this.listeJoueur.contains(supr)){
            this.listeJoueur.remove(supr);
        }else{System.out.println("Joueur absent");}    
    }

    public static String getNbPartie(ArrayList<Partie> liste){
        int nb = 0;
        for (int i =0 ; i < liste.size();i++){
            if (liste.get(i).lancer == false && liste.get(i).getNbJoueur() > 0)
                nb ++;
        }
        return String.valueOf(nb);
    }

    public static void envoyerListePartie(Joueur x,ArrayList<Partie> liste){
        try {
            for(int i = 0; i < liste.size(); i++ ){
                if(liste.get(i).lancer == false && liste.get(i).getNbJoueur() > 0){
                    PrintWriter pw = new PrintWriter(x.getSocket().getOutputStream());
                    pw.write("OGAMES " + liste.get(i).getID() + " " + String.valueOf(liste.get(i).getNbJoueur()) + "***" );
                    pw.flush();
                   // String str = liste.get(i).getID() + " " + String.valueOf(liste.get(i).getNbJoueur());
                }

            }
        }catch(Exception e){
            e.printStackTrace();
        }
    }
    
}
