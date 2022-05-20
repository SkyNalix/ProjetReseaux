package serveur.labyrinthe;

public class Fantome extends Personne{
    public Fantome(Position position) {
        super("fantome");
        setPosition( position );
    }

    public Fantome(){}
    public synchronized void elimine(){
        this.setPosition(null);
    }
}
