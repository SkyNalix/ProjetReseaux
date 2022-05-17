/**
 * les traitement sont effectué sur un plateau avec bordure
 * on cherche tjrs une case valide autours de la case actuel
 * on utilise piles pour traiter tout les cases valide
 * -1: extremité 0: chemin 1: mur 2:fantome 3:joueur
 * labyrinthe est un int[][]
 */

package serveur.labyrinthe;

import java.io.IOException;
import java.util.*;

import serveur.Game;
import serveur.Joueur;

public class Labyrinthe {

	private static final Random random = new Random();
	public Game game;
	private final LinkedList<Joueur> joueurs;
	private Fantome[] fantomes;
	private int hauteur, largeur; // ceux là comptent aussi la bordure! ne pas envoyer au client
	int[][] labyrinthe;
	boolean border = true;
	ArrayList<Position> path; //tout position de chemin
	Deque<Integer>[][] posJoueurNbr;

	@SuppressWarnings( "unchecked" )
	public Labyrinthe( Game game ) {
		this.hauteur = game.partie.getHauteur();
		this.largeur = game.partie.getLargeur();
		this.labyrinthe = new int[hauteur][largeur];
		this.path = new ArrayList<>();
		this.game = game;
		this.joueurs = game.partie.getListeJoueur();
		this.posJoueurNbr = new ArrayDeque[hauteur][largeur];
		init( false );
	}

	public synchronized void moveUp( Joueur j, int pas ) {
		Position pos = j.getPosition();
		for( int i = 0; i < pas; i++ ) {
			if( this.labyrinthe[pos.getX() - 1][pos.getY()] == 1 || this.labyrinthe[pos.getX() - 1][pos.getY()] == -1 ) {
				break;
			} else {
				if( this.labyrinthe[pos.getX() - 1][pos.getY()] == 2 ) {
					//retire fantome
					elimineFantome( new Position( pos.getX() - 1, pos.getY() ) );
					j.setScore( j.getScore() + 1 );
					//envoie message
					game.playerCoughtGhost( j );
				}
				this.posJoueurNbr[pos.getX()][pos.getY()].pop();
				if( this.posJoueurNbr[pos.getX()][pos.getY()].isEmpty() ) {
					this.labyrinthe[pos.getX()][pos.getY()] = 0;
				}
				this.posJoueurNbr[pos.getX() - 1][pos.getY()].push( 3 );
			}
			pos.setX( pos.getX() - 1 );
		}
		this.labyrinthe[pos.getX()][pos.getY()] = 3;
		j.setPosition( pos );
		print();
	}

	public synchronized void moveRight( Joueur j, int pas ) {
		Position pos = j.getPosition();
		for( int i = 0; i < pas; i++ ) {
			if( this.labyrinthe[pos.getX()][pos.getY() + 1] == 1 || this.labyrinthe[pos.getX()][pos.getY() + 1] == -1 ) {
				break;
			} else {
				if( this.labyrinthe[pos.getX()][pos.getY() + 1] == 2 ) {
					//retire Fantome
					j.setScore( j.getScore() + 1 );
					elimineFantome( new Position( pos.getX(), pos.getY() + 1 ) );
					//envoie message
					game.playerCoughtGhost( j );
				}
				this.posJoueurNbr[pos.getX()][pos.getY()].pop();
				if( this.posJoueurNbr[pos.getX()][pos.getY()].isEmpty() ) {
					this.labyrinthe[pos.getX()][pos.getY()] = 0;
				}
				this.posJoueurNbr[pos.getX()][pos.getY() + 1].push( 3 );

			}
			pos.setY( pos.getY() + 1 );
		}
		this.labyrinthe[pos.getX()][pos.getY()] = 3;
		j.setPosition( pos );
		print();
	}

