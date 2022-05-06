comp:
	javac serveur/*.java
	gcc -g -Wall -Wextra -O1 -pthread Client/client.c -o client -lncurses

clean:
	rm -f Client/client
	rm -f serveur/*.class
	rm -f serveur/labyrinthe/*.class
	rm -f serveur/gui/*.class