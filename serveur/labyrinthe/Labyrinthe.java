/**
 * les traitement sont effectué sur un plateau avec bordure
 * on cherche tjrs une case valide autours de la case actuel
 * on utilise piles pour traiter tout les cases valide
 * -1: extremité 0: chemin 1: mur 2:fantome 3:joueur
 * labyrinthe est un int[][]
 */

//TODO fantomeMove()

package serveur.labyrinthe;

import java.util.*;


import static java.lang.Thread.sleep;
import serveur.Joueur;

public class Labyrinthe {
    Random random;
    private Joueur[] joueurs;
    private Fantome[] fantomes;
    private int hauteur, largeur;
    int[][] labyrinthe;
    boolean border = true;
    public boolean drawTrace=false;//permet d'affiche le detail de la création
    ArrayList<Position> path;//tout position de chemin
    Deque<Integer>[][] posJoueurNbr;
    public Labyrinthe(int hauteur, int largeur) {
        this.random=new Random();
        this.hauteur = hauteur;
        this.largeur = largeur;
        this.labyrinthe = new int[hauteur][largeur];
        this.path=new ArrayList<>();
    }
    @SuppressWarnings("unchecked")
    public Labyrinthe(int hauteur,int largeur,Joueur[] joueurs){
        this(hauteur,largeur);
        this.joueurs=joueurs;
        //TODO mettre a jour posJoueurNbr dans setBorder()
        this.posJoueurNbr=new ArrayDeque[hauteur][largeur];
        init(false);
    }

    //dep
    //TODO au lieux de retourne une position, on envoie directement la commande au socket
    //TODO cas renconctre fantome
    public synchronized Position moveUp(Joueur j,int pas){
        boolean scoreChange=false;
        Position pos=j.getPosition();
        for(int i=0;i<pas;i++){
            if(this.labyrinthe[pos.getX()-1][pos.getY()]==1||this.labyrinthe[pos.getX()-1][pos.getY()]==-1){
                break;
            }else{
                if(this.labyrinthe[pos.getX()-1][pos.getY()]==2){
                    scoreChange=true;
                    elimineFantome(new Position(pos.getX()-1,pos.getY()));
                    j.setScore(j.getScore()+1);
                }
                this.posJoueurNbr[pos.getX()][pos.getY()].pop();
                if (this.posJoueurNbr[pos.getX()][pos.getY()].isEmpty()) {
                    this.labyrinthe[pos.getX()][pos.getY()] = 0;
                }
                this.posJoueurNbr[pos.getX() - 1][pos.getY()].push(3);
            }
            pos.setX(pos.getX()-1);
        }
        j.setPosition(pos);
        if(scoreChange){//TODO
            //Message.send(j,pos,myScore);
        }else {
            //Message.send(j,pos,-1);
        }
        return pos;
    }
    public synchronized Position moveRight(Joueur j,int pas){
        Position pos=j.getPosition();
        boolean scoreChange=false;
        for(int i=0;i<pas;i++){
            if(this.labyrinthe[pos.getX()][pos.getY()+1]==1||this.labyrinthe[pos.getX()][pos.getY()+1]==-1){
                break;
            }else{
                if(this.labyrinthe[pos.getX()][pos.getY()+1]==2){
                    j.setScore(j.getScore()+1);
                    scoreChange=true;
                    elimineFantome(new Position(pos.getX(),pos.getY()+1));
                }
                this.posJoueurNbr[pos.getX()][pos.getY()].pop();
                if(this.posJoueurNbr[pos.getX()][pos.getY()].isEmpty()){
                    this.labyrinthe[pos.getX()][pos.getY()]=0;
                }
                this.posJoueurNbr[pos.getX()][pos.getY()+1].push(3);

            }
            pos.setY(pos.getY()+1);
        }
        this.labyrinthe[pos.getX()][pos.getY()]=3;
        j.setPosition(pos);
        //TODO
        return pos;
    }
    public synchronized Position moveLeft(Joueur j,int pas){
        boolean scoreChange=false;
        Position pos=j.getPosition();
        for(int i=0;i<pas;i++){
            if(this.labyrinthe[pos.getX()][pos.getY()-1]==1||this.labyrinthe[pos.getX()][pos.getY()-1]==-1){
                break;
            }else{
                if(this.labyrinthe[pos.getX()][pos.getY()-1]==2){
                    //TODO effacce fantome
                    j.setScore(j.getScore()+1);
                    scoreChange=true;
                    elimineFantome(new Position(pos.getX(),pos.getY()-1));
                }
                this.posJoueurNbr[pos.getX()][pos.getY()].pop();
                if(this.posJoueurNbr[pos.getX()][pos.getY()].isEmpty()){
                    this.labyrinthe[pos.getX()][pos.getY()]=0;
                }
                this.posJoueurNbr[pos.getX()][pos.getY()-1].push(3);
            }
            pos.setY(pos.getY()-1);
        }

        j.setPosition(pos);
        this.labyrinthe[pos.getX()][pos.getY()]=3;
        return pos;
    }
    public synchronized Position moveDown(Joueur j,int pas){
        boolean scoreChange=false;
        Position pos=j.getPosition();
        for(int i=0;i<pas;i++){
            if(this.labyrinthe[pos.getX()+1][pos.getY()]==1||this.labyrinthe[pos.getX()+1][pos.getY()]==-1){
                break;
            }else{
                if(this.labyrinthe[pos.getX()+1][pos.getY()]==2){
                    //TODO efface fantome
                    j.setScore(j.getScore()+1);
                    scoreChange=true;
                    elimineFantome(new Position(pos.getX()+1,pos.getY()));
                }
                this.posJoueurNbr[pos.getX()][pos.getY()].pop();
                if(this.posJoueurNbr[pos.getX()][pos.getY()].isEmpty()){
                    this.labyrinthe[pos.getX()][pos.getY()]=0;
                }
                this.posJoueurNbr[pos.getX()+1][pos.getY()].push(3);

            }
            pos.setX(pos.getX()+1);
        }
        j.setPosition(pos);
        this.labyrinthe[pos.getX()][pos.getY()]=3;
        //message replit client
        return pos;
    }

