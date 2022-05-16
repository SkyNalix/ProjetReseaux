package serveur;

import java.io.*;
import java.net.*;

import serveur.labyrinthe.Labyrinthe;
import serveur.labyrinthe.Personne;

public class Joueur extends Personne {

	private final String pseudo;
	private final Socket socketAssocie;
	public Partie partie;
	private boolean isReady = false;
	private final int port;
	private int score = 0;

	public Joueur( String pseudo, Socket socketAssocie, int port ) {
		super( pseudo );
		this.pseudo = pseudo;
		this.socketAssocie = socketAssocie;
		this.port = port;
	}

	public String getPseudo() {
		return this.pseudo;
	}

	public int getPort() {
		return this.port;
	}

	public Socket getSocket() {
		return this.socketAssocie;
	}

	public Partie getPartie() {
		return this.partie;
	}

	public boolean getReady() {
		return this.isReady;
	}

	public void setReady( boolean nouveau ) {
		this.isReady = nouveau;
	}

	public int getScore() {
		return score;
	}

	public void setScore( int score ) {
		this.score = score;
	}

	@Override
	public boolean isFantome() {
		return false;
	}

	public static Joueur newpl( Socket socket, String str ) {
		//on decrypte le str de la Forme NEWPL id port***
		String[] split = Utils.splitString( str );
		String id = split[1];
		String port_str = split[2];
		int port;
		try {
			port = Integer.parseInt( port_str );
			for(int i =0 ; i < Serveur.listePartie.size();i++){
				for(int j = 0; j < Serveur.listePartie.get(i).getListeJoueur().size();j++){
					if(port == Serveur.listePartie.get(i).getListeJoueur().get(j).getPort()){
						return null;
					}
				}
			}
		} catch( Exception e ) {
			return null;
		}
		if( id.length() != 8 || port_str.length() != 4 )
			return null;
		Joueur joueur = new Joueur( id, socket,port);
		Partie partie = new Partie();
		partie.ajouterJoueur( joueur );
		joueur.partie = partie;
		return joueur;
	}

	public static Joueur rejoindrePartie( Socket socket, String str ) {
		//  [REGIS␣id␣port␣m***]
		String[] split = Utils.splitString( str );
		String id = split[1];
		String port_str = split[2];
		String partie_id_str = split[3];
		if( id.length() != 8 || port_str.length() != 4 ) {
			return null;
		}
		int port;
		int partie_id;
		try {
			port = Integer.parseInt( port_str );
			for(int i =0 ; i < Serveur.listePartie.size();i++){
				for(int j = 0; j < Serveur.listePartie.get(i).getListeJoueur().size();j++){
					if(port == Serveur.listePartie.get(i).getListeJoueur().get(j).getPort()){
						return null;
					}
				}
			}
			partie_id = Integer.parseInt( partie_id_str );
		} catch( Exception e ) {
			return null;
		}
		Joueur joueur = new Joueur( id, socket, port );
		for( Partie partie : Serveur.listePartie ) {
			if( partie.getID() == partie_id ) {
				if( partie.ajouterJoueur( joueur ) ) {
					System.out.println( "--- DEBUG joueur ajouté" );
					joueur.partie = partie;
					return joueur;
				} else {
					System.out.println( "--- DEBUG pas joueur ajouté" );
					return null;
				}
			}
		}
		return null; // partie non trouvée
	}



	public boolean chatter( String str ) {
		
		String[] tab = Utils.splitString(str);
		if( partie == null )
			return false;
		String id = tab[1];
		String msg = "";
		for(int i = 2;i < tab.length;i++){
			msg += tab[i] + " ";
		}

		System.out.println( "id|" + id + "|");
		for( Joueur joueur : partie.getListeJoueur() ) {
			if( joueur.getPseudo().equals( id ) ) {
				try {

					//String s = "Envoi";
					byte[] data = (this.getPseudo() + ": " + msg).getBytes();
					DatagramPacket paquet = new DatagramPacket( data, data.length,
																joueur.socketAssocie.getInetAddress(),
																joueur.port
					);
					DatagramSocket z = new DatagramSocket();
					z.send(paquet);
					z.close();
					//new DatagramSocket().send( paquet );
					System.out.println("msg envoyé à " + joueur.getPseudo() + " sur " + joueur.getPort());
					return true;
				} catch( Exception e ) {
					e.printStackTrace();
				}
			}
		}
		return false;
	}
	

}
