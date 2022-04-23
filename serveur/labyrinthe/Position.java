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
}
