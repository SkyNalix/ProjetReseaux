package serveur;

import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.IOException;
import java.util.Random;

public class Utils {

	public static String[] splitString( String str ) {
		if( str.endsWith( "***" ) )
			return str.substring( 0, str.length() - 3 ).split( " " );
		return str.split( " " );
	}

}
