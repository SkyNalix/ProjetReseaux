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
	boolean gui;
	public Game(Labyrinthe lab,boolean gui){
		this.gui=gui;
		this.joueurs=lab.getJoueurs();
		this.lab=lab;
		if(gui) {
			this.display=new Display(this.lab.getLabyrinthe());

		}
		fantomeMove.start();
		//commandControl.start();
	}
	public void moveUp(Joueur joueur,int pas){
		this.lab.moveUp(joueur,pas);
	}
	public void moveRight(Joueur joueur,int pas){
		this.lab.moveRight(joueur,pas);
	}
	public void moveDown(Joueur joueur,int pas){
		this.lab.moveDown(joueur,pas);
	}
	public void moveLeft(Joueur joueur,int pas){
		this.lab.moveLeft(joueur,pas);
	}

	public void posJoueursUpdate(){
		this.lab.posJoueursUpdate();
	}

	Thread commandControl=new Thread(()->{
		String commande="";
		Scanner sc=new Scanner(System.in);
		while(true) {
			System.out.println("first");
			lab.print();
			if (gui) {
				display.updateContent(this.lab.getLabyrinthe());
			}
			commande=sc.nextLine();
			if(commande.equals("z")){
				System.out.println("retour touch z");
				moveUp(joueurs[0],1);
			}
			if(commande.equals("s")){
				System.out.println("retour touch s");
				moveDown(joueurs[0],1);
			}
			if(commande.equals("q")){
				System.out.println("retour touch q");
				moveLeft(joueurs[0],1);
			}
			if(commande.equals("d")){
				System.out.println("retour touch d");
				moveRight(joueurs[0],1);
			}
			posJoueursUpdate();
			System.out.println("first end");
		}

	});

	Thread fantomeMove=new Thread(()->{
		boolean go=true;
		while(go) {
			System.out.println("THREAD IN PROCECING");

            try {
                sleep(2000);
            } catch (InterruptedException e) {

            }
			go=this.lab.fantomeMove();
			System.out.println("block Test7");
			if(gui){
				display.updateContent(this.lab.getLabyrinthe());
			}
			System.out.println("block Test8");
		}

	});

}