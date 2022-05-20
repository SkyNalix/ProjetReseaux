package serveur;

import java.io.*;
import java.net.*;
import java.util.ArrayList;

public class Serveur implements Runnable {

	private final Connexion connexion;

	public Serveur( Connexion connexion ) {
		this.connexion = connexion;
	}

	public static ArrayList<Partie> listePartie = new ArrayList<>();

	@Override
	public void run() {
		try {

			//connexione des listes de partie
			Partie.envoyerListePartie( connexion );

			Joueur joueur = null;

			while( true ) { //boucle pour commande
				String x = null;
				try {
					x = connexion.lire();
				} catch( Exception e ) {
					e.printStackTrace();
				}
				if( x == null || this.connexion.socket.isClosed() ) {
					if( joueur != null ) {
						System.out.println( "|" + joueur.getPseudo() + " has disconnected|" );
						joueur.getPartie().retirerJoueur( joueur );
					}
					return;
				}

				System.out.println( "|" + x + "|" );
				try {
					if( x.equals( "GAME?***" ) ) { //GAMES*** affiche le nb de partie non lancé
						Partie.envoyerListePartie( connexion );
					} else if( x.startsWith( "NEWPL" ) && x.endsWith( "***" ) ) {
						if( joueur != null ) {
							connexion.write( Converter.convert( "REGNO***" ) );
						} else {
							joueur = Message.newpl( connexion, x );

							if( joueur == null ) {
								connexion.write( Converter.convert( "REGNO***" ) );
							} else {
								connexion.write( Converter.convert( "REGOK",
																	new Nombre( joueur.getPartie().getID(), 1 )
										  , "***" ) );

							}
						}

					} else if( x.startsWith( "REGIS" ) && x.endsWith( "***" ) && joueur == null ) {
						joueur = Message.regis( connexion, x );
						if( joueur != null ) {  //REGIS id port m***
							connexion.write( Converter.convert( "REGOK", new Nombre( joueur.getPartie().getID(), 1 ), "***" ) );
						} else {
							connexion.write( Converter.convert( "REGNO***" ) );
						}
					} else if( x.equals( "UNREG***" ) && joueur != null ) { //UNREG*** quitte la partie
						connexion.write( Converter.convert( "UNROK",
															new Nombre( joueur.getPartie().getID(), 1 ), "***" ) );

						joueur.getPartie().retirerJoueur( joueur );
						joueur = null;
					} else if( x.equals( "START***" ) && joueur != null ) {
						joueur.setReady( true );
						if( joueur.getPartie().tousPret() ) {
							joueur.getPartie().launchGame();
						}
					} else if( x.startsWith( "LIST?" ) && x.endsWith( "***" ) ) { //LIST? numPartie*** affiche les joueurs de la
						Message.list( x, connexion );                      // partie demandé

					} else if( x.startsWith( "SEND?" ) && x.endsWith( "***" ) && joueur != null ) {
						if( joueur.chatter( x ) ) {
							connexion.write( Converter.convert( "SEND!***" ) );
						} else {
							connexion.write( Converter.convert( "NSEND***" ) );
						}

					} else if( x.startsWith( "DISC!" ) && x.endsWith( "***" ) && joueur == null ) { // se deconnecte
						connexion.write( Converter.convert( "GOBYE***" ) );
						connexion.socket.close();
						return;
					} else if( x.startsWith( "SIZE? " ) && x.endsWith( "***" ) ) {
						int m;
						try {
							m = Integer.parseInt( Utils.splitString( x )[1] );
						} catch( Exception e ) {
							connexion.write( Converter.convert( "DUNNO***" ) );
							continue;
						}
						Partie p = null;
						for( Partie partie : listePartie ) {
							if( m == partie.getID() )
								p = partie;
						}

						if( p == null ) {
							connexion.write( Converter.convert( "DUNNO***" ) );
						} else {
							connexion.write( Converter.convert( "SIZE!",
																new Nombre( p.getID(), 1 ),
																new Nombre( p.getHauteur(), 2 ),
																new Nombre( p.getLargeur(), 2 ),
																"***" ) );

						}

					} else if( x.startsWith( "UPMOV" ) && x.endsWith( "***" ) && joueur != null && joueur.getPartie() != null && joueur.getPartie().getLancer() ) { //mouvement
						int pas;
						try {
							pas = Integer.parseInt( Utils.splitString( x )[1] );
						} catch( Exception e ) {
							connexion.write( Converter.convert( "DUNNO***" ) );
							continue;
						}
						int score_avant = joueur.getScore();
						joueur.getPartie().getGame().lab.moveUp( joueur, pas );
						joueur.getPartie().getGame().refresh();
						joueur.getPartie().getGame().posJoueursUpdate();

						if( score_avant == joueur.getScore() ) {
							connexion.write( Converter.convert( "MOVE!",
																joueur.getPosition().getXStr(),
																joueur.getPosition().getYStr(),
																"***" ) );

						} else {
							connexion.write( Converter.convert( String.format( "MOVEF %s %s %s***",
																			   joueur.getPosition().getXStr(),
																			   joueur.getPosition().getYStr(),
																			   joueur.getScoreStr() ) ) );

						}

					} else if( x.startsWith( "DOMOV" ) && x.endsWith( "***" ) && joueur != null && joueur.getPartie() != null && joueur.getPartie().getLancer() ) { //mouvement
						int pas;
						try {
							pas = Integer.parseInt( Utils.splitString( x )[1] );
						} catch( Exception e ) {
							connexion.write( Converter.convert( "DUNNO***" ) );
							continue;
						}
						int score_avant = joueur.getScore();
						joueur.getPartie().getGame().lab.moveDown( joueur, pas );
						joueur.getPartie().getGame().refresh();
						joueur.getPartie().getGame().posJoueursUpdate();

						if( score_avant == joueur.getScore() ) {
							connexion.write( Converter.convert( String.format( "MOVE! %s %s***", joueur.getPosition().getXStr(), joueur.getPosition().getYStr() ) ) );

						} else {
							connexion.write( Converter.convert( String.format( "MOVEF %s %s %s***",
																			   joueur.getPosition().getXStr(),
																			   joueur.getPosition().getYStr(),
																			   joueur.getScoreStr() ) ) );

						}

					} else if( x.startsWith( "RIMOV" ) && x.endsWith( "***" ) && joueur != null && joueur.getPartie() != null && joueur.getPartie().getLancer() ) { //mouvement
						int pas;
						try {
							pas = Integer.parseInt( Utils.splitString( x )[1] );
						} catch( Exception e ) {
							connexion.write( Converter.convert( "DUNNO***" ) );
							continue;
						}
						int score_avant = joueur.getScore();
						joueur.getPartie().getGame().lab.moveRight( joueur, pas );
						joueur.getPartie().getGame().refresh();
						joueur.getPartie().getGame().posJoueursUpdate();

						if( score_avant == joueur.getScore() ) {
							connexion.write( Converter.convert( String.format( "MOVE! %s %s***",
																			   joueur.getPosition().getXStr(),
																			   joueur.getPosition().getYStr() ) ) );

						} else {
							connexion.write( Converter.convert( String.format( "MOVEF %s %s %s***",
																			   joueur.getPosition().getXStr(),
																			   joueur.getPosition().getYStr(),
																			   joueur.getScoreStr() ) ) );

						}
					} else if( x.startsWith( "LEMOV" ) && x.endsWith( "***" ) && joueur != null && joueur.getPartie() != null && joueur.getPartie().getLancer() ) { //mouvement
						int pas;
						try {
							pas = Integer.parseInt( Utils.splitString( x )[1] );
						} catch( Exception e ) {
							connexion.write( Converter.convert( "DUNNO***" ) );
							continue;
						}
						int score_avant = joueur.getScore();
						joueur.getPartie().getGame().lab.moveLeft( joueur, pas );
						joueur.getPartie().getGame().refresh();
						joueur.getPartie().getGame().posJoueursUpdate();

						if( score_avant == joueur.getScore() ) {
							connexion.write( Converter.convert( String.format( "MOVE! %s %s***",
																			   joueur.getPosition().getXStr(),
																			   joueur.getPosition().getYStr() ) ) );

						}
						// envoi de MOVEF se passe dans Labyrinthe.elimineFantome()

					} else if( x.equals( "IQUIT***" ) && joueur != null && joueur.getPartie() != null && joueur.getPartie().getLancer() ) {
						Partie p = joueur.getPartie();
						p.retirerJoueur( joueur );
						//end game
						if( p.getNbJoueur() == 0 ) {
							p.getGame().fantomeMove.interrupt();
						}
						connexion.write( Converter.convert( "GOBYE***" ) );
						connexion.socket.close();
						return;
					} else if( x.equals( "GLIS?***" ) && joueur != null && joueur.getPartie() != null && joueur.getPartie().getLancer() ) {
						connexion.write( Converter.convert(
								  "GLIS!",
								  new Nombre( joueur.getPartie().getNbJoueur(), 1 ),
								  "***" ) );

						for( Joueur j : joueur.getPartie().getListeJoueur() ) { // [GPLYR␣id␣x␣y␣p]
							connexion.write( Converter.convert( String.format( "GPLYR %s %s %s %s***",
																			   j.getPseudo(),
																			   j.getPosition().getXStr(),
																			   j.getPosition().getYStr(),
																			   j.getScoreStr()
																			 ) ) );

						}
					} else if( x.startsWith( "MALL?" ) && joueur != null && joueur.getPartie().getLancer() ) {
						String str = x.substring( 6, x.length() - 3 );
						Connexion.sendUDP( joueur.getPartie().getAddress(),
										   Converter.convert( String.format( "MESSA %s %s+++",
																			 joueur.getPseudo(), str ) ) );
						connexion.write( Converter.convert( "MALL!***" ) );
					} else {
						connexion.write( Converter.convert( "DUNNO***" ) );
					}
				} catch( Exception ignored ) {
					connexion.write( Converter.convert( "DUNNO***" ) );
				}
			}

		} catch( SocketException ignored ) {
		} catch( Exception e ) {
			try {
				connexion.socket.close();
			} catch( IOException ignored ) {
			}
		}
	}