    //
    /**
     * initialisation du plateau
     * @param randomInitPos true:random  false:pos[0][0]
     */
    public void init(boolean randomInitPos) {
        for (int i = 0; i < hauteur; i++) {//remplir labyrinthe par 1    1:mur 0:chemin
            for (int j = 0; j < largeur; j++) {
                this.labyrinthe[i][j] = 1;
                this.posJoueurNbr[i][j]=new ArrayDeque<Integer>();
            }
        }
        make(randomInitPos);
        initPosJoueur(joueurs);
        initPosFantome();
        initDeque();
    }
    public void initDeque(){
        for(Joueur j:joueurs){
            this.posJoueurNbr[j.getPosition().getX()][j.getPosition().getY()].push(3);
        }
    }
    public void make(boolean randomInitPos) {//creation des chemin
        setBorderOn();
        Deque<Position> stack = new ArrayDeque<>();
        int posX = 1,posY = 1;
        if (randomInitPos) {//un point aleatoire sur plateau
            posX = random.nextInt(this.hauteur-3)+1;
            posY = random.nextInt(this.largeur-3)+1;
        }
        Position currentPos = new Position(posX, posY);
        while (true) {
            if(drawTrace) {
                this.print();
                try {sleep(20);} catch (Exception e) {}
            }
            this.labyrinthe[currentPos.getX()][currentPos.getY()] = 0;
            path.add(new Position(currentPos.getX(),currentPos.getY()));
            stack.push(currentPos);
            Position newPos = gotoNewPosFrom(currentPos);
            currentPos = newPos;
            if (stack.isEmpty()) {
                break;
            }
            if(currentPos.getY()==-1 && currentPos.getX()==-1){
                stack.pop();
                if(stack.isEmpty()){
                    break;
                }
                currentPos=stack.pop();
            }
        }
    }

