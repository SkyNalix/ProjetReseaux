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


#define BUFF_SIZE 200
#define MESS_SIZE 1000
int sock;
in_port_t port;
char **tab;
char *tmp;

// variables pour l'affichage avec Ncurses
char stdin_buff[1024];
short input_x = 0;
short print_y = 0;
short max_y = 0;
pthread_mutex_t verrou = PTHREAD_MUTEX_INITIALIZER;

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
    close(sock);
    exit(exitCode);
}

char *receive() {
    char *buff = malloc(BUFF_SIZE);
    buff = strcpy(buff, "");
    int size = 0;
    int stars_counter = 0;
    char c[2];
    while (1) {
        ssize_t tmp = recv(sock, c, 1, 0);
        if (tmp <= 0)
            closeConnection(EXIT_FAILURE, "recv");
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
    print("recu: '%s'", buff);
    return buff;
}

int splitString(char *str, char ***res) {
    char copy[strlen(str)];
    strcpy(copy, str);
    int size = 0;

    char *mess = strtok(copy, " ");
    while (mess != NULL) {
        size++;
        mess = strtok(NULL, " ");
    }

    char **tab_tmp = malloc(size);
    int i = 0;
    strcpy(copy, str);
    mess = strtok(copy, " ");
    while (mess != NULL) {
        tab_tmp[i] = malloc(sizeof(mess));
        strcpy(tab_tmp[i], mess);
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
        pthread_mutex_lock(&verrou);
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
            pthread_mutex_unlock(&verrou);
            return size;
        } else if (ch == 127) { // shift ( supprimer lettre
            stdin_buff[input_x] = '\0';
            input_x -= 1;
        } else {
            const char *key = keyname(ch);
            if (strlen(key) == 1 && key[0] != '^' && key[0] != '[') {
                strcat(stdin_buff, keyname(ch));
                input_x += 1;
            }
        }
        printw("%s", stdin_buff);
        pthread_mutex_unlock(&verrou);
        refresh();
    }
//    ssize_t size = read(STDIN_FILENO, stockIci, BUFF_SIZE - 1);
//    stockIci[size - 1] = '\0'; // enleve aussi le '\n'
}

void getGamesList(char *str) {
    splitString(str, &tab);
    uint8_t n = strtoul(tab[1], NULL, 16);

    print("found %d games", n);
    for (int i = 0; i < n; ++i) {
        str = receive(); // [OGAME␣m␣s***]
        splitString(str, &tab);
        uint8_t m = strtoul(tab[1], NULL, 16);
        uint8_t s = strtoul(tab[2], NULL, 16);
        print("OGAME %d %d", m, s);
    }
}

void *multicastThread(void *arg) {
    char (*args)[20] = arg;
    print("[MULTICAST] %s, %s", args[0], args[1]);
    int multicast_sock = socket(PF_INET, SOCK_DGRAM, 0);
    int ok = 1;
    int r = setsockopt(multicast_sock, SOL_SOCKET, SO_REUSEPORT, &ok, sizeof(ok));
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
    char tampon[100];
    while (1) {
        ssize_t rec = recv(multicast_sock, tampon, 100, 0);
        if (rec < 0)
            pthread_exit((void *) EXIT_FAILURE);
        tampon[rec] = '\0';
        print("[MULTICAST] %s", tampon);
    }
    pthread_exit(EXIT_SUCCESS);
}

void *udpThread(void *arg) { //serveur udp pour recevoir message
    in_port_t udp_port = *((int *) arg);
    int udp_sock = socket(PF_INET, SOCK_DGRAM, 0);
    struct sockaddr_in address_sock;
    address_sock.sin_family = AF_INET;
    address_sock.sin_port = htons(udp_port);
    address_sock.sin_addr.s_addr = htonl(INADDR_ANY);
    int r = bind(udp_sock, (struct sockaddr *) &address_sock, sizeof(struct sockaddr_in));
    if (r == 0) {
        char tampon[500];
        while (1) {
            ssize_t rec = recv(udp_sock, tampon, 500, 0);
            if (rec < 0) break;
            tampon[rec] = '\0';
            print("[UDP %d] %s", udp_port, tampon);
        }
    } else {
        print("[ERROR] You will not receive private messages");
    }
    return 0;
}

