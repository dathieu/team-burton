package ch.epfl.sweng.partyup.dbstore;

import java.util.Random;

public class Tools {
    /**
     * @param len of the key to be generated
     * @return random valid firebase id
     */
    public static String generateKey(int len) {

        // this will be filled with random characters
        StringBuffer key = new StringBuffer();
        Random random = new Random();

        // all characters that are valid in a firebase key
        String validChars = "aAbBcCdDeEfFgGhHiIjJkKlLmMnNoOpPqQrRsStTuUvVwWxXyYzZ1234567890-_";

        // choose random characters and append them to the buffer until the desired length is reached
        for (int index = 0; index < len; index++) {
            char chosen = validChars.charAt(random.nextInt(validChars.length()));
            key.append(chosen);
        }
        return key.toString();
    }

    public static String mapId(String id){
        return id.replaceAll("\\.", "_point_");
    }

    public static String unMapId(String id){
        return id.replaceAll("_point_", ".");
    }
}
