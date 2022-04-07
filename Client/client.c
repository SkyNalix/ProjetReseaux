#include <sys/socket.h>
#include <netinet/in.h>
#include <stdlib.h>
#include <unistd.h>
#include <stdio.h>
#include <string.h>
#include <netdb.h>

#define SIZE_NAME 8
#define BUFF_SIZE 200
#define MESS_SIZE 200
int sock;
in_port_t port;
char buff[BUFF_SIZE];
char *mess;
char **tab;

void closeConnection(int exitCode, char *error) {
    if (error != NULL)
        perror(error);
    close(sock);
    exit(exitCode);
}

char *receive() {
    char *buff = malloc(BUFF_SIZE);
    int size = recv(sock, buff, BUFF_SIZE - 1, 0);
    if (size <= 0)
        closeConnection(EXIT_FAILURE, "recv");
    buff[size] = '\0';
    printf("recu: '%s'\n", buff);
    return buff;
}

int splitString(char *str, char ***res) {
    char str2[strlen(str) - 2]; // removing the ending '***'
    strncpy(str2, str, strlen(str) - 3);
    str2[strlen(str) - 3] = '\0';
    char copy[strlen(str2)]; // counting amount of words

    int size = 0;
    strcpy(copy, str2);

    char *mess = strtok(copy, " ");
    while (mess != NULL) {
        size++;
        mess = strtok(NULL, " ");
    }
    char **tab = malloc(size);
    int i = 0;
    mess = strtok(str2, " ");
    while (mess != NULL) {
        tab[i] = malloc(sizeof(mess));
        strcpy(tab[i], mess);
        i++;
        mess = strtok(NULL, " ");
    }
    *res = tab;
    return size;
}

int readInput(char *stockIci) {
    int size;
    while ((size = read(STDIN_FILENO, stockIci, BUFF_SIZE - 1)) <= 0) {}
    stockIci[size - 1] = '\0';
    printf("input: '%s'\n", stockIci);
    return size;
}

int entreePartie() {
    printf("Entrez 'NEWPL' ou 'REGIS'\n");
    readInput(buff);
    char id[BUFF_SIZE];
    printf("Entrez votre id\n");
    readInput(id);
    if (strcmp(buff, "NEWPL") == 0) {
        // [NEWPL␣id␣port***]
        printf("NEWPL %s %d\n", id, port);
        sprintf(mess, "NEWPL %s %x***", id, port);

        send(sock, mess, strlen(mess), 0);
        splitString(receive(), &tab);
        if (strcmp(tab[0], "REGOK") == 0) {
            //  [REGOK␣m***]
            uint8_t m = strtoul(tab[1], NULL, 16);
            printf("Partie %d créée\n", m);
        } else if (strcmp(tab[0], "REGNO") == 0) {
            // [REGNO***]
            printf("Creation de partie non terminée\n");
        }
    } else if (strcmp(buff, "REGIS") == 0) {
        // [REGIS␣id␣port␣m***]
        printf("Entrez le numéro de la partie que vous souhaitez rejoindre\n");
        readInput(buff);
        printf("REGIS %s %d %s\n", id, port, buff);
        sprintf(mess, "REGIS %s %d %s***", id, port, buff);
        send(sock, mess, strlen(mess), 0);
        splitString(receive(), &tab);
        if (strcmp(tab[0], "REGOK") == 0) {
            //  [REGOK␣m***]
            uint8_t m = strtoul(tab[1], NULL, 16);
            printf("Partie %d rejoint\n", m);
        } else if (strcmp(tab[0], "REGNO") == 0) {
            // [REGNO***]
            printf("La partie %s n'a pas été rejoint\n", buff);
        }
    }
    return 1;
}

int main(int argc, char **argv) {
    char *str_port = "4243";
    port = 4243;
    if (argc >= 2) {
        char *error;
        int newport = strtol(argv[1], &error, 10);
        if (strcmp(error, "\0") == 0) {
            if (newport > 1024 && newport < 49151) {
                str_port = argv[1];
                port = newport;
            }
        }
    }

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


    mess = receive();
    splitString(mess, &tab); // [GAMES␣n***]
    int n = strtol(tab[1], NULL, 16);
    printf("found %d games\n", n);
    for (int i = 0; i < n; ++i) {
        mess = receive(); // [OGAME␣m␣s***]
        splitString(mess, &tab);
        uint8_t m = strtoul(tab[1], NULL, 16);
        uint8_t s = strtoul(tab[2], NULL, 16);
        printf("OGAME %d %d\n", m, s);
    }

    r = entreePartie();
    if( r ) {

    }

    close(sock);
    exit(EXIT_SUCCESS);
}
