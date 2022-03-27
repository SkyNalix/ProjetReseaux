import java.io.*;
import java.net.*;
import java.util.ArrayList;

public class Serveur implements Runnable{

    private Socket socket;
    public Serveur(Socket socket){
        this.socket = socket;
    }

    public static ArrayList<Joueur> listeJoueur = new ArrayList<>();
    public static ArrayList<Partie> listePartie = new ArrayList<>();

    @Override
    public void run() {
        try {
            BufferedReader br = new BufferedReader(new InputStreamReader(this.socket.getInputStream()));
            PrintWriter pw = new PrintWriter(this.socket.getOutputStream());           
            /*connexion :
                1) message d'acceuil
                2) choix connexion/creation profil
                
            */
            pw.write("Bienvenue ! entrer un pseudo de 8 caractères svp "); 
            pw.flush();
            String pseudo = br.readLine(); // on a le droit à readLine ?
            System.out.println(pseudo);

            Joueur joueur = new Joueur(pseudo, this.socket,null);
            joueur.creationProfile(); //vérification du pseudo
            
            //envoie des listes de partie
            pw.write("GAMES " + Partie.getNbPartie(listePartie) + "***");
            pw.flush();
            //envoie liste partie avec id 
            Partie.envoyerListePartie(joueur,listePartie);

            while(true){ //boucle pour commande
                String x = br.readLine();
                switch(x){
                     case "GAMES": pw.write("GAMES " + Partie.getNbPartie(listePartie) + "***"); pw.flush(); break;
                }
            }

        }catch(Exception e){
            e.printStackTrace();
        }
    }


    public static void main(String[] args){

        Joueur j1 = new Joueur("Test0000",null,null);
        Joueur j2 = new Joueur("Test0001",null,null);
        Joueur j3 = new Joueur("Test0002",null,null);
        

        Partie test1 = new Partie(listeJoueur, "4000", "0", false, 2);
        test1.ajouterJoueur(j1); test1.ajouterJoueur(j2); test1.ajouterJoueur(j1);
        listePartie.add(test1);
        
        try {
            ServerSocket servSocket = new ServerSocket(4243);
            System.out.println("lancé !");
            while(true){
                Socket socket = servSocket.accept();
                Serveur serv = new Serveur(socket);
                Thread t = new Thread(serv);
                t.start();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
