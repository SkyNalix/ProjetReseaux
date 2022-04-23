comp:
	javac serveur/*.java

run:
	java serveur/Serveur

clean:
	rm -f serveur/*.class
	rm -f serveur/labyrinthe/*.class
	rm -f serveur/gui/*.class