#include <sys/socket.h>
#include <netinet/in.h>
#include <stdlib.h>
#include <unistd.h>
#include <stdio.h>
#include <string.h>
#include <netdb.h>
#include <pthread.h>
#include <arpa/inet.h>
#include <ncurses.h>
#include <signal.h>

#define BUFF_SIZE 200
#define MESS_SIZE 300
int localhost = 1;
int debug = 0;
int sock;
pthread_t multicast_thread;
pthread_t udp_thread;

// variables pour l'affichage avec Ncurses
char stdin_buff[300];
short input_x = 0;
short print_y = 0;
short max_y = 0;

pthread_mutex_t verrou = PTHREAD_MUTEX_INITIALIZER;

int while_continue = 1;

void sig_handler() {
    while_continue = 0;
}

void print(char *fmt, ...) {
    va_list args;
    va_start(args, fmt);
    char txt[1024];
    vsprintf(txt, fmt, args);
    va_end(args);
    pthread_mutex_lock(&verrou);
    if (print_y == max_y) {
        move(print_y - 1, 0);
        clrtoeol();
        printw("%s\n", txt);
    } else {
        move(print_y, 0);
        clrtoeol();
        printw("%s\n", txt);
        print_y += 1;
    }
    move(print_y, 0);
    clrtoeol();
    printw("%s\n", stdin_buff);
    move(print_y, input_x);
    refresh();

    pthread_mutex_unlock(&verrou);
}

uint16_t little16toh(uint8_t b, uint8_t b2) {
    uint16_t t = 3;
    if (htons(t) == 3) { // si je sui en big endian
        print("test");
        return (b >> 8) | (b2 << 8);
    }
    return b | b2;
}


void closeConnection(int exitCode, char *error) {
    if (error != NULL) {
        perror(error);
    }
    endwin();
    close(sock);
    exit(exitCode);
}

char *cutEnd(char *s) {
    s[strlen(s)] = '\0';
    return s;
}

ssize_t receive(char *buff, int n) {
    buff = strcpy(buff, "");
    ssize_t sz = recv(sock, buff, n, 0);
    buff[sz] = '\0';
    if (debug)
        print("[DEBUG] sz= %d, recu: '%s'", sz, buff);
    return sz;
}

ssize_t readInput(char *stockIci) {
    while (1) {
        int ch = getch();
        if (!while_continue)
            return -1;
        if (ch == ERR)
            return -1;
        if (max_y == print_y)
            move(print_y - 1, 0);
        else
            move(print_y, 0);
        clrtoeol();
        if (ch == 10) { // touche entrer
            strcpy(stockIci, stdin_buff);
            int size = input_x;
            input_x = 0;
            strcpy(stdin_buff, "");
            printw("%s", stdin_buff);
            refresh();
            return size;
        } else if (ch == 127) { // shift
            stdin_buff[input_x] = '\0';
            if (input_x > 0)
                input_x -= 1;
        } else {
            const char *key = keyname(ch);
            if (strlen(key) == 1 && key[0] != '^' && key[0] != '[') {
                strcat(stdin_buff, keyname(ch));
                input_x += 1;
            }
        }
        printw("%s", stdin_buff);
        refresh();
    }
}

void getGamesList(char *str) {

    uint8_t n;
    memcpy(&n, str + 6, sizeof(uint8_t));
    print("%d parties trouvees", n);
    char tmp[BUFF_SIZE];
    ssize_t sz;
    for (int i = 0; i < (int) n; ++i) {
        sz = receive(tmp, 12); // [OGAME␣m␣s***]
        if (sz == 12 && strncmp(tmp, "OGAME", 5) == 0) {
            uint8_t m;
            memcpy(&m, tmp + 6, sizeof(uint8_t));
            uint8_t s;
            memcpy(&s, tmp + 8, sizeof(uint8_t));
            print("\tpartie numero %d, avec %d joueurs", m, s);
        }
    }
}

char *subString(char *s, int off, int sz) {
    char *res = malloc(sz);
    memcpy(res, s + off, sz);
    return res;
}

