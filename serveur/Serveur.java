package serveur;

import java.io.*;
import java.net.*;
import java.util.ArrayList;

public class Serveur implements Runnable {

	private final Socket socket;

	public Serveur( Socket socket ) {
		this.socket = socket;
	}

	public static ArrayList<Partie> listePartie = new ArrayList<>();


	@Override
	public void run() {
		try {
			BufferedReader br = new BufferedReader( new InputStreamReader( this.socket.getInputStream() ) );
			PrintWriter pw = new PrintWriter( this.socket.getOutputStream() );
			//envoie des listes de partie
			pw.write( "GAMES " + Partie.getNbPartie() + "***" );
			pw.flush();
			//envoie liste partie avec id
			Partie.envoyerListePartie( pw, listePartie );

			Joueur joueur = null;

			while( true ) { //boucle pour commande
				Thread.sleep( 1000 );
				String x = Utils.lecture2( br );
				if( this.socket.isClosed() ) {
					return;
				}
				System.out.println( "|" + x + "|" );

				if( x.equals( "GAME?***" ) ) { //GAMES*** affiche le nb de partie non lancé
					pw.write( "GAMES " + Partie.getNbPartie() + "***" ); pw.flush();
					Partie.envoyerListePartie( pw, listePartie );
				} else if( x.startsWith( "NEWPL" ) && x.endsWith( "***" ) ) {
					if( joueur != null ) {
						pw.write( "REGNO***" ); pw.flush();
					} else {
						// NEWPL id port*** crée une partie (id = id joueur)
						joueur = Joueur.newpl( this.socket, x );
						if( joueur == null ) {
							pw.write( "REGNO***" ); pw.flush();
						} else {
							pw.write( "REGOK " + joueur.getPartie().getID() + "***" ); pw.flush();
						}
					}

				} else if( x.startsWith( "REGIS" ) && x.endsWith( "***" ) && joueur == null ) {
					joueur = Joueur.rejoindrePartie( this.socket, x );
					if( joueur != null ) {  //REGIS id port m***
						pw.write( "REGOK " + joueur.getPartie().getID() + "***" ); pw.flush();
					} else {
						pw.write( "REGNO***" ); pw.flush();
					}

				} else if( x.equals( "UNREG***" ) && joueur != null ) { //UNREG*** quitte la partie
					pw.write( "UNROK" + " " + joueur.getPartie().getID() + "***" ); pw.flush();
					joueur.getPartie().retirerJoueur( joueur );
					joueur = null;
				} else if( x.equals( "START***" ) && joueur != null ) { //START*** bloque dans la game,attends le lancement
					joueur.setReady( true );
					System.out.println( joueur.getPseudo() + " est prêt" );
					if( joueur.getPartie().tousPret() ) {
						joueur.getPartie().launchGame();
					}

				} else if( x.startsWith( "LIST?" ) && x.endsWith( "***" ) ) { //LIST? numPartie*** affiche les joueurs de la
					Partie.listePartie( x, pw );                      // partie demandé

				} else if( x.startsWith( "SEND?" ) && x.endsWith( "***" ) && joueur != null && joueur.getPartie().getLancer() ) {
					joueur.chatter( x );
				} else if( x.startsWith( "DISC!" ) && x.endsWith( "***" ) && joueur == null ) { // se deconnecte
					socket.close();
					return;
				} else if( x.startsWith( "SETPS" ) && x.endsWith( "***" ) && joueur != null && !joueur.getReady() ) {//envie de changer de pseudo
					x = x.substring( 6, x.length() - 3 );
					if( x.length() != 8 ) {
						pw.write( "REGNO***" );
						pw.flush();
					} else {
						joueur.setPseudo( x );
					}
				} else if( x.equals( "-1" ) ) {//si le joueur a crash
					if( joueur != null ) {
						joueur.getPartie().retirerJoueur( joueur );
						System.out.println( "|" + joueur.getPseudo() + " has disconnected|" );
					}
					return;
				} else if( x.startsWith( "SIZE? " ) && x.endsWith( "***" ) ) {
					int m;
					try {
						m = Integer.parseInt( Utils.splitString( x )[1] );
					} catch( Exception e ) {
						pw.write( "DUNNO***" ); pw.flush();
						continue;
					}
					Partie p = null;
					for( Partie partie : listePartie ) {
						if( m == partie.getID() )
							p = partie;
					}

					if( p == null ) {
						pw.write( "DUNNO***" ); pw.flush();
					} else {
						pw.write( "SIZE! " + p.getID() + " "
								  + p.getGame().lab.getHauteur() + " "
								  + p.getGame().lab.getLargeur() );
						pw.flush();
					}

				} else if( x.startsWith( "UPMOV" ) && x.endsWith( "***" ) && joueur != null && joueur.getPartie() != null && joueur.getPartie().getLancer() ) { //mouvement
					int pas;
					try {
						pas = Integer.parseInt( Utils.splitString( x )[1] );
					} catch( Exception e ) {
						pw.write( "DUNNO***" ); pw.flush();
						continue;
					}
					joueur.getPartie().getGame().moveUp( joueur, pas );
					joueur.getPartie().getGame().posJoueursUpdate();
					pw.write( "MOVE! " + joueur.getPosition().getX() + " " + joueur.getPosition().getY() ); pw.flush();

				} else if( x.startsWith( "DOMOV" ) && x.endsWith( "***" ) && joueur != null && joueur.getPartie() != null && joueur.getPartie().getLancer() ) { //mouvement
					int pas;
					try {
						pas = Integer.parseInt( Utils.splitString( x )[1] );
					} catch( Exception e ) {
						pw.write( "DUNNO***" ); pw.flush();
						continue;
					}
					joueur.getPartie().getGame().moveDown( joueur, pas );
					joueur.getPartie().getGame().posJoueursUpdate();
					pw.write( "MOVE! " + joueur.getPosition().getX() + " " + joueur.getPosition().getY() ); pw.flush();

				} else if( x.startsWith( "RIMOV" ) && x.endsWith( "***" ) && joueur != null && joueur.getPartie() != null && joueur.getPartie().getLancer() ) { //mouvement
					int pas;
					try {
						pas = Integer.parseInt( Utils.splitString( x )[1] );
					} catch( Exception e ) {
						pw.write( "DUNNO***" ); pw.flush();
						continue;
					}
					joueur.getPartie().getGame().moveRight( joueur, pas );
					joueur.getPartie().getGame().posJoueursUpdate();
					pw.write( "MOVE! " + joueur.getPosition().getX() + " " + joueur.getPosition().getY() ); pw.flush();

				} else if( x.startsWith( "LEMOV" ) && x.endsWith( "***" ) && joueur != null && joueur.getPartie() != null && joueur.getPartie().getLancer() ) { //mouvement
					int pas;
					try {
						pas = Integer.parseInt( Utils.splitString( x )[1] );
					} catch( Exception e ) {
						pw.write( "DUNNO***" ); pw.flush();
						continue;
					}
					joueur.getPartie().getGame().moveLeft( joueur, pas );
					joueur.getPartie().getGame().posJoueursUpdate();
					pw.write( "MOVE! " + joueur.getPosition().getX() + " " + joueur.getPosition().getY() ); pw.flush();
				} else if( x.equals( "IQUIT***" ) && joueur != null && joueur.getPartie() != null && joueur.getPartie().getLancer() ) {
					Partie p = joueur.getPartie();
					p.retirerJoueur( joueur );
					//end game
					if( p.getNbJoueur() == 0 ) {
						p.getGame().commandControl.interrupt();
						p.getGame().fantomeMove.interrupt();
					}
					pw.write( "GOBYE***" );
					pw.flush();
				} else if( x.equals( "GLIS?***" ) && joueur != null && joueur.getPartie() != null && joueur.getPartie().getLancer() ) {
					pw.write( String.format( "GLIS! %x***", joueur.getPartie().getNbJoueur() ) ); pw.flush();
					for( Joueur joueur1 : joueur.getPartie().getListeJoueur() ) { // [GPLYR␣id␣x␣y␣p]
						String str_x = joueur.getPosition().getX() + "";
						str_x = "0".repeat( 3 - str_x.length() ) + str_x;
						String str_y = joueur.getPosition().getX() + "";
						str_y = "0".repeat( 3 - str_y.length() ) + str_y;
						String str_p = joueur.getScore() + "";
						str_p = "0".repeat( 4 - str_p.length() ) + str_p;
						pw.write( String.format( "GPLYR %s %s %s %s***",
												 joueur1.getPseudo(),
												 str_x,
												 str_y,
												 str_p
											   ) ); pw.flush();
					}
				} else {
					pw.write( "DUNNO***" ); pw.flush();
				}
			}

		} catch( Exception e ) {
			e.printStackTrace();
		}
	}


	public static void main( String[] args ) {
		try {
			Joueur j1 = new Joueur( "joueur01", null, 4242 );
			Joueur j2 = new Joueur( "joueur02", null, 4243 );
			Joueur j3 = new Joueur( "joueur03", null, 4244 );

			j1.setReady( true );
			j2.setReady( true );
			j3.setReady( true );

			Partie test1 = new Partie( 32 );
			Partie test2 = new Partie( 32 );
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
				Thread t = new Thread( new Serveur( socket ) );
				synchronized( t ) {
					t.start();
				}
			}
		} catch( Exception e ) {
			e.printStackTrace();
		}
	}

}
