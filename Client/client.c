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
#define MESS_SIZE 1000
int debug = 0;
int sock;
char **tab;
char *tmp;
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

char *receive() {
    char *buff = malloc(BUFF_SIZE);
    buff = strcpy(buff, "");
    int size = 0;
    int stars_counter = 0;
    char c[2];
    while (while_continue) {
        ssize_t tmp = recv(sock, c, 1, 0);
        if (tmp <= 0) {
            if (while_continue)
                closeConnection(EXIT_FAILURE, "recv");
            else
                closeConnection(EXIT_SUCCESS, NULL);
        }
        c[1] = '\0';
        if (strcmp(c, "*") == 0) {
            if (stars_counter == 2) {
                break;
            } else
                stars_counter++;
        } else
            strcat(buff, c);
        size++;
    }
    buff[size] = '\0';
    if (debug)
        print("[DEBUG] recu: '%s'", buff);
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
    uint8_t n = strtoul(tab[1], NULL, 16);

    print("%d parties trouvees", n);
    for (int i = 0; i < n; ++i) {
        str = receive(); // [OGAME␣m␣s***]
        splitString(str, &tab);
        uint8_t m = strtoul(tab[1], NULL, 16);
        uint8_t s = strtoul(tab[2], NULL, 16);
        print("\tpartie numero %d, avec %d joueurs", m, s);
    }
}

void *multicastThread(void *arg) {
    char (*args)[20] = arg;
    if (debug)
        print("[MULTICAST] ip = %s, port = %s", args[0], args[1]);
    int multicast_sock = socket(PF_INET, SOCK_DGRAM, 0);
    int ok = 1;
    int r = setsockopt(multicast_sock, SOL_SOCKET, SO_REUSEPORT, &ok, sizeof(ok));
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
    while (while_continue) {
        ssize_t rec = recv(multicast_sock, tampon, 100, 0);
        if (rec == 0) continue;
        if (rec < 0) break;
        tampon[rec] = '\0';
        print("[GLOBAL] %s", tampon);
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
        if (rec == 0) continue;
        if (rec < 0) break;
        tampon[rec] = '\0';
        print("[PRIVE] %s", tampon);
    }
    pthread_exit(EXIT_SUCCESS);
}

int main(int argc, char **argv) {
    char *str_port = "4243";
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
        if (strcmp(argv[i], "--debug") == 0)
            debug = 1;
    }

