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
	Joueur[] joueurs;
	Labyrinthe lab;
	Display display;
	public Game(Labyrinthe lab){
		this.joueurs=lab.getJoueurs();
		this.lab=lab;
		this.display=new Display(this.lab.getLabyrinthe());
		fantomeMove.start();
	}
	public void moveUp(Joueur joueur,int pas){
		this.lab.moveUp(joueur,pas);
		display.updateContent(this.lab.getLabyrinthe());
	}
	public void moveRight(Joueur joueur,int pas){
		this.lab.moveRight(joueur,pas);
		display.updateContent(this.lab.getLabyrinthe());
	}
	public void moveDown(Joueur joueur,int pas){
		this.lab.moveDown(joueur,pas);
		display.updateContent(this.lab.getLabyrinthe());
	}
	public void moveLeft(Joueur joueur,int pas){
		this.lab.moveLeft(joueur,pas);
		display.updateContent(this.lab.getLabyrinthe());
	}

	public void posJoueursUpdate(){
		this.lab.posJoueursUpdate();
	}

	boolean go=true;
	Thread fantomeMove=new Thread(()->{
		while(go&&display.isInGame()) {
			go=this.lab.fantomeMove();
			display.updateContent(this.lab.getLabyrinthe());
			try {
				sleep(60000);
			} catch (InterruptedException ignored ) {
			}
		}

	});

}