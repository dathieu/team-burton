package ch.epfl.sweng.partyup.dbstore;

import android.support.test.runner.AndroidJUnit4;

import org.junit.Before;
import org.junit.runner.RunWith;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import ch.epfl.sweng.partyup.containers.Tuple;
import ch.epfl.sweng.partyup.dbstore.listeners.CompletionListener;
import ch.epfl.sweng.partyup.dbstore.statemachine.DBResult;
import ch.epfl.sweng.partyup.dbstore.statemachine.DBState;


@RunWith(AndroidJUnit4.class)
public class MockedPartyTests extends PartyTests{

    @Before
    public void init(){
        connection = MockedConnection.getInstance();
        if(connection.getState()== DBState.SignedIn)
            connection.signOut();

        final CountDownLatch waiter = new CountDownLatch(1);

        connection.signIn(new CompletionListener<DBResult>() {
            @Override
            public void onCompleted(DBResult result) {

                connection.createParty(new CompletionListener<Tuple<DBResult, Party>>() {
                    @Override
                    public void onCompleted(Tuple<DBResult, Party> result) {
                        party=result.object2;
                        waiter.countDown();
                    }
                });
            }
        });

        boolean finished_in_time;
        try {
            finished_in_time = waiter.await(TIMEOUT, TimeUnit.MILLISECONDS);
        } catch (InterruptedException ex) {
            throw new AssertionError("we have been interrupted");
        }
        if (!finished_in_time)
            throw new AssertionError("timeout in name setting");


    }
}