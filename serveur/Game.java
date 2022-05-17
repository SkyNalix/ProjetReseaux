package serveur;


import java.io.IOException;
import java.util.*;

import serveur.gui.Display;
import serveur.labyrinthe.*;
import static java.lang.Thread.sleep;

public class Game {

	public Partie partie;
	public LinkedList<Joueur> joueurs;
	public Labyrinthe lab;
	public Display display;

	public Game( Partie partie) {
		this.partie = partie;
		this.joueurs = partie.getListeJoueur();
		this.lab = new Labyrinthe( this );
		if( !Serveur.nogui )
			this.display = new Display( this.lab.getLabyrinthe() );
		fantomeMove.start();
	}

	public void refresh() {
		if( !Serveur.nogui )
			display.updateContent( this.lab.getLabyrinthe() );
		else
			lab.print();
	}

	public void moveUp( Joueur joueur, int pas ) {
		this.lab.moveUp( joueur, pas );
		refresh();
	}

	public void moveRight( Joueur joueur, int pas ) {
		this.lab.moveRight( joueur, pas );
		refresh();
	}

	public void moveDown( Joueur joueur, int pas ) {
		this.lab.moveDown( joueur, pas );
		refresh();
	}

	public void moveLeft( Joueur joueur, int pas ) {
		this.lab.moveLeft( joueur, pas );
		refresh();
	}

	public void posJoueursUpdate() {
		this.lab.posJoueursUpdate();
	}

	boolean go = true;
	Thread fantomeMove = new Thread( () -> {
		while( go ) {
			try {
				sleep( 60000 );
			} catch( InterruptedException ignored ) {
			}
			go = this.lab.fantomeMove();
			if( !Serveur.nogui )
				display.updateContent( this.lab.getLabyrinthe() );
		}
	} );

	public void playerCoughtGhost( Joueur joueur ) {
		//envoie message
		String scoreChange = String.format( "SCORE  %s %s %s %s+++",
											joueur.getPseudo(),
											joueur.getScoreStr(),
											joueur.getPosition().getXStr(),
											joueur.getPosition().getYStr());
		try {
			Message.sendUDP( partie.getAddress(), scoreChange );
		} catch( IOException ignored ) {
		}
	}

	public void sendRanking() {
		try {
			Message.sendUDP( partie.getAddress(), "CLASS " + Converter.int8ToHexString( partie.getNbJoueur() ) + "+++" );
			joueurs.sort( ( j1, j2 ) -> j1.getScore() >= j2.getScore() ? -1 : 0 );
			for( Joueur joueur : joueurs ) {
//				if( joueur != null ) {
//					Joueur gagnant = new Joueur( null, null, 0 );
//					gagnant.setScore( -1 );
//					for( Joueur value : joueurs ) {
//						if( value != null ) {
//							if( value.getScore() > gagnant.getScore() ) {
//								gagnant = value;
//							}
//						}
//					}
//					gagnant.getSocket().close();
//					retireJoueur( gagnant );

				Message.sendUDP( partie.getAddress(),
								 String.format( "TOPPL %s %s+++", joueur.getPseudo(),joueur.getScoreStr() ) );
//				}
			}
			Message.sendUDP( partie.getAddress(), "ENDGA+++" );
		} catch( Exception ignored ) {
		}
	}

}