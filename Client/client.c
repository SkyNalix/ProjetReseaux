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
char **tab;
pthread_t multicast_thread;
pthread_t udp_thread;

// variables pour l'affichage avec Ncurses
char stdin_buff[1024];
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

uint16_t host16ToLittleEndian(uint16_t b) {
    uint16_t t = 3;
    if (htons(t) == 3) { // si je sui en big endian
        return (b >> 8) | (b << 8);
    }
    return b;
}

uint16_t littleEndian16ToHost(uint16_t b) {
    uint16_t t = 3;
    if (htons(t) == 3) { // si je sui en big endian
        return (b >> 8) | (b << 8);
    }
    return b;
}


void closeConnection(int exitCode, char *error) {
    if (error != NULL) {
        perror(error);
    }
    endwin();
    close(sock);
    exit(exitCode);
}

uint8_t stringToUint8(const char *s) {
    if (strcmp(s, "0") == 0)
        return 0;
    else
        return s[0];
}

uint16_t stringToUint16(const char *s) {
    if (strcmp(s, "0") == 0)
        return 0;
    else
        return (unsigned char) (s[0] << 8) | s[1];
}

char *convertString(char *s, ssize_t len) {
    char *res = malloc(40);
    if (len == 1) {
        sprintf(res, " %d", stringToUint8(s));
    } else if (len == 2) {
        sprintf(res, " %d", stringToUint16(s));
    } else {
        sprintf(res, " %s", s);
    }
    return res;
}

char *receive() {
    char *buff = malloc(BUFF_SIZE);
    buff = strcpy(buff, "");
    int size = 0;
    int stars_counter = 0;
    char word_buffer[40];
    strcpy(word_buffer, "");
    int word_length = 0;

    ssize_t sz = recv(sock, buff, 5, 0);
    buff[sz] = '\0';
    size += (int) sz;

    while (while_continue) {
        char c[2];
        sz = recv(sock, c, 1, 0);
        if (sz <= 0) {
            if (while_continue)
                closeConnection(EXIT_FAILURE, "recv");
            else
                closeConnection(EXIT_SUCCESS, NULL);
        }
        if (sz == 1 && c[0] == '\0') {
            c[0] = '0';
        }
        c[1] = '\0';
        if (c[0] == '*' || c[0] == '+') {

            if (stars_counter == 2) {
                if (word_length > 0) {
                    buff = strcat(buff, convertString(word_buffer, word_length));
                }
                size++;
                break;
            } else
                stars_counter++;
        } else if (c[0] == ' ') {
            if (word_length > 0) {
                buff = strcat(buff, convertString(word_buffer, word_length));
                strcpy(word_buffer, "");
                word_length = 0;
            }
        } else {
            strcat(word_buffer, c);
            word_length++;
        }
        size++;
    }
    if (debug) {
        print("[DEBUG] recu: '%s'", buff);
    }
    return buff;
}

