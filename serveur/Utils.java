package serveur;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.Random;

public class Utils {

	public static String lecture2( BufferedReader br ) throws IOException {
		char[] buffer = new char[128];
		int charsIn;
		charsIn = br.read( buffer, 0, 128 );
		if( charsIn == -1 )
			return "-1";
		StringBuilder data = new StringBuilder( charsIn );
		data.append( buffer, 0, charsIn );
		return composition( data.toString() );
	}

	public static String composition( String str ) {
		String retour = "";
		for( int i = 0; i < str.length(); i++ ) {
			retour += Character.toString( str.charAt( i ) );
			if( retour.endsWith( "***" ) || retour.endsWith( "+++" ) ) {
				return retour;
			}
		}
		return "";
	}

	public static String[] splitString( String str ) {
		if( str.endsWith( "***" ) )
			return str.substring( 0, str.length() - 3 ).split( " " );
		return str.split( " " );
	}

}
