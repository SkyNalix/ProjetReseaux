package serveur;

import java.net.*;

import serveur.labyrinthe.Personne;

public class Joueur extends Personne {

	public Connexion connexion;
	private final String pseudo;
	public Partie partie;
	private boolean isReady = false;
	private final int port;
	private int score = 0;

	public Joueur( Connexion connexion, String pseudo, int port ) {
		super( pseudo );
		this.connexion = connexion;
		this.pseudo = pseudo;
		this.port = port;
	}

	public String getPseudo() {
		return this.pseudo;
	}

	public int getPort() {
		return this.port;
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

	public String getScoreStr() { // que pour l'affichage et envoi au client
		String str = score + "";
		return "0".repeat( 4 - str.length() ) + str;
	}

	public void setScore( int score ) {
		this.score = score;
	}

	public boolean chatter( String str ) {

		String[] tab = Utils.splitString( str );
		if( partie == null )
			return false;
		String id = tab[1];
		String msg = "";
		for( int i = 2; i < tab.length; i++ ) {
			msg += tab[i] + ( i < tab.length - 1 ? " " : "" );
		}

		if( Serveur.debug )
			System.out.printf( "%s envoi MP a %s : %s", getPseudo(), id, msg );
		for( Joueur joueur : partie.getListeJoueur() ) {
			if( joueur.getPseudo().equals( id ) ) {
				try {
					//String s = "Envoi";
					byte[] data = Converter.convert( String.format(
							  "MESSP %s %s+++", getPseudo(), msg
																  ) );
					DatagramPacket paquet = new DatagramPacket( data, data.length,
																joueur.connexion.socket.getInetAddress(),
																joueur.port
					);
					DatagramSocket z = new DatagramSocket();
					z.send( paquet );
					z.close();
					return true;
				} catch( Exception e ) {
					e.printStackTrace();
				}
			}
		}
		return false;
	}


}
