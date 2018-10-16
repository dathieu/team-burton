package ch.epfl.sweng.partyup.dbstore;

import android.support.test.runner.AndroidJUnit4;

import org.junit.Before;
import org.junit.runner.RunWith;

import ch.epfl.sweng.partyup.dbstore.statemachine.DBState;


@RunWith(AndroidJUnit4.class)
public class MockedConnectionTests extends ConnectionTests {

    @Override
    @Before
    public void init(){
        connection = MockedConnection.getInstance();
        if(connection.getState()== DBState.SignedIn)
            connection.signOut();
    }
}