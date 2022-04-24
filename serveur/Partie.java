package serveur;

import serveur.labyrinthe.Labyrinthe;

import java.io.*;

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetSocketAddress;
import java.util.ArrayList;

public class Partie {


	private final int id;
	private final InetSocketAddress address; // ip et port multicast
	private final ArrayList<Joueur> listeJoueur = new ArrayList<>();
	private int maxJoueur;
	private boolean lancer = false;
	private Game game;

	public Partie() {
		this( Integer.MAX_VALUE );
	}

	public Partie( int maxJoueur ) {
		this.maxJoueur = maxJoueur;
		int id = 0;
		while( true ) { // cherche un id de partie qui n'est pas pris
			boolean found = false;
			for( Partie partie : Serveur.listePartie )
				if( partie.getID() == id ) {
					id++;
				}
			if( !found ) break;
			id++;
		}
		this.id = id;
		int port = 5144;
		while( true ) { // cherche un port qui n'est pas pris
			boolean found = false;
			for( Partie partie : Serveur.listePartie )
				if( partie.getID() == port ) {
					port++;
				}
			if( !found ) break;
			port++;
		}
		address = new InetSocketAddress( "224.42.51.44", port );
		Serveur.listePartie.add( this );
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

	public int getMaxJoueur() {
		return this.maxJoueur;
	}

	public int getID() {
		return this.id;
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
		if( this.getNbJoueur() == this.getMaxJoueur() || this.lancer )
			return false;
		for( Joueur joueur : this.listeJoueur ) {
			if( joueur.getPseudo().equals( nouveau.getPseudo() ) )
				return false;
		}
		this.listeJoueur.add( nouveau );
		return true;
	}

	public boolean tousPret() {
		for( Joueur joueur : this.listeJoueur ) {
			if( !joueur.getReady() ) {
				System.out.println( joueur.getPseudo() + " is not ready yet" );
				return false;
			}
		}
		return true;
	}

	public void retirerJoueur( Joueur supr ) {
		if( this.listeJoueur.contains( supr ) ) {
			this.listeJoueur.remove( supr );
			if( getNbJoueur() == 0 )
				Serveur.listePartie.remove( this );
			else if( !lancer && tousPret() ) {
				launchGame();
				System.out.println( "tout le monde est prêt" );
			}
		} else {
			System.out.println( "Joueur absent" );
		}
	}

	public void listePartie( PrintWriter pw ) {
		pw.write( "LIST! " + this.getID() + " " + this.listeJoueur.size() + "***" );
		for( Joueur joueur : this.listeJoueur ) {
			pw.write( "PLAYR " + joueur.getPseudo() + "***" );
			pw.flush();
		}
	}

	public void launchGame() {
		lancer = true;
		game = new Game( listeJoueur, new Labyrinthe( 10, 10, listeJoueur ), true );
		String s = String.format( "WELCO %d %d %d %d %s %d***",
								  getID(),
								  getGame().lab.getHauteur(),
								  getGame().lab.getLargeur(),
								  getGame().lab.getNbFantomes(),
								  address.getHostName() + "#".repeat( 15 - address.getHostName().length() ),
								  address.getPort() );
		for( Joueur joueur : listeJoueur ) {
			try {
				PrintWriter pw = new PrintWriter( joueur.getSocket().getOutputStream() );
				pw.write( s );
				pw.flush();
				String str_x = joueur.getPosition().getX() + "";
				str_x = "0".repeat( 3 - str_x.length() ) + str_x;
				String str_y = joueur.getPosition().getY() + "";
				str_y = "0".repeat( 3 - str_y.length() ) + str_y;
				pw.write( String.format( "POSIT %s %s %s***", joueur.getPseudo(), str_x, str_y ) );
				pw.flush();
			} catch( Exception e ) {
				e.printStackTrace();
			}
		}
	}

	public static String getNbPartie( ArrayList<Partie> liste ) {
		int nb = 0;
		for( Partie partie : liste ) {
			if( !partie.lancer && partie.getNbJoueur() > 0 )
				nb++;
		}
		return String.valueOf( nb );
	}

	public static void envoyerListePartie( PrintWriter pw, ArrayList<Partie> liste ) {
		try {
			for( Partie partie : liste ) {
				if( !partie.lancer && partie.getNbJoueur() > 0 ) {
					pw.write( "OGAME " + partie.getID() + " " + partie.getNbJoueur() + "***" );
					pw.flush();
				}
			}
		} catch( Exception e ) {
			e.printStackTrace();
		}
	}

	public static void listePartie( String x, PrintWriter pw ) {
		String id_str = Utils.splitString( x )[1];
		int id;
		try {
			id = Integer.parseInt( id_str );
		} catch( Exception e ) {
			pw.write( "DUNNO***" ); pw.flush();
			return;
		}
		System.out.println( "###### " + id );
		for( Partie partie : Serveur.listePartie ) {
			if( partie.getID() == id ) {
				System.out.println( "found partie" );
				partie.listePartie( pw );
				return;
			}
		}
		pw.write( "DUNNO***" ); pw.flush();
	}


	public InetSocketAddress getAddress() {
		return address;
	}

}
