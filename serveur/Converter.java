package serveur;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

public class Converter {

	private static int calcSize( Object[] l ) {
		int res = 0; for( int i = 0; i < l.length; i++ ) {
			Object obj = l[i];
			if( obj instanceof String ) {
				res += ( (String) obj ).length();
			} else if( obj instanceof Nombre ) {
				Nombre nombre = (Nombre) obj;
				res += nombre.nbr_byte;
			}
			if( i < l.length - 2 ) { // pour l'espace
				res++;
			}
		} return res;
	}

	public static byte[] convert( Object... l ) {
		ByteBuffer bb = ByteBuffer.allocate( calcSize( l ) );
		bb.order( ByteOrder.BIG_ENDIAN );
		for( int i = 0; i < l.length; i++ ) {
			Object obj = l[i];
			if( obj instanceof String ) {
				String s = (String) obj;
				bb.put( s.getBytes() );
			} else if( obj instanceof Nombre ) {
				Nombre nombre = (Nombre) obj;
				int n = Math.max( 0, nombre.n );
				if( nombre.nbr_byte == 1 ) {
					bb.put( (byte) Math.min( 255, n ) );
				} else if( nombre.nbr_byte == 2 ) {
					bb.put( intToTwoBytes( Math.min( 1000, n ) ) );
				}
			}
			if( i < l.length - 2 )
				bb.put( (byte) ' ' );
		}
		return bb.array();
	}


	static byte[] intToTwoBytes( int i ) {
		return new byte[]{
				  (byte) ( ( i >>> 8 ) & 0xFF ),
				  (byte) ( i & 0xFF )
		};
	}


}
