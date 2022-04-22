package gui;

import java.awt.*;
import java.net.Socket;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JPanel;
public class Display extends JFrame{
	JPanel panneau;
	JButton[][] buttonMap;
	int[][] lab;
	int hauteur;
	int largeur;

	public Display(int[][] lab) {
		super("Display");
		this.lab=lab;
		this.hauteur=lab.length;
		this.largeur=lab[0].length;
		setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		panneauInit();
		setContentPane(panneau);
		setSize(1000,1000);
		setVisible(true);
	}

	public void panneauInit() {
		this.panneau=new JPanel(new GridLayout(hauteur,largeur));
		this.buttonMap=new JButton[hauteur][largeur];
		for(int i=0;i<hauteur;i++) {
			for(int j=0;j<largeur;j++) {
				buttonMap[i][j]=new JButton();
				panneau.add(buttonMap[i][j]);
			}
		}
	}

	//TODO ajoute boutton map dans pannel et mettre ajoue uniqueme le couleur de button
	public void paintPanel() {
		for(int i=0;i<hauteur;i++){
			for(int j=0;j<largeur;j++){
				if(lab[i][j]==1||lab[i][j]==-1){
					buttonMap[i][j].setEnabled(false);
					buttonMap[i][j].setBackground(Color.gray);
				}else if(lab[i][j]==0){
					buttonMap[i][j].setEnabled(false);
					buttonMap[i][j].setVisible(false);
					buttonMap[i][j].setBackground(Color.white);
				}else if(lab[i][j]==2) {
					buttonMap[i][j].setEnabled(false);
					buttonMap[i][j].setVisible(true);
					buttonMap[i][j].setBackground(Color.red);
				}else{
					buttonMap[i][j].setEnabled(false);
					buttonMap[i][j].setVisible(true);
					buttonMap[i][j].setBackground(Color.green);
				}
			}
		}
	}

	//grapiqueF
	public void updateContent(int[][] lab) {//TODO à finaliser
		this.lab=lab;
		paintPanel();
		//this.revalidate();
		System.out.println("block Test11");
	}
}
