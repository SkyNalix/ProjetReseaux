package labyrinthe;

public abstract class Personne {
    private Position position=null;
    private String pseudo;
    public Personne(){}
    public Personne(String pseudo){
        this.pseudo=pseudo;
    }
    public Personne(Position position){
        this.position=position;
    }
    public Personne(String pseudo,Position position){
        this(pseudo);
        this.position=position;
    }
    public void setPosition(Position pos){
        this.position=pos;
    }
    public Position getPosition(){
        return this.position;
    }
    public String getPseudo(){
        return this.pseudo;
    }
    public void setPseudo(String pseudo){
        this.pseudo=pseudo;
    }
    public abstract boolean isFantome();
}