// les commandes a utiliser avant le commencement d'une partie
uint8_t prePartieStart() {
    uint8_t id_partie = -1; // id de la partie rejoint
    char buff[BUFF_SIZE];
    char mess[MESS_SIZE];

    while (1) {
        print("Entrez le debut de la requete que vous voulez ecrire");
        readInput(buff);
        if (strcmp(buff, "NEWPL") == 0) { // [NEWPL␣id␣port***]
            char id[BUFF_SIZE];
            print("Entrez votre id");
            readInput(id);

            print("Entrez votre port");
            char tmpPort[BUFF_SIZE];
            readInput(tmpPort);
            in_port_t port2 = (in_port_t) atoi(tmpPort);

            print("NEWPL %s %d", id, port2);
            sprintf(mess, "NEWPL %s %d***", id, port2);

            send(sock, mess, strlen(mess), 0);
            splitString(receive(), &tab);
            if (strcmp(tab[0], "REGOK") == 0) { // [REGOK␣m***]
                uint8_t m = strtoul(tab[1], NULL, 16);
                print("Partie %d creee", m);
                id_partie = m;

                pthread_t pth;
                pthread_create(&pth, NULL, udpThread, (void *) &port2);
            } else {
                print("[ERROR] Partie non creee");
            }
        } else if (strcmp(buff, "REGIS") == 0) { // [REGIS␣id␣port␣m***]
            char id[BUFF_SIZE];
            print("Entrez votre id");
            readInput(id);
            print("Entrez votre port");
            char tmpPort[BUFF_SIZE];
            readInput(tmpPort);
            in_port_t port2 = (in_port_t) atoi(tmpPort);
            print("Entrez le numero de la partie");
            readInput(buff);

            print("REGIS %s %d %s", id, port2, buff);
            sprintf(mess, "REGIS %s %d %s***", id, port2, buff);
            send(sock, mess, strlen(mess), 0);
            splitString(receive(), &tab);
            if (strcmp(tab[0], "REGOK") == 0) { //  [REGOK␣m***]
                uint8_t m = strtoul(tab[1], NULL, 16);
                print("Partie %d rejoint", m);
                id_partie = m;
                pthread_t pth;
                pthread_create(&pth, NULL, udpThread, (void *) &port2);
            } else {
                print("[ERROR] La partie %s n'a pas ete rejoint", buff);
            }
        } else if (strcmp(buff, "START") == 0) { // [START***]
            strcpy(mess, "START***");
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
                    print("WELCO %d %d %d %d %s %s", m, h, w, f, ip, tab[6]);
                    pthread_t pth;
                    char args[2][20];
                    strcpy(args[0], ip);
                    strcpy(args[1], tab[6]);
                    pthread_create(&pth, NULL, multicastThread, (void *) &args);
                    splitString(receive(), &tab);
                    print("POSIT %s %s %s", tab[1], tab[2], tab[3]);
                    return id_partie;
                } else
                    print("DUNNO");
            }
        } else if (strcmp(buff, "UNREG") == 0) { // [UNREG***]
            strcpy(mess, "UNREG***");
            send(sock, mess, strlen(mess), 0);
            splitString(receive(), &tab);
            if (strcmp(tab[0], "UNROK") == 0) { // [UNROK␣m***]
                uint8_t m = strtoul(tab[1], NULL, 16);
                print("Partie %d quitte", m);
            } else {
                print("DUNNO");
            }
            id_partie = -1;
        } else if (strcmp(buff, "SIZE?") == 0) { // [SIZE?␣m***]
            print("Entrez le numero de la partie");
            readInput(buff);
            print("SIZE? %s", buff);
            sprintf(mess, "SIZE? %s***", buff);
            send(sock, mess, strlen(mess), 0);
            splitString(receive(), &tab);
            if (strcmp(tab[0], "SIZE!") == 0) { // [SIZE!␣m␣h␣w***]
                uint8_t m = strtoul(tab[1], NULL, 16);
                uint16_t h = littleEndian16ToHost(strtoul(tab[2], NULL, 16));
                uint16_t w = littleEndian16ToHost(strtoul(tab[3], NULL, 16));
                print("SIZE! %d %d %d", m, h, w);
            } else {
                print("DUNNO");
            }
        } else if (strcmp(buff, "LIST?") == 0) { // [LIST? m***]
            print("Entrez le numero de la partie");
            readInput(buff);
            print("LIST? %s", buff);

            sprintf(mess, "LIST? %s***", buff);
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
            send(sock, mess, strlen(mess), 0);
            tmp = receive();
            if (strcmp(tmp, "DUNNO") != 0) {
                getGamesList(tmp); // [GAMES␣n***]
            }

        } else {
            print("Reessayez");
        }
    }
}