    /**
     * de puis "from", trouve une autre position qui peut devenir un chemin et retourne cette position
     * @param from: position de depart
     * @return position à emprinter
     */
    public Position gotoNewPosFrom(Position from) {//
        Deque<Integer> iCanGo=new ArrayDeque<>();//inserer une list de position peut etre y aller dans iCanGo, de facon aleatoire
        ArrayList<Integer> allPos=new ArrayList<>(Arrays.asList(0,1,2,3));
        int cmp=4,newX=0,newY=0;
        for(int i=0;i<4;i++) {//insertion
            int dirc = random.nextInt(cmp);
            iCanGo.push(allPos.get(dirc));
            allPos.remove(dirc);
            cmp--;
        }
        while (true){
            if(iCanGo.isEmpty()){/*si aucun position est valide*/return new Position(-1,-1);}
            int direction=iCanGo.pop();
            switch (direction) {
                case 0://haut
                    newX=from.getX()-1;
                    newY=from.getY();
                    break;
                case 1://droite
                    newX=from.getX();
                    newY=from.getY()+1;
                    break;
                case 2://bas
                    newX=from.getX()+1;
                    newY=from.getY();
                    break;
                case 3://gauche
                    newX=from.getX();
                    newY=from.getY()-1;
                    break;
            }
            if((!(this.labyrinthe[newX][newY]==-1))&&(!(this.labyrinthe[newX][newY]==0))){//detection si position choisi est valide
                Position newPos = new Position(newX,newY);
                if (examinate(newPos)) {
                    return newPos;
                }
            }
        }
    }

    /**
     * examiner si la position pos peut aller sur une autre position (haut,droite,bas,gauche)
     * @param pos: position actuelle
     * @return true: cette position peut aller sur une autre position, false sinon
     */
    public boolean examinate(Position pos) {//true: peut aller sur un autre chemin
        int x = pos.getX(),y = pos.getY(),pathNb=0;
        if(this.labyrinthe[x-1][y]==0){/*haut*/pathNb+=1;}
        if(this.labyrinthe[x][y+1]==0){/*droite*/pathNb+=1;}
        if(this.labyrinthe[x+1][y]==0){/*bas*/pathNb+=1;}
        if(this.labyrinthe[x][y-1]==0){/*gauche*/pathNb+=1;}
        return pathNb<2;
    }

    //Joueur
    public void posJoueursUpdate(){
        for(Joueur j:joueurs){
            this.labyrinthe[j.getPosition().getX()][j.getPosition().getY()]=3;
        }
    }
    public synchronized void initPosJoueur(Joueur[] joueurs){
        for(Joueur j:joueurs){
            int rand=random.nextInt(this.path.size());
            j.setPosition(this.path.get(rand));
        }
        posJoueursUpdate();
    }

    //fantomes
    public synchronized boolean fantomeMove(){
        int positionNotNull=0;
        for (Fantome f : this.fantomes) {
            System.out.println("block Test1");
            System.out.println("fantomeMove f");
            int index=random.nextInt(path.size());
            if (f.getPosition() != null) {
                positionNotNull+=1;
                System.out.println("block Test2");
                this.labyrinthe[f.getPosition().getX()][f.getPosition().getY()]=0;
                Position newPos=path.get(index);
                while(this.labyrinthe[newPos.getX()][newPos.getY()]==2||this.labyrinthe[newPos.getX()][newPos.getY()]==3){
                    System.out.println("oh no");
                    index=random.nextInt(this.path.size());
                    newPos=this.path.get(index);
                }
                System.out.println("update Position: x "+ newPos.getX()+" y"+newPos.getY());
                f.setPosition(newPos);
                this.labyrinthe[newPos.getX()][newPos.getY()]=2;
            }
        }
        return positionNotNull!=0;

    }
    public synchronized void elimineFantome(Position pos){
        System.out.println("elimination procces");
        int nbrFantome=0;
        for(Fantome f:fantomes){
            if(f.getPosition()!=null) {
                nbrFantome+=1;
                if (f.getPosition().equalsTo(pos)) {
                    f.elimine();
                }
            }
        }
        if(nbrFantome==1){
            System.out.println("END OF G--A--M--E-----");
            //TODO END OF GAME
        }
    }

    public void initPosFantome(){
        this.fantomes=new Fantome[joueurs.length];
        for(int i=0;i<fantomes.length;i++){
            int index=random.nextInt(this.path.size());
            fantomes[i]=new Fantome();
            Position newPos=this.path.get(index);
            while(this.labyrinthe[newPos.getX()][newPos.getY()]==2||this.labyrinthe[newPos.getX()][newPos.getY()]==3){
                System.out.println("while iniPosFantome");
                index=random.nextInt(this.path.size());
                newPos=this.path.get(index);
            }
            fantomes[i].setPosition(newPos);
            this.labyrinthe[newPos.getX()][newPos.getY()]=2;

        }
    }

