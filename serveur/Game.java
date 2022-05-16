package serveur;

//import java.io.IOException;
//import java.net.Socket;
//import java.util.Dictionary;
//import java.util.HashMap;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Scanner;

import serveur.gui.Display;
import serveur.labyrinthe.*;

import static java.lang.Thread.interrupted;
import static java.lang.Thread.sleep;

import java.net.DatagramSocket;

public class Game {
	Message communication =new Message();
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
	public static boolean sleep=true;
	Thread fantomeMove=new Thread(()->{
		while(go&&display.isInGame()&&this.lab.isInGame()) {

			new Thread(()->{
				try{
					sleep(60000);
					sleep=false;
				}catch (Exception e){}
			}).start();
			while(sleep){
				try {
					wait();
				}catch (Exception e ) {}
			}
			sleep=true;

			go=this.lab.fantomeMove();
			display.updateContent(this.lab.getLabyrinthe());
		}
		//END
		try {
			Joueur gagnant=new Joueur(null,null,0);
			gagnant.setScore(-1);
			communication.sendUDP(this.lab.addressUDP,"CLASS "+getNbrJoueur()+"+++");
			sendClassement();
		} catch (IOException e) {}
		display.dispose();

	});
	private int getNbrJoueur(){
		int nbr=0;
		for(Joueur j:joueurs){
			if(j!=null){
				nbr+=1;
			}
		}
		return nbr;
	}
	private void retireJoueur(Joueur j){
		for(int i=0;i<this.joueurs.length;i++){
			if(joueurs[i]==j){
				joueurs[i]=null;
			}
		}
	}
	private void sendClassement(){
		try{
			for(int i=0;i<joueurs.length;i++){
				if(joueurs[i]!=null){
					Joueur gagnant=new Joueur(null,null,0);
					gagnant.setScore(-1);
					for(int j=0;j<joueurs.length;j++){
						if(joueurs[j]!=null){
							if(joueurs[j].getScore()>gagnant.getScore()){
								gagnant=joueurs[j];
							}
						}
					}
					gagnant.getSocket().close();
					retireJoueur(gagnant);
					communication.sendUDP(lab.addressUDP,"TOPPL "+gagnant.getPseudo()+" "+gagnant.getScore()+"+++");
				}
			}
			communication.sendUDP(lab.addressUDP,"ENDGA+++");
		}catch (Exception e){}
	}
}