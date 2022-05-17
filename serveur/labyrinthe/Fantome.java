package serveur.labyrinthe;

public class Fantome extends Personne{
    public Fantome(Position position) {
        super(position);
        this.setPseudo("fantome");
    }

    public Fantome(){}
    public boolean isFantome(){
        return true;
    }
    public synchronized void elimine(){
        this.setPosition(null);
    }
}