//     to close sock when detecting CTRL+C or CTRL+Z
    struct sigaction sig_action;
    sig_action.sa_handler = sig_handler;
    sig_action.sa_flags = 0;
    sigaction(SIGINT, &sig_action, NULL);
    sigaction(SIGTSTP, &sig_action, NULL);

    struct addrinfo *info;
    struct addrinfo hints;
    memset(&hints, 0, sizeof(hints));
    hints.ai_family = AF_INET;
    hints.ai_socktype = SOCK_STREAM;
    int addrinfo = getaddrinfo("localhost", str_port, &hints, &info);
    if (addrinfo != 0) {
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

    int r = connect(sock, (struct sockaddr *) info->ai_addr, sizeof(struct sockaddr_in));
    if (r != 0) {
        close(sock);
        perror("connect");
        exit(EXIT_FAILURE);
    }

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
                uint8_t m = strtoul(tab[1], NULL, 16);
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
            char id[BUFF_SIZE];
            print("Entrez votre id");
            readInput(id);
            print("Entrez votre port");
            char tmpPort[BUFF_SIZE];
            readInput(tmpPort);
            in_port_t port2 = (in_port_t) atoi(tmpPort);
            print("Entrez le numero de la partie");
            readInput(buff);

            sprintf(mess, "REGIS %s %d %s***", id, port2, buff);
            if (debug)
                print("[DEBUG] envoi: '%s'", mess);
            send(sock, mess, strlen(mess), 0);
            splitString(receive(), &tab);
            if (strcmp(tab[0], "REGOK") == 0) { //  [REGOK␣m***]
                uint8_t m = strtoul(tab[1], NULL, 16);
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
            tmp = receive();
            if (strcmp(tmp, "DUNNO") == 0) {
                print("DUNNO");
            } else {
                splitString(tmp, &tab);

                if (strcmp(tab[0], "WELCO") == 0) { // [WELCO␣m␣h␣w␣f␣ip␣port***]
                    uint8_t m = strtoul(tab[1], NULL, 16);
                    uint16_t h = littleEndian16ToHost(strtoul(tab[2], NULL, 16));
                    uint16_t w = littleEndian16ToHost(strtoul(tab[3], NULL, 16));
                    uint8_t f = strtoul(tab[4], NULL, 16);
                    char ip[strlen(tab[5])];
                    strcpy(ip, tab[5]);
                    for (int i = (int) strlen(tab[5]) - 1; i >= 0; i--) { // enleve les '#' de la fin de l'ip
                        if (ip[i] == '#') {
                            ip[i] = '\0';
                        } else
                            break;
                    }
                    print("La partie %d a commencee, le jeu est de taille %dx%d, et il y a %d fantomes", m, h, w, f);
                    in_game = 1;
                    char args[2][20];
                    strcpy(args[0], ip);
                    strcpy(args[1], tab[6]);
                    pthread_create(&multicast_thread, NULL, multicastThread, (void *) &args);
                    splitString(receive(), &tab);
                    print("Ta position sur le plateau est (%s, %s)", tab[2], tab[3]);
                } else
                    print("DUNNO");
            }
        } else if (strcmp(buff, "UNREG") == 0) { // [UNREG***]
            if (!in_party) {
                print("[ERROR] Vous n'etes pas dans une partie");
                continue;
            }
            if (in_game) {
                print("[ERROR] La partie a deja commencee, essaye plutot IQUIT");
                continue;
            }
            strcpy(mess, "UNREG***");
            if (debug)
                print("[DEBUG] envoi: '%s'", mess);
            send(sock, mess, strlen(mess), 0);
            splitString(receive(), &tab);
            if (strcmp(tab[0], "UNROK") == 0) { // [UNROK␣m***]
                uint8_t m = strtoul(tab[1], NULL, 16);
                print("Partie %d quitte", m);
            } else {
                print("DUNNO");
            }
        } else if (strcmp(buff, "SIZE?") == 0) { // [SIZE?␣m***]
            print("Entrez le numero de la partie");
            readInput(buff);
            print("SIZE? %s", buff);
            sprintf(mess, "SIZE? %s***", buff);
            if (debug)
                print("[DEBUG] envoi: '%s'", mess);
            send(sock, mess, strlen(mess), 0);
            splitString(receive(), &tab);
            if (strcmp(tab[0], "SIZE!") == 0) { // [SIZE!␣m␣h␣w***]
                uint8_t m = strtoul(tab[1], NULL, 16);
                uint16_t h = littleEndian16ToHost(strtoul(tab[2], NULL, 16));
                uint16_t w = littleEndian16ToHost(strtoul(tab[3], NULL, 16));
                print("La partie %d est de taille %dx%d", m, h, w);
            } else {
                print("DUNNO");
            }
        } else if (strcmp(buff, "LIST?") == 0) { // [LIST? m***]
            print("Entrez le numero de la partie");
            readInput(buff);
            print("LIST? %s", buff);

            sprintf(mess, "LIST? %s***", buff);
            if (debug)
                print("[DEBUG] envoi: '%s'", mess);
            send(sock, mess, strlen(mess), 0);
            splitString(receive(), &tab);
            if (strcmp(tab[0], "LIST!") == 0) { // [LIST!␣m␣s***]
                uint8_t m = strtoul(tab[1], NULL, 16);
                uint8_t s = strtoul(tab[2], NULL, 16);
                print("SIZE! %d %d", m, s);
                for (int i = 0; i < s; ++i) {
                    splitString(receive(), &tab); // [PLAYR␣id***]
                    print("PLAYR %s", tab[1]);
                }
            } else {
                print("DUNNO");
            }
        } else if (strcmp(buff, "GAME?") == 0) { // [GAME? m***]
            strcpy(mess, "GAME?***");
            if (debug)
                print("[DEBUG] envoi: '%s'", mess);
            send(sock, mess, strlen(mess), 0);
            tmp = receive();
            if (strcmp(tmp, "DUNNO") != 0) {
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
            char d[BUFF_SIZE];
            print("Entrez le nombre de pas");
            readInput(d);
            for (int i = 0; i < 3 - ((int) strlen(d)); i++) {
                strcat(mess, "0");
            }
            strcat(mess, d);
            strcat(mess, "***");
            if (debug)
                print("[DEBUG] envoi: '%s'", mess);
            send(sock, mess, strlen(mess), 0);

            splitString(receive(), &tab);
            if (strcmp(tab[0], "MOVE!") == 0) { // [MOVE!␣x␣y***]
                print("MOVE! %s %s", tab[1], tab[2]);
            } else if (strcmp(tab[0], "MOVEF") == 0) { // [MOVEF␣x␣y␣p***]
                print("MOVEF %s %s %s", tab[1], tab[2], tab[3]);
            } else {
                print("[ERROR] DUNNO");
            }
        } else if (strcmp(buff, "IQUIT") == 0) { // [IQUIT***]
            if (!in_party) {
                print("[ERROR] Vous n'etes pas dans une partie");
                continue;
            }
            if (!in_game) {
                print("[ERROR] La partie n'a pas encore commencee, essaye plutot UNREG");
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
                uint8_t s = strtoul(tab[1], NULL, 16);
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
            tmp = receive();
            if (strcmp(tmp, "MALL!") == 0) { // [MALL!***]
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
            print("%s ", mess);
            if (debug)
                print("[DEBUG] envoi: '%s'", mess);
            send(sock, mess, strlen(mess), 0);
            tmp = receive();
            if (strcmp(tmp, "SEND!") == 0) { //  [SEND!***]
                print("Message envoye");
            } else { // [NSEND***]
                print("[ERROR] Message non envoye");
            }
        } else {
            int testMov=1;
            if(strcmp(buff,"z")==0){
                strcat(mess,"UPMOV");
            }else if(strcmp(buff,"q")==0){
                strcat(mess,"LEMOV");
            }else if(strcmp(buff,"s")==0){
                strcat(mess,"DOMOV");
            }else if(strcmp(buff,"d")==0){
                strcat(mess,"RIMOV");
            }else{
                testMov=0;
                print("[ERROR] Reessayez");
            }
            if(testMov){
                strcat(mess, " ");
                strcat(mess, "001");
                strcat(mess, "***");
                send(sock, mess, strlen(mess), 0);
            }
            
        }
    }

    closeConnection(EXIT_SUCCESS, NULL);
}

