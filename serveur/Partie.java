package serveur;

import java.io.*;

import java.net.InetSocketAddress;
import java.util.LinkedList;

public class Partie {

	private final int id;
	private final InetSocketAddress address; // ip et port multicast
	private final LinkedList<Joueur> listeJoueur = new LinkedList<>();
	private boolean lancer = false;
	private Jeu jeu;
	private final int hauteur = 10, largeur = 10;

	public Partie() {
		int id = 0;
		while( true ) { // cherche un id de partie qui n'est pas pris
			boolean found = false;
			for( Partie partie : Serveur.listePartie ) {
				if( partie.getID() == id ) {
					found = true;
					break;
				}
			}
			if( !found ) break;
			id++;
		}
		this.id = id;
		int port = 5144;
		while( true ) { // cherche un port qui n'est pas pris
			boolean found = false;
			for( Partie partie : Serveur.listePartie ) {
				if( partie.getID() == port ) {
					found = true;
					break;
				}
			}
			if( !found ) break;
			port++;
		}
		address = new InetSocketAddress( "224.42.51.44", port );
		Serveur.listePartie.add( this );
	}

	public Jeu getGame() {
		return this.jeu;
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

	private String getPortStr() {
		String str = address.getPort() + "";
		str = "0".repeat( 4 - str.length() ) + str;
		return str;
	}

	private String getIPStr() {
		return address.getHostName() + "#".repeat( 15 - address.getHostName().length() );
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
				return false;
			}
		}
		return true;
	}

	public void retirerJoueur( Joueur joueur ) {
		if( this.listeJoueur.contains( joueur ) ) {
			this.listeJoueur.remove( joueur );
			if( jeu != null ) {
				jeu.lab.getLabyrinthe()[joueur.getPosition().getX()][joueur.getPosition().getY()] = 0;
				jeu.refresh();
			}
			if( getNbJoueur() == 0 ) {
				supprPartie();
			} else if( !lancer && tousPret() ) {
				launchGame();
			}
		}
	}

	public void launchGame() {
		lancer = true;
		jeu = new Jeu( this );
		byte[] welco = Converter.convert( "WELCO",
										  new Nombre( getID(), 1 ),
										  new Nombre( getHauteur(), 2 ),
										  new Nombre( getLargeur(), 2 ),
										  new Nombre( getGame().lab.getNbFantomes(), 1 ),
										  getIPStr(),
										  getPortStr(),
										  "***" );
		for( Joueur joueur : listeJoueur ) {
			try {
				joueur.connexion.write( welco );
				String posit = String.format( "POSIT %s %s %s***",
											  joueur.getPseudo(),
											  joueur.getPosition().getXStr(),
											  joueur.getPosition().getYStr() );
				joueur.connexion.write( Converter.convert( posit ) );
			} catch( Exception e ) {
				e.printStackTrace();
			}
		}
	}


	public void supprPartie() {
		if( this.jeu != null ) {
			if( this.jeu.affichage != null )
				this.jeu.affichage.dispose();
			this.jeu.go = false;
			Joueur gagnant = null;
			for( Joueur j : listeJoueur ) {
				if( gagnant == null || ( j != null && j.getScore() > gagnant.getScore() ) )
					gagnant = j;
			}
			if( gagnant != null ) {
				byte[] endga = Converter.convert(
						  String.format( "ENDGA %s %s+++", gagnant.getPseudo(), gagnant.getScoreStr() )
												);
				for( Joueur j : listeJoueur ) {
					try {
						Connexion.sendUDP( getAddress(), endga );
						j.connexion.socket.close();
					} catch( IOException ignored ) {
					}
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

	public static void envoyerListePartie( Connexion connexion ) {
		try {
			connexion.write( Converter.convert( "GAMES", new Nombre( Partie.getNbPartie(), 1 ), "***" ) );
			for( Partie partie : Serveur.listePartie ) {
				if( !partie.lancer && partie.getNbJoueur() > 0 ) {
					byte[] b = Converter.convert( "OGAME",
												  new Nombre( partie.getID(), 1 ),
												  new Nombre( partie.getNbJoueur(), 1 ),
												  "***" );
					connexion.write( b );
				}
			}
		} catch( Exception e ) {
			e.printStackTrace();
		}
	}


	public void listePartie( Connexion connexion ) throws IOException {
		connexion.write( Converter.convert(
				  "LIST!",
				  new Nombre( getID(), 1 ),
				  new Nombre( this.listeJoueur.size(), 1 ),
				  "***"
										  ) );
		for( Joueur joueur : this.listeJoueur ) {
			connexion.write( Converter.convert( String.format( "PLAYR %s***", joueur.getPseudo() ) ) );
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