	public static boolean debug = false; // print debug info
	public static boolean nogui = false;

	public static void main( String[] args ) {
		for( String arg : args ) {
			if( arg.equals( "--debug" ) )
				debug = true;
			if( arg.equals( "--nogui" ) )
				nogui = true;
		}

		try {
			Joueur j1 = new Joueur( null, "joueur01", 4242 );
			Joueur j2 = new Joueur( null, "joueur02", 4243 );
			Joueur j3 = new Joueur( null, "joueur03", 4244 );

			j1.setReady( true );
			j2.setReady( true );
			j3.setReady( true );

			Partie test1 = new Partie();
			Partie test2 = new Partie();
			test1.ajouterJoueur( j1 ); test1.ajouterJoueur( j2 );
			test2.ajouterJoueur( j3 );

			ServerSocket servSocket = null;
			int port = 4243;
			while( servSocket == null ) {
				try {
					servSocket = new ServerSocket( port );
				} catch( Exception e ) {
					port++;
				}
			}
			System.out.println( "lancé sur le port " + port );
			while( true ) {
				Socket socket = servSocket.accept();
				try {
					Serveur serveurRunnable = new Serveur( new Connexion( socket ) );
					Thread t = new Thread( serveurRunnable );
					synchronized( t ) {
						t.start();
					}
				} catch( IOException e ) {
					socket.close();
				}
			}
		} catch( Exception e ) {
			e.printStackTrace();
		}
	}

}