    public static final String ANSI_RESET = "\u001B[0m" ;
    public static final String ANSI = "\u001B[42m";
    public static final String ANSI_RED = "\u001B[31m";

    public void print(){//affiche

        for (int i=0;i<hauteur;i++){
            for( int j=0; j<largeur;j++){
                //System.out.print(this.labyrinthe[i][j]==-1?"$ ":(this.labyrinthe[i][j]==1?"@ ":"  "));
                if(this.labyrinthe[i][j]==-1){
                    System.out.print("$ ");
                }else if(this.labyrinthe[i][j]==1){
                    System.out.print("@ ");
                }else if(this.labyrinthe[i][j]==0){
                    System.out.print("  ");
                }else if(this.labyrinthe[i][j]==2) {
                    System.out.print( ANSI_RED+ "S "+ANSI_RESET);
                }else{
                    //System.out.print("V ");
                    System.out.print( ANSI+ "V "+ANSI_RESET);
                }

            }
            System.out.println();
        }
    }
    public void printDeque(){
        for (int i=1;i<hauteur-1;i++) {
            for (int j = 1; j < largeur-1; j++) {
                if(posJoueurNbr[i][j].peek()==null){
                    if(this.labyrinthe[i][j]==0){
                        System.out.print("  ");
                    }else{
                        System.out.print("@ ");
                    }

                }else{
                    System.out.print(posJoueurNbr[i][j].size()+" ");
                }
            }
            System.out.println();
        }
    }
    @SuppressWarnings("unchecked")
    public void setBorderOn(){//ajoute bordure
        this.border=true;
        this.hauteur+=2;this.largeur+=2;
        int[][] labyrintheBorder=new int[hauteur][largeur];
        int[] borderTopBot=new int[this.largeur];
        Deque<Integer>[][] newDeque=new ArrayDeque[this.hauteur][this.largeur];
        Deque<Integer>[] newDequeTopBot=new ArrayDeque[this.largeur];

        newDeque[0]=newDequeTopBot;
        newDeque[hauteur-1]=newDequeTopBot;

        labyrintheBorder[0]=borderTopBot;
        labyrintheBorder[hauteur-1]=borderTopBot;
        for(int i=0;i<this.largeur;i++){borderTopBot[i]=-1;}
        for(int i=1;i<hauteur-1;i++){
            for(int j=0;j<largeur;j++){
                if(j==0||j==largeur-1){labyrintheBorder[i][j]=-1;newDeque[i][j]=new ArrayDeque<>();}
                else{labyrintheBorder[i][j]=this.labyrinthe[i-1][j-1];newDeque[i][j]=this.posJoueurNbr[i-1][j-1];}
            }
        }
        this.posJoueurNbr=newDeque;
        this.labyrinthe=labyrintheBorder;
    }
    @SuppressWarnings("unchecked")
    public void setBorderOff(){//enleve bordure
        this.border=false;
        int[][] lab=new int[this.hauteur-2][this.largeur-2];
        Deque<Integer>[][] newDeque=new ArrayDeque[this.hauteur-2][this.largeur-2];

        this.hauteur-=2;this.largeur-=2;
        for(int i=0;i<hauteur;i++){
            for(int j=0;j<largeur;j++){lab[i][j]=this.labyrinthe[i+1][j+1];newDeque[i][j]=this.posJoueurNbr[i+1][j+1];}
        }
        this.posJoueurNbr=newDeque;
        this.labyrinthe=lab;
    }

    public void setHauteur(int hauteur) {
        this.hauteur = hauteur;
    }

    public void setLargeur(int largeur) {
        this.largeur = largeur;
    }

    public int getHauteur() {
        return hauteur;
    }

    public int getLargeur() {
        return largeur;
    }
    public Joueur[] getJoueurs(){
        return joueurs;
    }

    public int[][] getLabyrinthe() {
        return labyrinthe;
    }

    public int getNbFantomes(){
        return fantomes.length;
    }
}