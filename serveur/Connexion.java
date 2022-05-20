package serveur;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.util.Arrays;

public class Connexion {

	public final Socket socket;
	public final DataInputStream channelReception;
	public final DataOutputStream channelEnvoi;

	public Connexion( Socket socket ) throws IOException {
		this.socket = socket;
		this.channelReception = new DataInputStream( socket.getInputStream() );
		this.channelEnvoi = new DataOutputStream( socket.getOutputStream() );
	}

	public void write( byte[] b ) throws IOException {
		channelEnvoi.write( b );
		channelEnvoi.flush();
	}

	public String convertword( String entete, byte[] word ) {
		int len = word.length;
		while( len > 0 && word[len - 1] == 0x0 ) len--;
		if( len == 0 ) {
			return "0";
		} else if( entete.equals( "MALL?" ) || entete.equals( "SEND?" ) ) {
			// le messages peut avoir des mots de longueur 1 ou 2
			// ce if est pour ne pas les tranformer en int
			return new String( word );
		} else if( len == 1 ) {
			return Byte.toUnsignedInt( word[0] ) + "";
		} else if( len == 2 ) {
			return ( ( ( word[1] & 0xff ) << 8 ) | ( word[0] & 0xff ) ) + "";
		} else {
			return new String( word );
		}
	}

	public String lire() {
		String line = "";
		ByteBuffer word = ByteBuffer.allocate( 40 );
		int sz;
		int stars_counter = 0;
		try {
			byte[] entetebytes = new byte[5];
			sz = channelReception.read( entetebytes, 0, 5 );
			if( sz != 5 )
				return null;
			String entete = new String( entetebytes );
			line += entete;
			while( true ) {
				byte[] b = new byte[1];
				sz = channelReception.read( b, 0, 1 );
				char c = new String( b ).charAt( 0 );
				if( c == '*' || c == '+' ) {
					if( stars_counter == 2 ) {
						if( word.position() > 0 ) {
							line += " " + convertword( entete, word.array() ).trim();
						}
						if( c == '*' )
							line += "***";
						else
							line += "+++";
						break;
					} else {
						stars_counter++;
					}
				} else if( c == ' ' ) {
					if( word.position() > 0 ) {
						line += " " + convertword( entete, word.array() ).trim();
						word = ByteBuffer.allocate( 40 );
					}
				} else {
					word.put( b );
				}
			}
		} catch( IOException e ) {
			return null;
		}
		return line;
	}

	public static void sendUDP( InetSocketAddress ia, byte[] b ) throws IOException {
		DatagramSocket sock = new DatagramSocket();
		DatagramPacket paquet = new DatagramPacket( b, b.length, ia );
		sock.send( paquet );
		sock.close();
	}

}
