package serveur.labyrinthe;

import serveur.labyrinthe.Position;

public abstract class Personne {

    private Position position = null;
    private String pseudo;

    public Personne() {}

    public Personne( String pseudo ) {
        this.pseudo = pseudo;
    }

    public void setPosition( Position pos ) {
        this.position = pos;
    }

    public Position getPosition() {
        return this.position;
    }

    public String getPseudo() {
        return this.pseudo;
    }

}
