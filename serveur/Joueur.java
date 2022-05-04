package serveur;

import java.io.*;
import java.net.*;

import serveur.labyrinthe.Labyrinthe;
import serveur.labyrinthe.Personne;

public class Joueur extends Personne {

	private final String pseudo;
	private final Socket socketAssocie;
	private Partie partie;
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

	public void envoyerMessage( String msg ) {
		try {
			PrintWriter pw = new PrintWriter( this.socketAssocie.getOutputStream() );
			pw.write( msg );
			pw.flush();
		} catch( Exception e ) {
			e.printStackTrace();
		}
	}

	public void creationProfile() {
		try {
			BufferedReader br = new BufferedReader( new InputStreamReader( this.socketAssocie.getInputStream() ) );
			PrintWriter pw = new PrintWriter( this.socketAssocie.getOutputStream() );
			while( true ) {
				if( this.getPseudo().length() == 8 ) {
					return;
				} else {
					pw.write( "Pseudo invalide veuillez recommencer" );
					pw.flush();
					this.setPseudo( br.readLine() );
				}
			}
		} catch( Exception e ) {
			e.printStackTrace();
		}
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
		Partie partie = new Partie( 32 );
		partie.ajouterJoueur( joueur );
		Labyrinthe lab = new Labyrinthe( 10, 10, partie.getArrayJoueur() );
		Game g = new Game( lab, false );
		partie.setGame( g );
		joueur.partie = partie;
		return joueur;
	}

	public boolean connexionSimple( String nomPartie ) {
		if( this.getPartie() != null ) {
			return false;
		}
		String res = "";
		for( int i = 8; i < nomPartie.length() - 3; i++ ) {
			res += Character.toString( nomPartie.charAt( i ) );
		}
		for( int i = 0; i < Serveur.listePartie.size(); i++ ) {
			if( Serveur.listePartie.get( i ).getID() == Integer.parseInt( res ) ) {
				if( Serveur.listePartie.get( i ).ajouterJoueur( this ) ) {
					this.partie = Serveur.listePartie.get( i );
					return true;
				}
			}
		}
		return false;
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

	public void inviterPersonne( String pseudo ) {
		// TODO
	}


	public boolean chatter( String str ) {
		if( partie == null )
			return false;
		String id = ""; int n = 0; String msg = "";
		for( int i = 6; str.charAt(i) != ' '; i++ ) {
			id += str.charAt(i);
			n = i;
		}
		for(int j = n;j < str.length()-3;j++){
			msg += str.charAt(j);
		}

		System.out.println( "id|" + id + "|");
		for( Joueur joueur : partie.getListeJoueur() ) {
			if( joueur.getPseudo().equals( id ) ) {
				try {

					//String s = "Envoi";
					byte[] data = (msg + this.getPseudo()).getBytes();
					DatagramPacket paquet = new DatagramPacket( data, data.length,
																joueur.socketAssocie.getInetAddress(),
																joueur.socketAssocie.getPort()
					);
					new DatagramSocket().send( paquet );
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
