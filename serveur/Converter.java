package serveur;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Arrays;

public class Converter {

	public static byte[] dunnoMessageByteArray() {
		ByteBuffer bbuf = ByteBuffer.allocate( 8 );
		bbuf.order( ByteOrder.BIG_ENDIAN );
		bbuf.put( "DUNNO***".getBytes() );
		return bbuf.array();
	}


	private static int[] calcSize( Object[] l ) {
		int endian = 2; // 1 = little 2 = big;
		int res = 0; for( int i = 0; i < l.length; i++ ) {
			Object obj = l[i];
			if( obj instanceof String ) {
				res += ( (String) obj ).length();
			} else if( obj instanceof Nombre ) {
				Nombre nombre = (Nombre) obj;
				if( nombre.n < 0 ) continue;
				res += nombre.nbr_byte;
			} if( i < l.length - 2 ) {
				endian = 1;
				res++;
			}
		} return new int[]{ endian, res };
	}

	public static byte[] convert( Object... l ) {
		int[] info = calcSize( l );
		ByteBuffer bb = ByteBuffer.allocate( info[1] );
//		bb.order( info[0] == 1 ? ByteOrder.LITTLE_ENDIAN : ByteOrder.BIG_ENDIAN );
		bb.order( ByteOrder.BIG_ENDIAN );
		for( int i = 0; i < l.length; i++ ) {
			Object obj = l[i];
			if( obj instanceof String ) {
				bb.put( ( (String) obj ).getBytes() );
			} else if( obj instanceof Nombre ) {
				Nombre nombre = (Nombre) obj;
				if( nombre.nbr_byte == 1 ) {
					bb.put( (byte) nombre.n );
				} else if( nombre.nbr_byte == 2 ) {
					bb.put( intToTwoBytes( nombre.n ) );
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