int main(int argc, char **argv) {
    char *str_port = "4243";
    port = 4243;
    if (argc >= 2) {
        char *error;
        int newport = (int) strtol(argv[1], &error, 10);
        if (strcmp(error, "\0") == 0) {
            if (newport > 1024 && newport < 49151) {
                str_port = argv[1];
                port = newport;
            }
        }
    }


    struct _win_st *my_win = initscr();
    scrollok(my_win, TRUE);
    noecho();
    refresh();
    max_y = my_win->_maxy;

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
        closeConnection(EXIT_FAILURE, "socket");
    }

    int r = connect(sock, (struct sockaddr *) info->ai_addr, sizeof(struct sockaddr_in));
    if (r != 0) {
        closeConnection(EXIT_FAILURE, "connect");
    }

    getGamesList(receive());  // [GAMES␣n***]

    char buff[BUFF_SIZE];
    char mess[MESS_SIZE];

    prePartieStart();

    print("PARTIE COMMENCEE");

    while (1) {
        strcpy(buff, "");
        strcpy(mess, "");

        // a partir de là on est dans une partie
        print("Entrez le debut de la requete que vous voulez ecrire");
        readInput(buff);
        if (strcmp(buff, "UPMOV") == 0 || // [UPMOV␣d***]
            strcmp(buff, "DOMOV") == 0 || // [DOMOV␣d***]
            strcmp(buff, "LEMOV") == 0 || // [LEMOV␣d***]
            strcmp(buff, "RIMOV") == 0    // [RIMOV␣d***]
                ) {
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
            strcpy(mess, "IQUIT***");
            send(sock, mess, strlen(mess), 0);
            print("%s", receive()); // [GOBYE***]
            closeConnection(EXIT_SUCCESS, NULL);
        } else if (strcmp(buff, "GLIS?") == 0) { // [GLIS?***]
            strcpy(mess, "GLIS?***");
            send(sock, mess, strlen(mess), 0);
            splitString(receive(), &tab);
            if (strcmp(tab[0], "DUNNO") == 0) {
                print("[ERROR] DUNNO");
            } else { // [GLIS!␣s***]
                uint8_t s = strtoul(tab[1], NULL, 16);
                for (int i = 0; i < (int) s; ++i) {
                    char *player = receive();
                    print("%s", player);
                }
            }
        } else if (strcmp(buff, "MALL?") == 0) { // [MALL?␣mess***]
            print("Entrez le message a envoyer a tous les joueurs");
            readInput(buff);
            sprintf(mess, "MALL? %s***", buff);
            send(sock, mess, strlen(mess), 0);

            tmp = receive();
            if (strcmp(tmp, "MALL!") == 0) { // [MALL!***]
                print("Message envoye");
            } else {
                print("[ERROR] Message non envoye");
            }
        } else if (strcmp(buff, "SEND?") == 0) { // [SEND?␣id␣mess***]
            char id[10];
            char message[200];
            print("Entrez le id du destinataire");
            readInput(id);
            print("Entrez le message");
            readInput(message);
            sprintf(mess, "SEND? %s %s***", id, message);
            print("%s ", mess);
            send(sock, mess, strlen(mess), 0);

            tmp = receive();
            if (strcmp(tmp, "SEND!") == 0) { //  [SEND!***]
                print("Message envoye");
            } else { // [NSEND***]
                print("[ERROR] Message non envoye");
            }
        } else {
            print("[ERROR] Reessayez");
        }
    }

    close(sock);
    exit(EXIT_SUCCESS);
}

