package serveur.labyrinthe;

public class Fantome extends Personne{

    public Fantome(){}
    public synchronized void elimine(){
        this.setPosition(null);
    }
}
