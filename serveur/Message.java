package serveur;

import serveur.*;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetSocketAddress;

public class Message {

    public static Joueur newpl( Connexion connexion, String str ) {
        //on decrypte le str de la Forme NEWPL id port***
        String[] split = Utils.splitString( str );
        String id = split[1];
        String port_str = split[2];
        int port;
        try {
            port = Integer.parseInt( port_str );
            for( int i = 0; i < Serveur.listePartie.size(); i++ ) {
                for( int j = 0; j < Serveur.listePartie.get( i ).getListeJoueur().size(); j++ ) {
                    if( port == Serveur.listePartie.get( i ).getListeJoueur().get( j ).getPort() ) {
                        return null;
                    }
                }
            }
        } catch( Exception e ) {
            return null;
        }
        if( id.length() != 8 || port_str.length() != 4 )
            return null;
        Joueur joueur = new Joueur( connexion, id, port );
        Partie partie = new Partie();
        partie.ajouterJoueur( joueur );
        joueur.partie = partie;
        return joueur;
    }

    public static Joueur regis( Connexion connexion, String str ) {
        //  [REGIS␣id␣port␣m***]
        String[] split = Utils.splitString( str );
        String id = split[1];
        String port_str = split[2];
        String partie_id_str = split[3];
        if( id.length() != 8 || port_str.length() != 4 ) {
            return null;
        }
        int port;
        int partie_id;
        try {
            port = Integer.parseInt( port_str );
            for( int i = 0; i < Serveur.listePartie.size(); i++ ) {
                for( int j = 0; j < Serveur.listePartie.get( i ).getListeJoueur().size(); j++ ) {
                    if( port == Serveur.listePartie.get( i ).getListeJoueur().get( j ).getPort() ) {
                        return null;
                    }
                }
            }
            partie_id = Integer.parseInt( partie_id_str );
        } catch( Exception e ) {
            return null;
        }
        Joueur joueur = new Joueur( connexion, id, port );
        for( Partie partie : Serveur.listePartie ) {
            if( partie.getID() == partie_id ) {
                if( partie.ajouterJoueur( joueur ) ) {
                    joueur.partie = partie;
                    return joueur;
                } else {
                    return null;
                }
            }
        }
        return null; // partie non trouvée
    }

    public static void list( String x, Connexion connexion ) throws IOException {
        String id_str = Utils.splitString( x )[1];
        int id;
        try {
            id = Integer.parseInt( id_str );
        } catch( Exception e ) {
            e.printStackTrace();
            connexion.write( Converter.convert( "DUNNO***" ) );
            return;
        }
        for( Partie partie : Serveur.listePartie ) {
            if( partie.getID() == id ) {
                partie.listePartie( connexion );
                return;
            }
        }
        connexion.write( Converter.convert( "DUNNO***" ) );
    }

}