	public synchronized void moveLeft( Joueur j, int pas ) {
		Position pos = j.getPosition();
		for( int i = 0; i < pas; i++ ) {
			if( this.labyrinthe[pos.getX()][pos.getY() - 1] == 1 || this.labyrinthe[pos.getX()][pos.getY() - 1] == -1 ) {
				break;
			} else {
				if( this.labyrinthe[pos.getX()][pos.getY() - 1] == 2 ) {
					//effacce fantome
					j.setScore( j.getScore() + 1 );
					elimineFantome( new Position( pos.getX(), pos.getY() - 1 ) );
					//envoie message
					game.playerCoughtGhost( j );
				}
				this.posJoueurNbr[pos.getX()][pos.getY()].pop();
				if( this.posJoueurNbr[pos.getX()][pos.getY()].isEmpty() ) {
					this.labyrinthe[pos.getX()][pos.getY()] = 0;
				}
				this.posJoueurNbr[pos.getX()][pos.getY() - 1].push( 3 );
			}
			pos.setY( pos.getY() - 1 );
		}

		j.setPosition( pos );
		this.labyrinthe[pos.getX()][pos.getY()] = 3;
		print();
	}

	public synchronized void moveDown( Joueur j, int pas ) {
		Position pos = j.getPosition();
		for( int i = 0; i < pas; i++ ) {
			if( this.labyrinthe[pos.getX() + 1][pos.getY()] == 1 || this.labyrinthe[pos.getX() + 1][pos.getY()] == -1 ) {
				break;
			} else {
				if( this.labyrinthe[pos.getX() + 1][pos.getY()] == 2 ) {
					//efface fantome
					j.setScore( j.getScore() + 1 );
					elimineFantome( new Position( pos.getX() + 1, pos.getY() ) );
					//envoie message
					game.playerCoughtGhost( j );
				}
				this.posJoueurNbr[pos.getX()][pos.getY()].pop();
				if( this.posJoueurNbr[pos.getX()][pos.getY()].isEmpty() ) {
					this.labyrinthe[pos.getX()][pos.getY()] = 0;
				}
				this.posJoueurNbr[pos.getX() + 1][pos.getY()].push( 3 );

			}
			pos.setX( pos.getX() + 1 );
		}
		j.setPosition( pos );
		this.labyrinthe[pos.getX()][pos.getY()] = 3;
		print();
	}

	//

	/**
	 * initialisation du plateau
	 *
	 * @param randomInitPos true:random  false:pos[0][0]
	 */
	public void init( boolean randomInitPos ) {
		for( int i = 0; i < hauteur; i++ ) {//remplir labyrinthe par 1    1:mur 0:chemin
			for( int j = 0; j < largeur; j++ ) {
				this.labyrinthe[i][j] = 1;
				this.posJoueurNbr[i][j] = new ArrayDeque<>();
			}
		}
		make( randomInitPos );
		initPosJoueurs();
		initPosFantome();
		initDeque();
	}

	public void initDeque() {
		for( Joueur j : joueurs ) {
			this.posJoueurNbr[j.getPosition().getX()][j.getPosition().getY()].push( 3 );
		}
	}

	public void make( boolean randomInitPos ) {//creation des chemin
		setBorderOn();
		Deque<Position> stack = new ArrayDeque<>();
		int posX = 1, posY = 1;
		if( randomInitPos ) {//un point aleatoire sur plateau
			posX = random.nextInt( this.hauteur - 3 ) + 1;
			posY = random.nextInt( this.largeur - 3 ) + 1;
		}
		Position currentPos = new Position( posX, posY );
		while( true ) {
			this.labyrinthe[currentPos.getX()][currentPos.getY()] = 0;
			path.add( new Position( currentPos.getX(), currentPos.getY() ) );
			stack.push( currentPos );
			currentPos = gotoNewPosFrom( currentPos );
			if( stack.isEmpty() ) {
				break;
			}
			if( currentPos.getY() == -1 && currentPos.getX() == -1 ) {
				stack.pop();
				if( stack.isEmpty() ) {
					break;
				}
				currentPos = stack.pop();
			}
		}
	}