void *multicastThread(void *arg) {
    char (*args)[20] = arg;
    if (debug)
        print("[MULTICAST] ip = %s, port = %s", args[0], args[1]);
    int multicast_sock = socket(PF_INET, SOCK_DGRAM, 0);
    int ok = 1;
    int r;
    if (localhost)
        r = setsockopt(multicast_sock, SOL_SOCKET, SO_REUSEPORT, &ok, sizeof(ok));
    else
        r = setsockopt(multicast_sock, SOL_SOCKET, SO_REUSEADDR, &ok, sizeof(ok));
    if (r != 0) {
        print("[ERROR] vous n'allez pas recevoir les messages globales");
        pthread_exit((void *) EXIT_FAILURE);
    }
    struct sockaddr_in address_sock;
    address_sock.sin_family = AF_INET;
    address_sock.sin_port = htons(atoi(args[1]));
    address_sock.sin_addr.s_addr = htonl(INADDR_ANY);
    r = bind(multicast_sock, (struct sockaddr *) &address_sock, sizeof(struct sockaddr_in));
    if (r != 0) {
        print("[ERROR] vous n'allez pas recevoir les messages globales");
        pthread_exit((void *) EXIT_FAILURE);
    }
    struct ip_mreq mreq;
    mreq.imr_multiaddr.s_addr = inet_addr(args[0]);
    mreq.imr_interface.s_addr = htonl(INADDR_ANY);
    r = setsockopt(multicast_sock, IPPROTO_IP, IP_ADD_MEMBERSHIP, &mreq, sizeof(mreq));
    if (r != 0) {
        print("[ERROR] vous n'allez pas recevoir les messages globales");
        pthread_exit((void *) EXIT_FAILURE);
    }
    char tampon[300];
    while (while_continue) {
        ssize_t rec = recv(multicast_sock, tampon, 299, 0);
        if (rec < 3) continue;
        if (rec < 0) break;
        tampon[rec - 3] = '\0';
        if (debug)
            print("[DEBUG MULTICAST] sz= %d, recu: '%s'", rec, tampon);
        if (rec == 16 && strncmp(tampon, "GHOST", 5) == 0) { // [GHOST␣x␣y+++]
            print("Une fantome s'est deplace a (%s, %s)", subString(tampon, 6, 3),
                  subString(tampon, 10, 3));
        } else if (rec == 22 && strncmp(tampon, "ENDGA", 5) == 0) { // [ENDGA␣id␣p+++]
            print("Partie terminee, %s a gagne avec %s points", subString(tampon, 6, 8),
                  subString(tampon, 15, 4));
            print("Appuyez sur n'importe quelle touche pour quitter");
            while_continue = 0;
            break;
        } else if (rec > 15 && strncmp(tampon, "MESSA", 5) == 0) { // [MESSA␣id␣mess+++]
            print("[GLOBAL] %s: %s", subString(tampon, 6, 8),
                  subString(tampon, 15, ((int) rec) - 18));
        } else if (rec == 30 && strncmp(tampon, "SCORE", 5) == 0) { // [SCORE␣id␣p␣x␣y+++]
            print("Le joueur %s a attrape un fantome a (%s, %s) son score est %s",
                  subString(tampon, 6, 8),
                  subString(tampon, 20, 3),
                  subString(tampon, 24, 3),
                  subString(tampon, 15, 4));
        }
    }
    close(multicast_sock);
    pthread_exit(EXIT_SUCCESS);
}

