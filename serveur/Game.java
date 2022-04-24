package serveur;

//import java.io.IOException;
//import java.net.Socket;
//import java.util.Dictionary;
//import java.util.HashMap;

import java.util.ArrayList;
import java.util.Scanner;

import serveur.gui.Display;
import serveur.labyrinthe.*;

import static java.lang.Thread.sleep;

import java.net.DatagramSocket;

public class Game {

	ArrayList<Joueur> joueurs;
	Labyrinthe lab;
	Display display;
	boolean gui;

	public Game( ArrayList<Joueur> joueurs, Labyrinthe lab, boolean gui ) {
		this.gui = gui;
		this.joueurs = joueurs;
		this.lab = lab;
		if( gui ) {
			this.display = new Display( this.lab.labyrinthe );
		}
		//fantomeMove.start();
		//commandControl.start();
	}

	public void moveUp( Joueur joueur, int pas ) {
		this.lab.moveUp( joueur, pas );
	}

	public void moveRight( Joueur joueur, int pas ) {
		this.lab.moveRight( joueur, pas );
	}

	public void moveDown( Joueur joueur, int pas ) {
		this.lab.moveDown( joueur, pas );
	}

	public void moveLeft( Joueur joueur, int pas ) {
		this.lab.moveLeft( joueur, pas );
	}

	public void posJoueursUpdate() {
		this.lab.posJoueursUpdate();
	}

	public void startGame() {
		commandControl.start();
		fantomeMove.start();
	}

	Thread commandControl = new Thread( () -> {
		String commande;
		Scanner sc = new Scanner( System.in );
		while( true ) {
			System.out.println( "first" );
			lab.print();
			if( gui ) {
				display.updateContent( this.lab.labyrinthe );
			}
			commande = sc.nextLine();
			if( commande.equals( "z" ) ) {
				System.out.println( "retour touch z" );
				moveUp( joueurs.get( 0 ), 1 );
			}
			if( commande.equals( "s" ) ) {
				System.out.println( "retour touch s" );
				moveDown( joueurs.get( 0 ), 1 );
			}
			if( commande.equals( "q" ) ) {
				System.out.println( "retour touch q" );
				moveLeft( joueurs.get( 0 ), 1 );
			}
			if( commande.equals( "d" ) ) {
				System.out.println( "retour touch d" );
				moveRight( joueurs.get( 0 ), 1 );
			}
			posJoueursUpdate();
			System.out.println( "first end" );
		}
	} );

	Thread fantomeMove = new Thread( () -> {
		boolean go = true;
		while( go ) {
			System.out.println( "THREAD IN PROCECING" );

			try {
				sleep( 10000 );
			} catch( InterruptedException ignored ) {
			}
			go = this.lab.fantomeMove();
			System.out.println( "block Test7" );
			if( gui ) {
				display.updateContent( this.lab.labyrinthe );
			}
			System.out.println( "block Test8" );
		}

	} );

	public static void main( String[] args ) {
		try {
			ArrayList<Joueur> joueurs = new ArrayList<>();
			joueurs.add( new Joueur( "Vladimir", null, 4242 ) );
			joueurs.add( new Joueur( "Aypierre", null, 4243 ) );
			joueurs.add( new Joueur( "Zelenski", null, 4244 ) );

			Labyrinthe lab = new Labyrinthe( 10, 10, joueurs );
			Game game = new Game( joueurs, lab, false );

		} catch( Exception e ) {
			e.printStackTrace();
		}
		//display.updateContent();
	}

}
