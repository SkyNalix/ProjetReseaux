package serveur;

import java.io.*;
//import java.net.*;

import java.util.ArrayList;

public class Partie {

    private ArrayList<Joueur> listeJoueur;
    private int maxJoueur;
    private String port;
    private String id;
    private boolean lancer;
    private int m;
    //private String mdp on fait des parties avec mdp en option ??
    //Joueur chef de Partie
    private Game game;

    public Partie( ArrayList<Joueur> listeJoueur, String port, String id, boolean lancer, int maxJoueur, int m ) {
        this.listeJoueur = listeJoueur;
        this.id = id;
        this.port = port;
        this.lancer = lancer;
        this.maxJoueur = maxJoueur;
        this.m = m;
    }

    public Joueur[] getArrayJoueur() {
        Joueur[] a = new Joueur[this.listeJoueur.size()];
        for( int i = 0; i < this.listeJoueur.size(); i++ ) {
            a[i] = this.listeJoueur.get( i );
        }
        return a;
    }

    public void setGame( Game nouveau ) {
        this.game = nouveau;
    }

    public Game getGame() {
        return this.game;
    }

    public int getNbJoueur() {
        return this.listeJoueur.size();
    }

    public boolean getLancer() {
        return this.lancer;
    }

    public int getM() {
        return this.m;
    }

    public int getMaxJoueur() {
        return this.maxJoueur;
    }

    public String getPort() {
        return this.port;
    }

    public String getID() {
        return this.id;
    }

    public void setPort( String nouveau ) {
        this.port = nouveau;
    }

    public void setM( int nouveau ) {
        this.m = nouveau;
    }

    public void setID( String nouveau ) {
        this.id = nouveau;
    }

    public void setLancer( boolean nouveau ) {
        this.lancer = nouveau;
    }

    public void setMaxJoueur( int nouveau ) {
        this.maxJoueur = nouveau;
    }

    public ArrayList<Joueur> getListeJoueur() {
        return this.listeJoueur;
    }

    public boolean ajouterJoueur( Joueur nouveau ) {
        if( this.listeJoueur.contains( nouveau ) == false && this.getNbJoueur() < this.getMaxJoueur() && !this.lancer ) {
            this.listeJoueur.add( nouveau );
            return true;
        } else {
            return false;
        }
    }

    public boolean tousPret() {
        for( int i = 0; i < this.listeJoueur.size(); i++ ) {
            if( this.listeJoueur.get( i ).getReady() == false ) {
                System.out.println( this.listeJoueur.get( i ).getPseudo() + " is not ready yet" );
                return false;
            }
        }
        return true;
    }

    public void retirerJoueur( Joueur supr ) {
        if( this.listeJoueur.contains( supr ) ) {
            this.listeJoueur.remove( supr );
        } else {
            System.out.println( "Joueur absent" );
        }
    }

    public static String getNbPartie( ArrayList<Partie> liste ) {
        int nb = 0;
        for( int i = 0; i < liste.size(); i++ ) {
            if( liste.get( i ).lancer == false && liste.get( i ).getNbJoueur() > 0 )
                nb++;
        }
        return String.valueOf( nb );
    }

    public static void envoyerListePartie( Joueur x, ArrayList<Partie> liste ) {
        try {
            for( int i = 0; i < liste.size(); i++ ) {
                if( liste.get( i ).lancer == false && liste.get( i ).getNbJoueur() > 0 ) {
                    PrintWriter pw = new PrintWriter( x.getSocket().getOutputStream() );
                    pw.write( "OGAME " + liste.get( i ).getID() + " " + liste.get( i ).getM() + " " + liste.get( i ).getNbJoueur() + "***" );
                    pw.flush();
                }

            }
        } catch( Exception e ) {
            e.printStackTrace();
        }
    }

    public void listePartie( PrintWriter pw ) {
        pw.write( "LIST! " + this.getM() + " " + this.listeJoueur.size() + "***" );
        for( int i = 0; i < this.listeJoueur.size(); i++ ) {
            pw.write( "PLAYR " + this.listeJoueur.get( i ).getPseudo() + "***" );
            pw.flush();
        }
    }

    public void launchGame() {

        //boucle for qui envoie msg de lancement ou broadcast ?
    }


}
