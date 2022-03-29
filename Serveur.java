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

            Joueur joueur = new Joueur(pseudo, this.socket,null,false);
            joueur.creationProfile(); //vérification du pseudo
            
            //envoie des listes de partie
            pw.write("GAMES " + Partie.getNbPartie(listePartie) + "***");
            pw.flush();

            //envoie liste partie avec id 
            Partie.envoyerListePartie(joueur,listePartie);

            while(true){ //boucle pour commande
                String x = br.readLine();

                if(x.equals("GAMES")){
                    pw.write("GAMES " + Partie.getNbPartie(listePartie) + "***"); pw.flush();
                }else if(x.startsWith("OGAMES") && x.endsWith("***")){
                    Partie.envoyerListePartie(joueur, listePartie);
                }else if(x.startsWith("NEWPL") && x.endsWith("***")){
                    if(joueur.getPartie() == null){
                        Partie p = joueur.creerPartie(x);
                        joueur.rejoindrePartie(x);
                        listePartie.add(p);
                    }else{pw.write("REGNO***"); pw.flush();}

                }else if(x.startsWith("REGIS") && x.endsWith("***")){
                        if(joueur.rejoindrePartie(x)){
                            pw.write("REGOK***");pw.flush();
                        }else{pw.write("REGNO***");pw.flush();}

                }else if(x.startsWith("CONNECT") && x.endsWith("***")){ //version avec moins de Paramètre
                    if(joueur.connexionSimple(x) == true ){ // CONNECT NOMPARTIE***
                        pw.write("REGOK***");pw.flush();
                    }else{pw.write("REGNO***");pw.flush();}

                }else if(x.startsWith("START")  && x.endsWith("***")){
                    joueur.setReady(true);
                    if(joueur.getPartie().tousPret()){
                        //joueur.getPartie.launch() Broadcast?
                    }

                }else if(x.startsWith("INVITE") && x.endsWith("***")){
                    
                }else{
                    pw.write("DUNNO***");  pw.flush();
                }
            }

        }catch(Exception e){
            e.printStackTrace();
        }
    }


    public static void main(String[] args){

        Joueur j1 = new Joueur("Test0000",null,null,false);
        Joueur j2 = new Joueur("Test0001",null,null,false);
        Joueur j3 = new Joueur("Test0002",null,null,false);
        ArrayList<Joueur> p2 = new ArrayList<>();
        

        Partie test1 = new Partie(listeJoueur, "4000", "test", false, 2,0);
        Partie test2 = new Partie(p2, "4242", "1v1", false, 2,1);
        test1.ajouterJoueur(j1); test1.ajouterJoueur(j2); 
        test2.ajouterJoueur(j3);
        listePartie.add(test1);
        listePartie.add(test2);
        
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
