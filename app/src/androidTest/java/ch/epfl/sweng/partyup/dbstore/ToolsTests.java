package ch.epfl.sweng.partyup.dbstore;

import android.support.test.runner.AndroidJUnit4;

import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.junit.Assert.assertTrue;

@RunWith(AndroidJUnit4.class)
public class ToolsTests {

    @Test
    public void generateKeyTest(){
        String key = Tools.generateKey(10);
        assertTrue(key!=null);

        // check whether the key is a proper key as expected by Firebase
        Pattern pattern = Pattern.compile("^[a-zA-Z0-9-_]+$");
        Matcher matcher = pattern.matcher(key);
        boolean valid = matcher.matches();

        assertTrue(valid);
    }
}