int splitString(char *str, char ***res) {
    char copy[strlen(str)];
    strcpy(copy, str);
    int size = 0;
    size_t max_str_size = 0;

    char *mess = strtok(copy, " ");
    while (mess != NULL) {
        size++;
        if (max_str_size < strlen(mess))
            max_str_size = strlen(mess);
        mess = strtok(NULL, " ");
    }

    char **tab_tmp = malloc(size * max_str_size);
    int i = 0;
    strcpy(copy, str);
    mess = strtok(copy, " ");
    while (mess != NULL) {
        tab_tmp[i] = malloc(strlen(mess));
        tab_tmp[i] = strcpy(tab_tmp[i], mess);
        i++;
        mess = strtok(NULL, " ");
    }
    free(*res);
    *res = tab_tmp;
    return size;
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
        } else if (ch == 127) { // shift ( supprimer lettre
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
    splitString(str, &tab);

    int n = atoi(tab[1]);

    print("%d parties trouvees", n);
    for (int i = 0; i < n; ++i) {
        str = receive(); // [OGAME␣m␣s***]
        splitString(str, &tab);
        int m = atoi(tab[1]);
        int s = atoi(tab[2]);
        print("\tpartie numero %d, avec %d joueurs", m, s);
    }
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
    char tampon[250];
    char **multicast_tab;
    while (while_continue) {
        ssize_t rec = recv(multicast_sock, tampon, 100, 0);
        if (rec < 3) continue;
        if (rec < 0) break;
        tampon[rec - 3] = '\0';
        splitString(tampon, &multicast_tab);
        if (debug)
            print("[DEBUG MULTICAST] recu: '%s'", tampon);
        if (strcmp(multicast_tab[0], "GHOST") == 0) {
            print("Une fantome s'est deplace a (%s, %s)", multicast_tab[1], multicast_tab[2]);
        } else if (strcmp(multicast_tab[0], "ENDGA") == 0) {
            print("Partie terminee, %s a gagne avec %s points", multicast_tab[1], multicast_tab[2]);
            print("Appuyez sur n'importe quelle touche pour quitter");
            while_continue = 0;
            break;
        } else if (strcmp(multicast_tab[0], "MESSA") == 0) {
            char mess[200];
            strncpy(mess, tampon + 15, rec - 18);
            print("[GLOBAL] %s: %s", multicast_tab[1], mess);
        } else if (strcmp(multicast_tab[0], "SCORE") == 0) {
            print("Le joueur %s a attrape un fantome a (%s, %s) son score est %s",
                  multicast_tab[1], multicast_tab[3], multicast_tab[4], multicast_tab[2]);
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
    char **udp_tab;
    while (while_continue) {
        ssize_t rec = recv(udp_sock, tampon, 299, 0);
        if (rec < 3) continue;
        if (rec < 0) break;
        tampon[rec - 3] = '\0';
        splitString(tampon, &udp_tab);
        if (debug)
            print("[DEBUG UDP] recu: '%s'", tampon);
        if (strcmp(udp_tab[0], "MESSP") == 0) {
            char mess[200];
            strncpy(mess, tampon + 15, rec - 18);
            print("[PRIVE] %s: %s", udp_tab[1], mess);
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
        if (strcmp(argv[i], "--debug") == 0) {
            debug = 1;
        } else if (strncmp(argv[i], "--address=", 10) == 0) {
            strcpy(address, argv[i] + 10);
            if (strcmp(address, "localhost") != 0)
                localhost = 0;
        }
    }
    printf("%s\n", address);

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

//     to close sock when detecting CTRL+C or CTRL+Z
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

    getGamesList(receive());  // [GAMES␣n***]

    char buff[BUFF_SIZE];
    char mess[MESS_SIZE];
    int in_party = 0;
    int in_game = 0; // est 1 que si in_party==1
    char *tmp;

    while (while_continue) {
        strcpy(buff, "");
        strcpy(mess, "");

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

            sprintf(mess, "NEWPL %s %d***", id, port2);
            if (debug)
                print("[DEBUG] envoi: '%s'", mess);

            send(sock, mess, strlen(mess), 0);
            splitString(receive(), &tab);
            if (strcmp(tab[0], "REGOK") == 0) { // [REGOK␣m***]
                int m = atoi(tab[1]);
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
            sprintf(mess, "REGIS %s %d %s***", id, port2, buff);
            if (debug)
                print("[DEBUG] envoi: 'REGIS %s %d %s***'", id, port2, buff);
            sprintf(mess, "REGIS %s %d ", id, port2);
            send(sock, mess, strlen(mess), 0);
            send(sock, &m, sizeof(uint8_t), 0);
            send(sock, "***", 3, 0);
            splitString(receive(), &tab);
            if (strcmp(tab[0], "REGOK") == 0) { //  [REGOK␣m***]
                m = atoi(tab[1]);
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
            strcpy(mess, "START***");
            if (debug)
                print("[DEBUG] envoi: '%s'", mess);
            send(sock, mess, strlen(mess), 0);
            print("Vous etes pret, attente des autres joueurs");
            splitString(receive(), &tab);

            if (strcmp(tab[0], "WELCO") == 0) { // [WELCO␣m␣h␣w␣f␣ip␣port***]
                uint8_t m = atoi(tab[1]);
                uint16_t h = atoi(tab[2]);
                uint16_t w = atoi(tab[3]);
                uint8_t f = atoi(tab[4]);
                char ip[strlen(tab[5])];
                strcpy(ip, tab[5]);
                for (int i = (int) strlen(tab[5]) - 1; i >= 0; i--) { // enleve les '#' de la fin de l'ip
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
                strcpy(args[1], tab[6]);
                pthread_create(&multicast_thread, NULL, multicastThread, (void *) &args);
                splitString(receive(), &tab);
                if (strcmp(tab[0], "POSIT") == 0) {
                    print("Ta position sur le plateau est (%s, %s)", tab[2], tab[3]);
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
            strcpy(mess, "UNREG***");
            if (debug)
                print("[DEBUG] envoi: '%s'", mess);
            send(sock, mess, strlen(mess), 0);
            splitString(receive(), &tab);
            if (strcmp(tab[0], "UNROK") == 0) { // [UNROK␣m***]
                uint8_t m = atoi(tab[1]);
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
            send(sock, "SIZE? ", 6, 0);
            send(sock, &num_partie, sizeof(uint8_t), 0);
            send(sock, "***", 3, 0);

            splitString(receive(), &tab);
            if (strcmp(tab[0], "SIZE!") == 0) { // [SIZE!␣m␣h␣w***]
                uint8_t m = atoi(tab[1]);
                uint16_t h = atoi(tab[2]);
                uint16_t w = atoi(tab[3]);
                print("La partie %d est de taille %dx%d", m, h, w);
            } else {
                print("[ERROR] DUNNO");
            }
        } else if (strcmp(buff, "LIST?") == 0) { // [LIST? m***]
            print("Entrez le numero de la partie");
            readInput(buff);
            uint8_t m = atoi(buff);

            sprintf(mess, "LIST? %s***", buff);
            if (debug)
                print("[DEBUG] envoi: '%LIST? %d***'", m);
            send(sock, "LIST? ", 6, 0);
            send(sock, &m, sizeof(uint8_t), 0);
            send(sock, "***", 3, 0);

            splitString(receive(), &tab);
            if (strcmp(tab[0], "LIST!") == 0) { // [LIST!␣m␣s***]

                m = atoi(tab[1]);
                uint8_t s = atoi(tab[2]);
                print("Liste des joueurs dans la partie %d:", m);
                for (int i = 0; i < s; ++i) {
                    char *tmpe = receive();
                    splitString(tmpe, &tab); // [PLAYR␣id***]
                    print("\t- %s", tab[1]);
                }
            } else {
                print("[ERROR] DUNNO");
            }
        } else if (strcmp(buff, "GAME?") == 0) { // [GAME? m***]
            strcpy(mess, "GAME?***");
            if (debug)
                print("[DEBUG] envoi: '%s'", mess);
            send(sock, mess, strlen(mess), 0);
            tmp = receive();
            if (strcmp(tmp, "DUNNO***") != 0) {
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
            strcat(mess, buff);
            strcat(mess, " ");
            char pas[BUFF_SIZE];
            print("Entrez le nombre de pas");
            readInput(pas);
            for (int i = 0; i < 3 - ((int) strlen(pas)); i++) {
                strcat(mess, "0");
            }
            strcat(mess, pas);
            strcat(mess, "***");
            if (debug)
                print("[DEBUG] envoi: '%s'", mess);
            send(sock, mess, strlen(mess), 0);

            splitString(receive(), &tab);
            if (strcmp(tab[0], "MOVE!") == 0) { // [MOVE!␣x␣y***]
                print("Vous etes maintenant a (%s, %s)", tab[1], tab[2]);
            } else if (strcmp(tab[0], "MOVEF") == 0) { // [MOVEF␣x␣y␣p***]
                print("Vous etes maintenant a (%s, %s)", tab[1], tab[2]);
                print("Vous avez attrape un fantome! Votre score est %s", tab[3]);
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
            strcpy(mess, "IQUIT***");
            if (debug)
                print("[DEBUG] envoi: '%s'", mess);
            send(sock, mess, strlen(mess), 0);
            print("%s", receive()); // [GOBYE***]
            closeConnection(EXIT_SUCCESS, NULL);
        } else if (strcmp(buff, "GLIS?") == 0) { // [GLIS?***]
            if (!in_party) {
                print("[ERROR] Vous n'etes pas dans une partie");
                continue;
            }
            strcpy(mess, "GLIS?***");
            if (debug)
                print("[DEBUG] envoi: '%s'", mess);
            send(sock, mess, strlen(mess), 0);
            splitString(receive(), &tab);
            if (strcmp(tab[0], "DUNNO") == 0) {
                print("[ERROR] DUNNO");
            } else { // [GLIS!␣s***]
                uint8_t s = atoi(tab[1]);
                print("Les joueurs present dans la partie:");
                for (int i = 0; i < (int) s; ++i) {
                    char *player = receive();
                    print("\t- %s", player);
                }
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
            sprintf(mess, "MALL? %s***", buff);
            if (debug)
                print("[DEBUG] envoi: '%s'", mess);
            send(sock, mess, strlen(mess), 0);
            splitString(receive(), &tab);
            if (strcmp(tab[0], "MALL!") == 0) { // [MALL!***]
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
            sprintf(mess, "SEND? %s %s***", id, message);
            if (debug)
                print("[DEBUG] envoi: '%s'", mess);
            send(sock, mess, strlen(mess), 0);
            splitString(receive(), &tab);
            if (strcmp(tab[0], "SEND!") == 0) { //  [SEND!***]
                print("Message envoye");
            } else if (strcmp(tab[0], "NSEND") == 0) { // [NSEND***]
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
            strcpy(mess, "DISC!***");
            if (debug)
                print("[DEBUG] envoi: '%s'", mess);
            send(sock, mess, strlen(mess), 0);
            print("%s", receive()); // [GOBYE***]
            closeConnection(EXIT_SUCCESS, NULL);
            closeConnection(EXIT_SUCCESS, NULL);
        } else {
            int testMov = 1;
            if (strcmp(buff, "z") == 0) {
                strcat(mess, "UPMOV");
            } else if (strcmp(buff, "q") == 0) {
                strcat(mess, "LEMOV");
            } else if (strcmp(buff, "s") == 0) {
                strcat(mess, "DOMOV");
            } else if (strcmp(buff, "d") == 0) {
                strcat(mess, "RIMOV");
            } else {
                testMov = 0;
                print("[ERROR] Reessayez");
            }
            if (testMov) {
                strcat(mess, " 001***");
                if (debug)
                    print("[DEBUG] envoi: '%s'", mess);
                send(sock, mess, strlen(mess), 0);

                splitString(receive(), &tab);
                if (strcmp(tab[0], "MOVE!") == 0) { // [MOVE!␣x␣y***]
                    print("Vous etes maintenant a (%s, %s)", tab[1], tab[2]);
                } else if (strcmp(tab[0], "MOVEF") == 0) { // [MOVEF␣x␣y␣p***]
                    print("Vous etes maintenant a (%s, %s)", tab[1], tab[2]);
                    print("Vous avez attrape un fantome! Votre score est %s", tab[3]);
                } else {
                    print("[ERROR] DUNNO");
                }
            }
        }
    }
    closeConnection(EXIT_SUCCESS, NULL);
}

