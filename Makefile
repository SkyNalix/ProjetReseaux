comp:
	javac serveur/*.java
	gcc -g -Wall -Wextra -O1 -pthread Client/client.c -o client -lncurses

clean:
	rm -f client serveur/*.class serveur/labyrinthe/*.class serveur/gui/*.class