	/**
	 * de puis "from", trouve une autre position qui peut devenir un chemin et retourne cette position
	 *
	 * @param from: position de depart
	 * @return position à emprinter
	 */
	public Position gotoNewPosFrom( Position from ) {//
		Deque<Integer> iCanGo = new ArrayDeque<>();//inserer une list de position peut etre y aller dans iCanGo, de facon aleatoire
		ArrayList<Integer> allPos = new ArrayList<>( Arrays.asList( 0, 1, 2, 3 ) );
		int cmp = 4, newX = 0, newY = 0;
		for( int i = 0; i < 4; i++ ) {//insertion
			int dirc = random.nextInt( cmp );
			iCanGo.push( allPos.get( dirc ) );
			allPos.remove( dirc );
			cmp--;
		}
		while( true ) {
			if( iCanGo.isEmpty() ) {/*si aucun position est valide*/
				return new Position( -1, -1 );
			}
			int direction = iCanGo.pop();
			switch( direction ) {
				case 0://haut
					newX = from.getX() - 1;
					newY = from.getY();
					break;
				case 1://droite
					newX = from.getX();
					newY = from.getY() + 1;
					break;
				case 2://bas
					newX = from.getX() + 1;
					newY = from.getY();
					break;
				case 3://gauche
					newX = from.getX();
					newY = from.getY() - 1;
					break;
			}
			if( ( !( this.labyrinthe[newX][newY] == -1 ) ) && ( !( this.labyrinthe[newX][newY] == 0 ) ) ) {//detection si position choisi est valide
				Position newPos = new Position( newX, newY );
				if( examinate( newPos ) ) {
					return newPos;
				}
			}
		}
	}

	/**
	 * examiner si la position pos peut aller sur une autre position (haut,droite,bas,gauche)
	 *
	 * @param pos: position actuelle
	 * @return true: cette position peut aller sur une autre position, false sinon
	 */
	public boolean examinate( Position pos ) {//true: peut aller sur un autre chemin
		int x = pos.getX(), y = pos.getY(), pathNb = 0;
		if( this.labyrinthe[x - 1][y] == 0 ) {/*haut*/
			pathNb += 1;
		}
		if( this.labyrinthe[x][y + 1] == 0 ) {/*droite*/
			pathNb += 1;
		}
		if( this.labyrinthe[x + 1][y] == 0 ) {/*bas*/
			pathNb += 1;
		}
		if( this.labyrinthe[x][y - 1] == 0 ) {/*gauche*/
			pathNb += 1;
		}
		return pathNb < 2;
	}

	//Joueur
	public void posJoueursUpdate() {
		for( Joueur j : joueurs ) {
			this.labyrinthe[j.getPosition().getX()][j.getPosition().getY()] = 3;
		}
	}

	public synchronized void initPosJoueurs() {
		int path_size = this.path.size();
		int joueurs_size = this.joueurs.size();
		for( Joueur j : joueurs ) {
			int rand = random.nextInt( this.path.size() );
			Position pos = this.path.get( rand );
			while( path_size > joueurs_size && labyrinthe[pos.getX()][pos.getY()] == 3 ) {
				rand = random.nextInt( this.path.size() );
				pos = this.path.get( rand );
			}
			j.setPosition( pos );
		}
		posJoueursUpdate();
	}

	//fantomes
	public synchronized boolean fantomeMove() {
		int positionNotNull = 0;

		for( Fantome f : this.fantomes ) {
			int index = random.nextInt( path.size() );
			if( f.getPosition() != null ) {
				positionNotNull += 1;
				this.labyrinthe[f.getPosition().getX()][f.getPosition().getY()] = 0;
				Position newPos = path.get( index );
				while( this.labyrinthe[newPos.getX()][newPos.getY()] == 2 || this.labyrinthe[newPos.getX()][newPos.getY()] == 3 ) {
					index = random.nextInt( this.path.size() );
					newPos = this.path.get( index );
				}
				f.setPosition( newPos );
				//envoie message
				try {
					String fantomeSig = "GHOST " + newPos.getX() + " " + newPos.getY() + "+++";
					Message.sendUDP( game.partie.getAddress(), fantomeSig );
				} catch( IOException ignored ) {
				}
				this.labyrinthe[newPos.getX()][newPos.getY()] = 2;
			}
		}
		return positionNotNull != 0;
	}

