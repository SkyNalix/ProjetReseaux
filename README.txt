# Projet Reseaux - groupe 45

## Compilation:
Il suffit de faire 'make' au répertoire racine du projet

## Execution
Pour lancer le serveyr il suffit faire 'java serveur/Serveur' à la racine du projet
Et pour le client, l'executable est egalement à la racine du projet, pour lancer faites './client p' où p est la port sur lequel est lancé le serveur
Le serveur et le client ont chacun quelques arguments, ajoutez un '-h' pour les voir

## Utilisation
Pour utiliser notre client, il suffit d'entrer l'entete de l'instruction que vous voulez envoyer
Par exemple si je veux envoyer 'NEWPL username 5555'
Je rentre d'abord 'NEWPL', puis je suis les instructions sur le terminal pour rentrer mon id et mon port udp

## Architecture
On a choisit de faire notre serveur avec des threads.
Au moment de connection d'un client on fait un thread responsable de la communication avec ce joueur
Quand le client envoit une requete 'NEWPL' ou 'REGIS' valide on créé un objet Joueur, qui est inscrit dans la partie joueur.getPartie()
Le id et le port d'un joueur est unique dans la partie où il est inscrit
Quand tous les joueurs ont envoyé 'START' on fait un nouveau objet Jeu, qui lui meme va faire un objet Labyrinthe
et aussi l'objet Affichage si l'environnement où le serveur est lancé permet de lancer une interface Swing et si l'option '--nogui' n'a pas été donnée
A partir de là toutes les minutes les fantomes vont changer de place, tous les joueurs peuvents se deplacer sur toutes les cases vides
sans colision entre les joueurs

Sur le client, pour regler le probleme de l'affichage des messages au même moment où on attend une entrée nous avons utilisé Ncurses
sur le client nous avont simplement 2 threads, un qui lit sur le port udp du joueur et l'autre sur l'addresse multicast de la partie
