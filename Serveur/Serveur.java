import java.io.*;
import java.net.*;
//import java.nio.ByteBuffer;
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

            Joueur joueur = new Joueur("Invite00", this.socket,null,false,new DatagramSocket(Utilitaire.randomPort()));
            joueur.creationProfile(); //vérification du pseudo
            listeJoueur.add(joueur);

            //envoie des listes de partie
            pw.write("GAMES " + Partie.getNbPartie(listePartie) + "***");
            pw.flush();

            //envoie liste partie avec id 
            Partie.envoyerListePartie(joueur,listePartie);

            while(true){ //boucle pour commande
                Thread.sleep(1000);
                String x = Utilitaire.lecture2(br);
                if(this.socket.isClosed()){return ;}
                System.out.println("|" + x + "|");
                if(x.equals("GAME?***") && !joueur.getReady()){ //GAMES*** affiche le nb de partie non lancé
                    pw.write("GAMES " + Partie.getNbPartie(listePartie) + "***"); pw.flush();
                    Partie.envoyerListePartie(joueur, listePartie);
                }else if(x.startsWith("NEWPL") && x.endsWith("***") && !joueur.getReady()){
                    if(joueur.getPartie() == null){ // NEWPL id port*** crée une partie (id = id joueur)
                        Partie p = joueur.creerPartie(x);
                        if(p == null){
                            pw.write("REGNO***"); pw.flush();
                        }else{
                            joueur.rejoindrePartie(x);
                            listePartie.add(p);
                            pw.write("REGOK" + p.getM() + "***");
                        }
                    }else{pw.write("REGNO***"); pw.flush();}

                }else if(x.startsWith("REGIS") && x.endsWith("***") && !joueur.getReady()){
                        if(joueur.rejoindrePartie(x)){  //REGIS id port m***
                            pw.write("REGOK***");pw.flush();
                        }else{pw.write("REGNO***");pw.flush();}

                }else if(x.startsWith("CONNECT") && x.endsWith("***") && !joueur.getReady()){ //version avec moins de Paramètre
                    if(joueur.connexionSimple(x) == true ){ // CONNECT NOMPARTIE***
                        pw.write("REGOK***");pw.flush();
                    }else{pw.write("REGNO***");pw.flush();}

                }else if(x.equals("UNREG***") && !joueur.getReady()){ //UNREG*** quitte la partie 
                    pw.write("UNROK" + " " + joueur.getPartie().getM() + "***"); pw.flush();
                    joueur.getPartie().retirerJoueur(joueur);
                    joueur.setPartie(null);
                }else if(x.equals("START***") && joueur.getPartie() != null) { //START*** bloque dans la game,attends le lancement
                    joueur.setReady(true);
                    if(joueur.getPartie().tousPret()){
                        System.out.println("tout le monde est prêt");
                    }
                }else if(x.startsWith("LIST?") && x.endsWith("***") ){ //LIST? numPartie*** affiche les joueurs de la 
                        joueur.listePartie(x, pw);                      // partie demandé

                }else if(x.startsWith("SEND?") && x.endsWith("***") && joueur.getPartie().getLancer()) {   
                    joueur.chatter(x);

                }else if(x.startsWith("DISC!") && x.endsWith("***") && joueur.getPartie() == null) { // se deconnecte 
                    listeJoueur.remove(joueur);
                    joueur.getPort().close();
                    joueur.getSocket().close();
                    return ;
                }else if(x.equals("-1")){//si le joueur a crash
                        System.out.println(joueur.getPseudo() + " has disconnected");
                        return ;
                }else{
                    pw.write("DUNNO***");  pw.flush();
                }
            }

        }catch(Exception e){
            e.printStackTrace();
        }
    }


    public static void main(String[] args){
        try {
            Joueur j1 = new Joueur("Vladimir",null,null,false,new DatagramSocket(4242));
            Joueur j2 = new Joueur("Aypierre",null,null,false,new DatagramSocket(4243));
            Joueur j3 = new Joueur("Zelenski",null,null,false,new DatagramSocket(4244));
            ArrayList<Joueur> p2 = new ArrayList<>();
            
    
            Partie test1 = new Partie(listeJoueur, "4000", "Vladimir", false, 2,32);
            Partie test2 = new Partie(p2, "4242", "Zelenski", false, 2,32);
            test1.ajouterJoueur(j1); test1.ajouterJoueur(j2); 
            test2.ajouterJoueur(j3);
            listePartie.add(test1);
            listePartie.add(test2);


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
