import java.io.BufferedReader;
import java.nio.ByteBuffer;
import java.util.Random;

public class Utilitaire {

    public static int RandomM(){
        Random a = new Random(); 
        int m = a.nextInt(25536);
        for(int i =0; i < Serveur.listePartie.size();i++){
            if(Serveur.listePartie.get(i).getM() == m){
                return RandomM();
            }
        }
        return m;
    }

    public static String lecture3(){
        ByteBuffer x = ByteBuffer.allocate(128);
        String str = "";
        for(int i =0; i < x.capacity();i++ ){
            str += Character.toString(x.getChar(i));
        }
        System.out.println(str);
        return str;
    }    

    public static String lecture2(BufferedReader br){
        char[] buffer = new char[128];
        int charsIn;
        try {
            charsIn = br.read(buffer, 0, 128);
            StringBuilder data = new StringBuilder(charsIn);
            data.append(buffer, 0, charsIn);
            return composition(data.toString());
            
        } catch (Exception e) {
           // e.printStackTrace();
        }
        return "";
    }

    public static String composition(String str){
        String retour = "";
        for(int i =0; i < str.length();i++){
            retour += Character.toString(str.charAt(i));
            if(retour.endsWith("***") || retour.endsWith("+++")){return retour;}
        }
        return "";
    }

    public static String entrerNom(BufferedReader br){
        char[] buffer = new char[128];
        int charsIn;
        try {
            charsIn = br.read(buffer, 0, 8);
            System.out.println("CHARSIN:"+charsIn);
            StringBuilder data = new StringBuilder(charsIn);
            data.append(buffer, 0, charsIn);
            System.out.println(data.toString());
            return data.toString();
        } catch (Exception e) {
           // e.printStackTrace();
        }
        return "";
    }
    
}
