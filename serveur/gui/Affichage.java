package serveur.gui;

import java.awt.event.*;
import java.awt.*;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JPanel;

public class Affichage extends JFrame {

	JPanel panneau;
	JButton[][] buttonMap;
	int[][] lab;
	int hauteur;
	int largeur;

	public Affichage( int[][] lab ) {
		super( "Display" );
		this.lab = lab;
		this.hauteur = lab.length;
		this.largeur = lab[0].length;
		WindowListener l = new WindowAdapter() {
			public void windowClosing( WindowEvent e ) {
				dispose();
			}
		};
		addWindowListener( l );
		panneauInit();
		setContentPane( panneau );
		setSize( 400, 400 );
		setVisible( true );
		updateContent( lab );
	}

	public void panneauInit() {
		this.panneau = new JPanel( new GridLayout( hauteur, largeur ) );
		this.buttonMap = new JButton[hauteur][largeur];
		for( int i = 0; i < hauteur; i++ ) {
			for( int j = 0; j < largeur; j++ ) {
				buttonMap[i][j] = new JButton();
				panneau.add( buttonMap[i][j] );
			}
		}
	}

	public void paintPanel() {
		for( int i = 0; i < hauteur; i++ ) {
			for( int j = 0; j < largeur; j++ ) {
				if( lab[i][j] == 1 || lab[i][j] == -1 ) {
					buttonMap[i][j].setEnabled( false );
					buttonMap[i][j].setBackground( Color.gray );
				} else if( lab[i][j] == 0 ) {
					buttonMap[i][j].setEnabled( false );
					buttonMap[i][j].setVisible( false );
					buttonMap[i][j].setBackground( Color.white );
				} else if( lab[i][j] == 2 ) {
					buttonMap[i][j].setEnabled( false );
					buttonMap[i][j].setVisible( true );
					buttonMap[i][j].setBackground( Color.red );
				} else {
					buttonMap[i][j].setEnabled( false );
					buttonMap[i][j].setVisible( true );
					buttonMap[i][j].setBackground( Color.green );
				}
			}
		}
	}

	//grapiqueF
	public void updateContent( int[][] lab ) {
		this.lab = lab;
		paintPanel();
	}

}