	public synchronized void elimineFantome( Position pos ) {
		System.out.println( "elimination procces" );
		int nbrFantome = 0;
		for( Fantome f : fantomes ) {
			if( f.getPosition() != null ) {

				if( f.getPosition().equalsTo( pos ) ) {
					f.elimine();
				} else {
					nbrFantome += 1;
				}
			}
		}
		System.out.println( "nbrFantome: " + nbrFantome );

		if( nbrFantome == 0 ) {
			//END
			game.partie.stopGame();
		}
	}

	public void initPosFantome() {
		this.fantomes = new Fantome[joueurs.size()];
		for( int i = 0; i < fantomes.length; i++ ) {
			int index = random.nextInt( this.path.size() );
			fantomes[i] = new Fantome();
			Position newPos = this.path.get( index );
			while( this.labyrinthe[newPos.getX()][newPos.getY()] == 2 || this.labyrinthe[newPos.getX()][newPos.getY()] == 3 ) {
				index = random.nextInt( this.path.size() );
				newPos = this.path.get( index );
			}
			fantomes[i].setPosition( newPos );
			this.labyrinthe[newPos.getX()][newPos.getY()] = 2;

		}
	}

	public static final String ANSI_RESET = "\u001B[0m";
	public static final String ANSI = "\u001B[42m";
	public static final String ANSI_RED = "\u001B[31m";

	public void print() {//affiche

		for( int i = 0; i < hauteur; i++ ) {
			for( int j = 0; j < largeur; j++ ) {
				//System.out.print(this.labyrinthe[i][j]==-1?"$ ":(this.labyrinthe[i][j]==1?"@ ":"  "));
				if( this.labyrinthe[i][j] == -1 ) {
					System.out.print( "$ " );
				} else if( this.labyrinthe[i][j] == 1 ) {
					System.out.print( "@ " );
				} else if( this.labyrinthe[i][j] == 0 ) {
					System.out.print( "  " );
				} else if( this.labyrinthe[i][j] == 2 ) {
					System.out.print( ANSI_RED + "S " + ANSI_RESET );
				} else {
					//System.out.print("V ");
					System.out.print( ANSI + "V " + ANSI_RESET );
				}

			}
			System.out.println();
		}
	}

	@SuppressWarnings( "unchecked" )
	public void setBorderOn() {//ajoute bordure
		this.border = true;
		this.hauteur += 2; this.largeur += 2;
		int[][] labyrintheBorder = new int[hauteur][largeur];
		int[] borderTopBot = new int[this.largeur];
		Deque<Integer>[][] newDeque = new ArrayDeque[this.hauteur][this.largeur];
		Deque<Integer>[] newDequeTopBot = new ArrayDeque[this.largeur];

		newDeque[0] = newDequeTopBot;
		newDeque[hauteur - 1] = newDequeTopBot;

		labyrintheBorder[0] = borderTopBot;
		labyrintheBorder[hauteur - 1] = borderTopBot;
		for( int i = 0; i < this.largeur; i++ ) {
			borderTopBot[i] = -1;
		}
		for( int i = 1; i < hauteur - 1; i++ ) {
			for( int j = 0; j < largeur; j++ ) {
				if( j == 0 || j == largeur - 1 ) {
					labyrintheBorder[i][j] = -1; newDeque[i][j] = new ArrayDeque<>();
				} else {
					labyrintheBorder[i][j] = this.labyrinthe[i - 1][j - 1]; newDeque[i][j] = this.posJoueurNbr[i - 1][j - 1];
				}
			}
		}
		this.posJoueurNbr = newDeque;
		this.labyrinthe = labyrintheBorder;
	}

	public int[][] getLabyrinthe() {
		return labyrinthe;
	}

	public int getNbFantomes() {
		return fantomes.length;
	}

}