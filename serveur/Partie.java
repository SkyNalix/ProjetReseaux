package serveur;

import java.io.*;

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.LinkedList;

public class Partie {

	private final int id;
	private final InetSocketAddress address; // ip et port multicast
	private final LinkedList<Joueur> listeJoueur = new LinkedList<>();
	private boolean lancer = false;
	private Game game;
	private final int hauteur = 10, largeur = 10;

	public Partie() {
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

	public Game getGame() {
		return this.game;
	}

	public int getNbJoueur() {
		return this.listeJoueur.size();
	}

	public boolean getLancer() {
		return this.lancer;
	}

	public int getID() {
		return this.id;
	}

	public LinkedList<Joueur> getListeJoueur() {
		return this.listeJoueur;
	}

	public boolean ajouterJoueur( Joueur nouveau ) {
		if( this.lancer )
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
			if( game != null ) {
				game.lab.getLabyrinthe()[supr.getPosition().getX()][supr.getPosition().getY()] = 0;
				game.refresh();
			}
			if( getNbJoueur() == 0 ) {
				stopGame();
			} else if( !lancer && tousPret() ) {
				launchGame();
			}
		}
	}

	public void listePartie( PrintWriter pw ) {
		pw.write( "LIST! " + this.getID() + " " + Converter.int8ToHexString( this.listeJoueur.size() ) + "***" );
		for( Joueur joueur : this.listeJoueur ) {
			pw.write( "PLAYR " + joueur.getPseudo() + "***" );
			pw.flush();
		}
	}

	public void launchGame() {
		new Thread( () -> {
			lancer = true;
			game = new Game( this );

			String s = String.format( "WELCO %d %s %s %s %s %d***",
									  getID(),
									  Converter.int16ToHexString( getHauteur() ),
									  Converter.int16ToHexString( getLargeur() ),
									  Converter.int8ToHexString( getGame().lab.getNbFantomes() ),
									  address.getHostName() + "#".repeat( 15 - address.getHostName().length() ),
									  address.getPort() );
			for( Joueur joueur : listeJoueur ) {
				try {
					PrintWriter pw = new PrintWriter( joueur.getSocket().getOutputStream() );
					pw.write( s );
					pw.flush();
					pw.write( String.format( "POSIT %s %s %s***",
											 joueur.getPseudo(),
											 joueur.getPosition().getXStr(),
											 joueur.getPosition().getYStr() ) );
					pw.flush();
				} catch( Exception e ) {
					e.printStackTrace();
				}
			}
		} ).start();
	}

	public void stopGame() {
		if( this.game != null ) {
			game.sendRanking();
			if( this.game.display != null )
				this.game.display.dispose();
			this.game.go = false;
			for(Joueur j : listeJoueur) {
				try {
					j.getSocket().close();
				} catch( IOException e ) {
					throw new RuntimeException( e );
				}
			}
		}
		Serveur.listePartie.remove( this );
	}

	public static int getNbPartie() {
		int nb = 0;
		for( Partie partie : Serveur.listePartie ) {
			if( !partie.lancer && partie.getNbJoueur() > 0 )
				nb++;
		}
		return nb;
	}

	public static void envoyerListePartie( PrintWriter pw, ArrayList<Partie> liste ) {
		try {
			for( Partie partie : liste ) {
				if( !partie.lancer && partie.getNbJoueur() > 0 ) {
					pw.write( String.format( "OGAME %s %s***",
											 Converter.int8ToHexString( partie.getID() ),
											 Converter.int8ToHexString( partie.getNbJoueur() ) ) );
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
			id = Converter.uint8ToInt( id_str );
		} catch( Exception e ) {
			pw.write( "DUNNO***" ); pw.flush();
			return;
		}
		for( Partie partie : Serveur.listePartie ) {
			if( partie.getID() == id ) {
				partie.listePartie( pw );
				return;
			}
		}
		pw.write( "DUNNO***" ); pw.flush();
	}

	public void multicastMessage( String mess ) {
		try {
			DatagramSocket socket = new DatagramSocket();
			byte[] data = mess.getBytes();
			DatagramPacket paquet = new DatagramPacket( data, data.length, address );
			socket.send( paquet );
			socket.close();
		} catch( Exception e ) {
			e.printStackTrace();
		}
	}

	public InetSocketAddress getAddress() {
		return address;
	}

	public int getHauteur() {
		return hauteur;
	}

	public int getLargeur() {
		return largeur;
	}

}
