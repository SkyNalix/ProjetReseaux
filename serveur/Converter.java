package serveur;

import java.nio.ByteBuffer;

public class Converter {

	public static String int8ToHexString( int n ) {
		return Integer.toHexString( Math.min( 255, n ) );
	}

	public static String int16ToHexString( int n ) {
		byte[] b = ByteBuffer.allocate( 4 ).putInt( Math.min( 65535, n ) ).array();
		String res = "";
		boolean left_zero = true;
		for( byte by : b ) {
			String s =String.format( "%x", by );
			if( !s.equals( "0" ) || !left_zero) {
				res += s;
				left_zero = false;
			}
		}
		return res;
	}

	public static byte[] hexStringToByteArray(String str) {
		String s = str;
		if( s.length() % 2 == 1 )
			s = "0" + s;
		int len = s.length();
		byte[] data = new byte[len / 2];
		for (int i = 0; i < len; i += 2) {
			data[i / 2] = (byte) ((Character.digit(s.charAt(i), 16) << 4)
								  + Character.digit(s.charAt(i+1), 16));
		}
		return data;
	}

	public static int uint8ToInt(String hex) {
		return hexStringToByteArray(hex)[0] & 0xFF;
	}

	public static int uint16ToInt(String hex) {
		byte[] b = hexStringToByteArray( hex );
		if( b.length == 1)
			return b[0] & 0xFF;
		return ((b[0] << 8) & 0x0000ff00) | (b[1] & 0x000000ff);
	}

}
