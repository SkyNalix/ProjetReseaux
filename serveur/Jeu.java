package serveur;


import java.io.IOException;
import java.util.*;

import serveur.gui.Affichage;
import serveur.labyrinthe.*;
import static java.lang.Thread.sleep;

public class Jeu {

	public Partie partie;
	public LinkedList<Joueur> joueurs;
	public Labyrinthe lab;
	public Affichage affichage;

	public Jeu( Partie partie ) {
		this.partie = partie;
		this.joueurs = partie.getListeJoueur();
		this.lab = new Labyrinthe( this );
		if( !Serveur.nogui )
			this.affichage = new Affichage( this.lab.getLabyrinthe() );
		fantomeMove.start();
	}

	public void refresh() {
		if( !Serveur.nogui )
			affichage.updateContent( this.lab.getLabyrinthe() );
		else
			lab.print();
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
				affichage.updateContent( this.lab.getLabyrinthe() );
		}
	} );

	public void playerCoughtGhost( Joueur joueur ) {
		//envoie message
		String scoreChange = String.format( "SCORE %s %s %s %s+++",
											joueur.getPseudo(),
											joueur.getScoreStr(),
											joueur.getPosition().getXStr(),
											joueur.getPosition().getYStr() );
		try {
			Connexion.sendUDP( partie.getAddress(), Converter.convert( scoreChange ) );
		} catch( IOException ignored ) {
		}
	}

}