void *udpThread(void *arg) { //serveur udp pour recevoir message
    in_port_t udp_port = *((int *) arg);
    if (debug)
        print("[UDP] port = %d", udp_port);
    int udp_sock = socket(PF_INET, SOCK_DGRAM, 0);
    struct sockaddr_in address_sock;
    address_sock.sin_family = AF_INET;
    address_sock.sin_port = htons(udp_port);
    address_sock.sin_addr.s_addr = htonl(INADDR_ANY);
    int r = bind(udp_sock, (struct sockaddr *) &address_sock, sizeof(struct sockaddr_in));
    if (r != 0) {
        print("[ERROR] Vous n'allez pas recevoir les messages privés");
        pthread_exit((void *) EXIT_FAILURE);
    }
    char tampon[300];
    while (while_continue) {
        ssize_t rec = recv(udp_sock, tampon, 299, 0);
        if (rec < 3) continue;
        if (rec < 0) break;
        tampon[rec - 3] = '\0';
        if (debug)
            print("[DEBUG UDP] sz= %d, recu: '%s'", rec, tampon);
        if (rec > 15 && strncmp(tampon, "MESSP", 5) == 0) { // [MESSP␣id2␣mess+++]
            print("[PRIVE] %s: %s", subString(tampon, 6, 8),
                  subString(tampon, 15, ((int) rec) - 18));
        }
    }
    pthread_exit(EXIT_SUCCESS);
}

