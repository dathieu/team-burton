package ch.epfl.sweng.partyup.dbstore;

import android.support.test.runner.AndroidJUnit4;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import ch.epfl.sweng.partyup.containers.Tuple;
import ch.epfl.sweng.partyup.dbstore.listeners.CompletionListener;
import ch.epfl.sweng.partyup.dbstore.listeners.SchemaListener;
import ch.epfl.sweng.partyup.dbstore.statemachine.DBResult;

import static org.junit.Assert.assertTrue;

@RunWith(AndroidJUnit4.class)
abstract public class ConnectionTests {

    protected final int TIMEOUT = 1000000;

    protected Connection connection;

    @Before
    abstract public void init();

    @Test
    public void initWorks() {
        assertTrue(connection != null);
    }

    @Test
    public void canCreateParty() throws Exception {

        final CountDownLatch waiter = new CountDownLatch(1);

        connection.signIn(new CompletionListener<DBResult>() {
            @Override
            public void onCompleted(DBResult result) {

                connection.createParty(new CompletionListener<Tuple<DBResult, Party>>() {
                    @Override
                    public void onCompleted(Tuple<DBResult, Party> result) {
                        if (result.object1 != DBResult.Success)
                            throw new AssertionError("couldn't create a party");

                        final Party party = result.object2;
                        connection.signOut();
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
            throw new AssertionError("timeout in creation of party");

    }

    @Test(expected = AssertionError.class)
    public void cantCreateBeforeSignIn() throws Exception {

        connection.createParty(new CompletionListener<Tuple<DBResult, Party>>() {
            @Override
            public void onCompleted(Tuple<DBResult, Party> result) {

            }
        });
    }


    @Test
    public void canConnectToParty() throws Exception {

        final CountDownLatch waiter = new CountDownLatch(1);

        connection.signIn(new CompletionListener<DBResult>() {
            @Override
            public void onCompleted(DBResult result) {

                connection.createParty(new CompletionListener<Tuple<DBResult, Party>>() {
                    @Override
                    public void onCompleted(Tuple<DBResult, Party> tuple) {
                        final Party party = tuple.object2;
                        final String key = party.getKey();

                        connection.connectToParty(key, new CompletionListener<Tuple<DBResult, Party>>() {
                            @Override
                            public void onCompleted(Tuple<DBResult, Party> tuple) {
                                Party result = tuple.object2;
                                String otherKey = result.getKey();
                                if (!key.equals(otherKey))
                                    throw new AssertionError("keys are not equal");

                                // this is intentional checking for reference equality
                                // there must always be only one party object for each key
                                if (party != result)
                                    throw new AssertionError("the references are not the same");

                                connection.signOut();
                                waiter.countDown();
                            }
                        });
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
            throw new AssertionError("timeout in connection to party");
    }

    @Test
    public void cantConnectToInvalidKey() throws Exception {

        final CountDownLatch waiter = new CountDownLatch(1);

        connection.signIn(new CompletionListener<DBResult>() {
            @Override
            public void onCompleted(DBResult result) {

                // spaces are not valid for a key
                String key = "a key that can never exist in the database";
                connection.connectToParty(key, new CompletionListener<Tuple<DBResult, Party>>() {
                    @Override
                    public void onCompleted(Tuple<DBResult, Party> result) {
                        if (result.object1 != DBResult.InvalidKey)
                            throw new AssertionError("you shouldn't be able to connect to this");
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
            throw new AssertionError("timeout in key checking");
    }

    @Test(expected = AssertionError.class)
    public void cantConnectBeforeSignIn() throws Exception {
        connection.connectToParty("random string, really shouldn't matter", new CompletionListener<Tuple<DBResult, Party>>() {
            @Override
            public void onCompleted(Tuple<DBResult, Party> result) {

            }
        });
    }

    @Test(expected = AssertionError.class)
    public void cantSignOutBeforeSignIn() {
        connection.signOut();
    }

    @Test(expected = AssertionError.class)
    public void cantSignInTwice() throws Exception {
        final CountDownLatch waiter = new CountDownLatch(1);

        connection.signIn(new CompletionListener<DBResult>() {
            @Override
            public void onCompleted(DBResult result) {
                waiter.countDown();
            }
        });
        boolean finished_in_time;
        try {
            finished_in_time = waiter.await(TIMEOUT, TimeUnit.MILLISECONDS);
        } catch (InterruptedException ex) {
            throw new AssertionError("we have been interrupted");
        }
        if (!finished_in_time)
            throw new AssertionError("timeout in sign in");

        // signing in for the second time -> exception
        connection.signIn(new CompletionListener<DBResult>() {
            @Override
            public void onCompleted(DBResult result) {

            }
        });
    }

    @Test
    public void canGetUserNameAfterSignIn() throws Exception {

        final CountDownLatch waiter = new CountDownLatch(1);

        connection.signIn(new CompletionListener<DBResult>() {
            @Override
            public void onCompleted(DBResult result) {
                String userName = connection.getUserName();
                if (userName == null)
                    throw new AssertionError("userName must be properly initialized");

                connection.signOut();
                waiter.countDown();
            }
        });


        boolean finished_in_time;
        try {
            finished_in_time = waiter.await(TIMEOUT, TimeUnit.MILLISECONDS);
        } catch (InterruptedException ex) {
            throw new AssertionError("we have been interrupted");
        }
        if (!finished_in_time)
            throw new AssertionError("timeout in sign in");

    }

    @Test(expected = AssertionError.class)
    public void cantGetUserNameBeforeSignIn() throws Exception {
        connection.getUserName();
    }

/*
    @Test
    public void canFindPreviousParties() {
        final CountDownLatch waiter = new CountDownLatch(1);

        final AtomicBoolean success = new AtomicBoolean(false);

        // generate a party
        connection.signIn(new CompletionListener<DBResult>() {
            @Override
            public void onCompleted(DBResult result) {

                connection.createParty(new CompletionListener<Tuple<DBResult, Party>>() {
                    @Override
                    public void onCompleted(Tuple<DBResult, Party> result) {
                        if (result.object1 != DBResult.Success)
                            throw new AssertionError("couldn't create a party");

                        final Party party = result.object2;


                        // generate a fake spotify key
                        final String spotifyId = Tools.generateKey(10);

                        connection.registerSpotifyId(spotifyId, new CompletionListener<DBResult>() {
                            @Override
                            public void onCompleted(DBResult result) {

                                // now it should be possible to look up this party
                                connection.lookupParties(spotifyId, new SchemaListener<Party>() {
                                    @Override
                                    public void onItemAdded(Party item) {
                                        if (item.getKey() .equals( party.getKey())) {
                                            success.set(true);
                                        } else {
                                            success.set(false);
                                        }
                                        waiter.countDown();
                                    }

                                    @Override
                                    public void onItemChanged(Party item) {

                                    }

                                    @Override
                                    public void onItemDeleted(Party item) {

                                    }
                                });
                            }
                        });
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
            throw new AssertionError("timeout in lookup of party");

        if (!success.get()) {
            throw new AssertionError("looked up party doesn't have the same key");
        }

    }*/
}
