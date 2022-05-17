package serveur.labyrinthe;
public class Position {
    private int x;
    private int y;
    public Position(int x, int y){
        this.x=x;
        this.y=y;
    }
    public String toString(){
        return "position  x: "+x+"  y: "+y;
    }
    public boolean equalsTo(Position pos){
        return pos.getX()==this.getX()&&pos.getY()==this.getY();
    }
    public int getX(){
        return this.x;
    }
    public int getY(){
        return this.y;
    }
    void setX(int x){
        this.x=x;
    }
    void setY(int y){
        this.y=y;
    }
    public String getXStr() { // que pour l'affichage et envoi au client
        String str = x - 1 + "";
        return "0".repeat( 3 - str.length() ) + str;
    }
    public String getYStr() { // que pour l'affichage et envoi au client
        String str = y - 1 + "";
        return "0".repeat( 3 - str.length() ) + str;
    }

}