int main(int argc, char **argv) {
    char *str_port = "4243";
    char address[100] = "localhost";
    if (argc >= 2) {
        char *error;
        int newport = (int) strtol(argv[1], &error, 10);
        if (strcmp(error, "\0") == 0) {
            if (newport > 1024 && newport < 49151) {
                str_port = argv[1];
            }
        }
    }
    for (int i = 0; i < argc; ++i) {
        if(strcmp(argv[i], "-h") == 0) {
            printf("Lancement basique: ./client 4243\n");
            printf("Options: \n\t--debug : affiche les messages de debug\n");
            printf("\t--address : pour entrer une addresse où se connecter\n");
            printf("\tExemple: ./client 4243 --address=lulu.informatique.univ-paris-diderot.fr\n");
        } else if (strcmp(argv[i], "--debug") == 0) {
            debug = 1;
        } else if (strncmp(argv[i], "--address=", 10) == 0) {
            strcpy(address, argv[i] + 10);
            if (strcmp(address, "localhost") != 0)
                localhost = 0;
        }
    }

    struct addrinfo *info;
    struct addrinfo hints;
    memset(&hints, 0, sizeof(hints));
    hints.ai_family = AF_INET;
    hints.ai_socktype = SOCK_STREAM;
    int r = getaddrinfo(address, str_port, &hints, &info);
    if (r != 0) {
        perror("addrinfo != 0");
        exit(EXIT_FAILURE);
    }
    if (info == NULL) {
        perror("info == NULL");
        exit(EXIT_FAILURE);
    }

    sock = socket(PF_INET, SOCK_STREAM, 0);
    if (sock == -1) {
        perror("socket");
        exit(EXIT_FAILURE);
    }

    r = connect(sock, (struct sockaddr *) info->ai_addr, sizeof(struct sockaddr_in));
    if (r != 0) {
        close(sock);
        perror("connect");
        exit(EXIT_FAILURE);
    }

    // pour fermer normalement le programme quand l'utilisateur fait CTRL+C or CTRL+Z
    struct sigaction sig_action;
    sig_action.sa_handler = sig_handler;
    sig_action.sa_flags = 0;
    sigaction(SIGINT, &sig_action, NULL);
    sigaction(SIGTSTP, &sig_action, NULL);

    // L'affichage avec Ncurses
    struct _win_st *my_win = initscr();
    scrollok(my_win, TRUE);
    noecho();
    refresh();
    max_y = my_win->_maxy;

    char buff[BUFF_SIZE];
    char tosend[MESS_SIZE];
    int in_party = 0;
    int in_game = 0; // est 1 que si in_party==1
    char *tmp = malloc(BUFF_SIZE);
    ssize_t sz;

    sz = receive(tmp, 10);
    if (sz == 10 && strncmp(tmp, "GAMES", 5) == 0) {
        getGamesList(tmp);  // [GAMES␣n***]
    }

    while (while_continue) {
        strcpy(buff, "");
        memset(tosend, 0, MESS_SIZE);

        print("Entrez le debut de la requete que vous voulez ecrire");
        if (readInput(buff) == -1) break;

        if (strcmp(buff, "NEWPL") == 0) { // [NEWPL␣id␣port***]
            if (in_party) {
                print("[ERROR] Vous etes deja dans une partie");
                continue;
            }
            char id[BUFF_SIZE];
            print("Entrez votre id");
            readInput(id);

            print("Entrez votre port");
            char tmpPort[BUFF_SIZE];
            readInput(tmpPort);
            in_port_t port2 = (in_port_t) atoi(tmpPort);

            sprintf(tosend, "NEWPL %s %d***", id, port2);
            if (debug)
                print("[DEBUG] envoi: '%s'", tosend);

            send(sock, tosend, strlen(tosend), 0);
            sz = receive(tmp, 10);
            if (sz == 10 && strncmp(tmp, "REGOK", 5) == 0) { // [REGOK␣m***]
                uint8_t m;
                memcpy(&m, tmp + 6, sizeof(uint8_t));
                print("Partie %d creee", m);
                pthread_create(&udp_thread, NULL, udpThread, (void *) &port2);
                in_party = 1;
            } else {
                print("[ERROR] Partie non creee");
            }
        } else if (strcmp(buff, "REGIS") == 0) { // [REGIS␣id␣port␣m***]
            if (in_party) {
                print("[ERROR] Vous etes deja dans une partie");
                continue;
            }
            char id[9];
            print("Entrez votre id");
            readInput(id);
            print("Entrez votre port");
            char tmpPort[5];
            readInput(tmpPort);
            in_port_t port2 = (in_port_t) atoi(tmpPort);
            print("Entrez le numero de la partie");
            readInput(buff);
            uint8_t m = atoi(buff);
            sprintf(tosend, "REGIS %s %d ", id, port2);
            memcpy(tosend + 20, &m, sizeof(uint8_t));
            memcpy(tosend + 21, "***", sizeof(char) * 3);
            send(sock, tosend, 24, 0);
            sz = receive(tmp, 10);
            if (sz == 10 && strncmp(tmp, "REGOK", 5) == 0) { //  [REGOK␣m***]
                memcpy(&m, tmp + 6, sizeof(uint8_t));
                print("Partie %d rejoint", m);
                pthread_create(&udp_thread, NULL, udpThread, (void *) &port2);
                in_party = 1;
            } else {
                print("[ERROR] La partie %s n'a pas ete rejoint", buff);
            }
        } else if (strcmp(buff, "START") == 0) { // [START***]
            if (!in_party) {
                print("[ERROR] Vous n'etes pas dans une partie");
                continue;
            }
            if (in_game) {
                print("[ERROR] La partie a deja commencee");
                continue;
            }
            strcpy(tosend, "START***");
            if (debug)
                print("[DEBUG] envoi: '%s'", tosend);
            send(sock, tosend, strlen(tosend), 0);
            print("Vous etes pret, attente des autres joueurs");
            sz = receive(tmp, 39);
            if (sz == 39 && strncmp(tmp, "WELCO", 5) == 0) { // [WELCO␣m␣h␣w␣f␣ip␣port***]
                tmp = cutEnd(tmp);
                uint8_t m;
                memcpy(&m, tmp + 6, sizeof(uint8_t));
                uint16_t h;
                h = little16toh(tmp[8], tmp[9]);
                uint16_t w;
                w = little16toh(tmp[11], tmp[12]);
                uint8_t f;
                memcpy(&f, tmp + 14, sizeof(uint8_t));
                char *ip = subString(tmp, 16, 15);
                for (int i = 14; i >= 0; i--) { // enleve les '#' de la fin de l'ip
                    if (ip[i] == '#') {
                        ip[i] = '\0';
                    } else
                        break;
                }
                print("La partie %d a commencee, le jeu est de taille %dx%d, et il y a %d fantomes", m, h, w,
                      f);
                in_game = 1;
                char args[2][20];
                strcpy(args[0], ip);
                strcpy(args[1], subString(tmp, 32, 4));
                pthread_create(&multicast_thread, NULL, multicastThread, (void *) &args);
                sz = receive(tmp, 25); // [POSIT␣id␣x␣y***]
                if (sz == 25 && strncmp(tmp, "POSIT", 5) == 0) {
                    tmp = cutEnd(tmp);
                    print("Ta position sur le plateau est (%s, %s)",
                          subString(tmp, 15, 3),
                          subString(tmp, 19, 3));
                } else
                    print("[ERROR] position initiale non recu");
            } else
                print("[ERROR] DUNNO");
        } else if (strcmp(buff, "UNREG") == 0) { // [UNREG***]
            if (!in_party) {
                print("[ERROR] Vous n'etes pas dans une partie, essayez plutot DISC!");
                continue;
            }
            if (in_game) {
                print("[ERROR] La partie a deja commencee, essayez plutot IQUIT");
                continue;
            }
            strcpy(tosend, "UNREG***");
            if (debug)
                print("[DEBUG] envoi: '%s'", tosend);
            send(sock, tosend, strlen(tosend), 0);
            sz = receive(tmp, 10);
            if (sz == 10 && strncmp(tmp, "UNROK", 5) == 0) { // [UNROK␣m***]
                uint8_t m;
                memcpy(&m, tmp + 6, sizeof(uint8_t));
                print("Partie %d quitte", m);
                in_game = 0;
                in_party = 0;
            } else {
                print("[ERROR] DUNNO");
            }
        } else if (strcmp(buff, "SIZE?") == 0) { // [SIZE?␣m***]
            print("Entrez le numero de la partie");
            readInput(buff);
            uint8_t num_partie = atoi(buff);

            if (debug)
                print("[DEBUG] envoi: 'SIZE? %d***'", num_partie);
            memcpy(tosend, "SIZE? ", sizeof(char) * 6);
            memcpy(tosend + 6, &num_partie, sizeof(uint8_t));
            memcpy(tosend + 7, "***", sizeof(char) * 3);
            send(sock, tosend, sizeof(char) * 10, 0);

            sz = receive(tmp, 16);
            if (sz == 16 && strncmp(tmp, "SIZE!", 5) == 0) { // [SIZE!␣m␣h␣w***]
                uint8_t m;
                memcpy(&m, tmp + 6, sizeof(uint8_t));
                uint16_t h;
                h = little16toh(tmp[8], tmp[9]);
                uint16_t w;
                w = little16toh(tmp[11], tmp[12]);
                print("La partie %d est de taille %dx%d", m, h, w);
            } else {
                print("[ERROR] DUNNO");
            }
        } else if (strcmp(buff, "LIST?") == 0) { // [LIST? m***]
            print("Entrez le numero de la partie");
            readInput(buff);
            uint8_t m = atoi(buff);

            memcpy(tosend, "LIST? ", 6);
            memcpy(tosend + 6, &m, sizeof(uint8_t));
            memcpy(tosend + 7, "***", 3);
            send(sock, tosend, 10, 0);

            sz = receive(tmp, 12);
            if (sz == 12 && strncmp(tmp, "LIST!", 5) == 0) { // [LIST!␣m␣s***]
                memcpy(&m, tmp + 6, sizeof(uint8_t));
                uint8_t s;
                memcpy(&s, tmp + 8, sizeof(uint8_t));
                print("Liste des joueurs dans la partie %d:", m);
                for (int i = 0; i < s; ++i) {
                    sz = receive(tmp, 17);
                    if (sz == 17 && strncmp(tmp, "PLAYR", 5) == 0) {
                        print("\t- %s", subString(cutEnd(tmp), 6, 8));
                    }
                }
            } else {
                print("[ERROR] DUNNO");
            }
        } else if (strcmp(buff, "GAME?") == 0) { // [GAME? m***]
            strcpy(tosend, "GAME?***");
            if (debug)
                print("[DEBUG] envoi: '%s'", tosend);
            send(sock, tosend, strlen(tosend), 0);
            sz = receive(tmp, 10);
            if (sz == 10 && strcmp(tmp, "DUNNO***") != 0) {
                getGamesList(tmp); // [GAMES␣n***]
            }

        } else if (strcmp(buff, "UPMOV") == 0 || // [UPMOV␣d***]
                   strcmp(buff, "DOMOV") == 0 || // [DOMOV␣d***]
                   strcmp(buff, "LEMOV") == 0 || // [LEMOV␣d***]
                   strcmp(buff, "RIMOV") == 0    // [RIMOV␣d***]
                ) {
            if (!in_party) {
                print("[ERROR] Vous n'etes pas dans une partie");
                continue;
            }
            if (!in_game) {
                print("[ERROR] La partie n'a pas encore commencee");
                continue;
            }
            strcpy(tosend, buff);
            strcat(tosend, " ");
            char pas[BUFF_SIZE];
            print("Entrez le nombre de pas");
            readInput(pas);
            for (int i = 0; i < 3 - ((int) strlen(pas)); i++) {
                strcat(tosend, "0");
            }
            strcat(tosend, pas);
            strcat(tosend, "***");
            if (debug)
                print("[DEBUG] envoi: '%s'", tosend);
            send(sock, tosend, strlen(tosend), 0);

            sz = receive(tmp, 21);
            if (sz == 16 && strncmp(tmp, "MOVE!", 5) == 0) { // [MOVE!␣x␣y***]
                tmp = cutEnd(tmp);
                print("Vous etes maintenant a (%s, %s)",
                      subString(tmp, 6, 3), subString(tmp, 10, 3));
            } else if (sz == 21 && strncmp(tmp, "MOVEF", 5) == 0) { // [MOVEF␣x␣y␣p***]
                tmp = cutEnd(tmp);
                print("Vous etes maintenant a (%s, %s)",
                      subString(tmp, 6, 3), subString(tmp, 10, 3));
                print("Vous avez attrape un fantome! Votre score est %s", subString(tmp, 14, 4));
            } else {
                print("[ERROR] DUNNO");
            }
        } else if (strcmp(buff, "IQUIT") == 0) { // [IQUIT***]
            if (!in_party) {
                print("[ERROR] Vous n'etes pas dans une partie, essayez plutot DISC!");
            }
            if (!in_game) {
                print("[ERROR] La partie n'a pas encore commencee, essayez plutot UNREG");
                continue;
            }
            strcpy(tosend, "IQUIT***");
            if (debug)
                print("[DEBUG] envoi: '%s'", tosend);
            send(sock, tosend, strlen(tosend), 0);
            sz = receive(tmp, 8);
            print("%s", tmp); // [GOBYE***]
            closeConnection(EXIT_SUCCESS, NULL);
        } else if (strcmp(buff, "GLIS?") == 0) { // [GLIS?***]
            if (!in_party) {
                print("[ERROR] Vous n'etes pas dans une partie");
                continue;
            }
            strcpy(tosend, "GLIS?***");
            if (debug)
                print("[DEBUG] envoi: '%s'", tosend);
            send(sock, tosend, strlen(tosend), 0);
            sz = receive(tmp, 10);
            if (sz == 10 && strncmp(tmp, "GLIS!", 5) == 0) { // [GLIS!␣s***]
                uint8_t s;
                memcpy(&s, tmp + 6, sizeof(uint8_t));
                print("Les joueurs present dans la partie:");
                for (int i = 0; i < (int) s; ++i) {
                    sz = receive(tmp, 30); // [GPLYR␣id␣x␣y␣p***]
                    if (sz == 30 && strncmp(tmp, "GPLYR", 5) == 0) {
                        tmp = cutEnd(tmp);
                        // GPLYR username 007 006 0
                        print("\t- %s, position (%s, %s), score: %s",
                              subString(tmp, 6, 8),
                              subString(tmp, 15, 3),
                              subString(tmp, 19, 3),
                              subString(tmp, 23, 4));
                    }
                }
            }
            if (strncmp(tmp, "DUNNO", 5) == 0) {
                print("[ERROR] DUNNO");
            }
        } else if (strcmp(buff, "MALL?") == 0) { // [MALL?␣mess***]
            if (!in_party) {
                print("[ERROR] Vous n'etes pas dans une partie");
                continue;
            }
            if (!in_game) {
                print("[ERROR] Le chat global est accessible uniquement quand la partie a commencé");
                continue;
            }
            print("Entrez le message a envoyer a tous les joueurs");
            readInput(buff);
            sprintf(tosend, "MALL? %s***", buff);
            if (debug)
                print("[DEBUG] envoi: '%s'", tosend);
            send(sock, tosend, strlen(tosend), 0);
            sz = receive(tmp, 8);
            if (sz == 8 && strncmp(tmp, "MALL!", 5) == 0) { // [MALL!***]
                print("Message envoye");
            } else {
                print("[ERROR] Message non envoye");
            }
        } else if (strcmp(buff, "SEND?") == 0) { // [SEND?␣id␣mess***]
            if (!in_party) {
                print("[ERROR] Vous n'etes pas dans une partie");
                continue;
            }
            char id[10];
            char message[200];
            print("Entrez le id du destinataire");
            readInput(id);
            print("Entrez le message");
            readInput(message);
            sprintf(tosend, "SEND? %s %s***", id, message);
            if (debug)
                print("[DEBUG] envoi: '%s'", tosend);
            send(sock, tosend, strlen(tosend), 0);
            sz = receive(tmp, 8);
            if (sz == 8 && strncmp(tmp, "SEND!", 5) == 0) { //  [SEND!***]
                print("Message envoye");
            } else if (strncmp(tmp, "NSEND", 5) == 0) { // [NSEND***]
                print("[ERROR] Message non envoye");
            }
        } else if (strcmp(buff, "DISC!") == 0) {
            if (in_party) {
                print("[ERROR] Vous etes deja dans une partie, essayez plutot UNREG");
                continue;
            }
            if (in_game) {
                print("[ERROR] La partie a deja commencee, essayez plutot IQUIT");
                continue;
            }
            strcpy(tosend, "DISC!***");
            if (debug)
                print("[DEBUG] envoi: '%s'", tosend);
            send(sock, tosend, strlen(tosend), 0);
            sz = receive(tmp, 8);
            print("%s", tmp); // [GOBYE***]
            closeConnection(EXIT_SUCCESS, NULL);
            closeConnection(EXIT_SUCCESS, NULL);
        } else {
            int testMov = 1;
            if (strcmp(buff, "z") == 0) {
                strcat(tosend, "UPMOV");
            } else if (strcmp(buff, "q") == 0) {
                strcat(tosend, "LEMOV");
            } else if (strcmp(buff, "s") == 0) {
                strcat(tosend, "DOMOV");
            } else if (strcmp(buff, "d") == 0) {
                strcat(tosend, "RIMOV");
            } else {
                testMov = 0;
                print("[ERROR] Reessayez");
            }
            if (testMov) {
                strcat(tosend, " 001***");
                if (debug)
                    print("[DEBUG] envoi: '%s'", tosend);
                send(sock, tosend, strlen(tosend), 0);

                sz = receive(tmp, 21);
                if (sz == 16 && strncmp(tmp, "MOVE!", 5) == 0) { // [MOVE!␣x␣y***]
                    tmp = cutEnd(tmp);
                    print("Vous etes maintenant a (%s, %s)",
                          subString(tmp, 6, 3), subString(tmp, 10, 3));
                } else if (sz == 21 && strncmp(tmp, "MOVEF", 5) == 0) { // [MOVEF␣x␣y␣p***]
                    tmp = cutEnd(tmp);
                    print("Vous etes maintenant a (%s, %s)",
                          subString(tmp, 6, 3), subString(tmp, 10, 3));
                    print("Vous avez attrape un fantome! Votre score est %s", subString(tmp, 14, 4));
                } else {
                    print("[ERROR] DUNNO");
                }
            }
        }
    }
    closeConnection(EXIT_SUCCESS, NULL);
}

