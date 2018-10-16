package ch.epfl.sweng.partyup.dbstore;

import android.os.Handler;
import android.os.Looper;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import ch.epfl.sweng.partyup.containers.Tuple;
import ch.epfl.sweng.partyup.dbstore.listeners.CompletionListener;
import ch.epfl.sweng.partyup.dbstore.listeners.SchemaListener;
import ch.epfl.sweng.partyup.dbstore.schemas.PartySchema;
import ch.epfl.sweng.partyup.dbstore.statemachine.DBResult;
import ch.epfl.sweng.partyup.dbstore.statemachine.DBState;

public class MockedConnection implements Connection {

    DBState state;

    // delay for interactions with the database
    int asyncDelay = 200;

    HashMap<String, Set<String>> mapSpotifyToFirebaseUsers = new HashMap<>();
    HashMap<String, Set<String>> mapFirebaseUsersToParties = new HashMap<>();

    HashMap<String, Party> parties = new HashMap<>();
    String userName;

    static Connection instance = null;

    /**
     * the constuctor that setup a mocked connection
     */
    private MockedConnection() {
        state = DBState.SignedOut;
        userName = Tools.generateKey(15);
    }

    private static MockingState mockingState = MockingState.none;

    /**
     * multiple mocking mode
     */
    public enum MockingState {
        none,
        memories
    }

    /**
     * setup the mocking of memories
     */
    public void mockMemoriesActivity(){
        // remove the current state
        parties.clear();
        mapSpotifyToFirebaseUsers.clear();
        mapFirebaseUsersToParties.clear();

        mockingState = MockingState.memories;

        // set up sample parties
        int numParties = 5;
        for(int i =0; i<numParties; i++){
            PartySchema partySchema = new PartySchema();
            partySchema.setName(Tools.generateKey(5));
            partySchema.setEnded(true);
            partySchema.setTimeStamp(i);

            String key = Tools.generateKey(10);
            MockedParty party = new MockedParty(partySchema, key);

            parties.put(key, party);
        }
    }

    /**
     * reset the operating mode
     */
    public void resetMockingState(){
        mockingState=MockingState.none;
    }

    /**
     * get the database user id
     * @return the id of the user
     */
    @Override
    public String getUserName() {
        if (state != DBState.SignedIn)
            throw new AssertionError("you need to be signed in to get the user name");
        return userName;
    }

    /**
     *
     * @return the number of parties
     */
    @Override
    public int getPartiesSize() {
        return 0;
    }

    /**
     * register a spotify id to link party to user
     * @param spotifyId the spotify id
     * @param callback the methode to treat the result asynchronously
     */
    @Override
    public void registerSpotifyId(final String spotifyId, final CompletionListener<DBResult> callback) {
        if (state != DBState.SignedIn)
            throw new AssertionError("you need to be signed in to register a spotify id");

        Runnable registeringProcess = new Runnable() {
            @Override
            public void run() {
                try {
                    Thread.sleep(asyncDelay);
                } catch (Exception ex) {
                    throw new AssertionError("no exception may occur here");
                }

                if (mapSpotifyToFirebaseUsers.containsKey(spotifyId)) {
                    Set<String> userNames = mapSpotifyToFirebaseUsers.get(spotifyId);
                    userNames.add(getUserName());
                    mapSpotifyToFirebaseUsers.put(spotifyId, userNames);
                } else {
                    HashSet<String> userNames = new HashSet<String>();
                    userNames.add(getUserName());
                    mapSpotifyToFirebaseUsers.put(spotifyId, userNames);
                }

                callback.onCompleted(DBResult.Success);
            }
        };


        Handler mainHandler = new Handler(Looper.getMainLooper());
        mainHandler.post(registeringProcess);
    }

    /**
     * find party that a user have participated in
     * @param spotifyId the id of the user
     * @param schemaListener a listener to handle the found parties asynchronously
     */
    @Override
    public void lookupParties(final String spotifyId, final SchemaListener<Party> schemaListener) {
        if (state != DBState.SignedIn)
            throw new AssertionError("you need to be signed in to lookup parties");

        Runnable lookupProcess = new Runnable() {
            @Override
            public void run() {
                try {
                    Thread.sleep(asyncDelay);
                } catch (Exception ex) {
                    throw new AssertionError("no exception may occur here");
                }

                if(mockingState==MockingState.none)
                {
                    if (!mapSpotifyToFirebaseUsers.containsKey(spotifyId))
                        return;

                    Set<String> userNames = mapSpotifyToFirebaseUsers.get(spotifyId);
                    for (String userName : userNames) {
                        if (mapFirebaseUsersToParties.containsKey(userName)) {
                            Set<String> parties = mapFirebaseUsersToParties.get(userName);
                            for (String party : parties) {
                                connectToParty(party, new CompletionListener<Tuple<DBResult, Party>>() {
                                    @Override
                                    public void onCompleted(Tuple<DBResult, Party> result) {
                                        if (result.object1 == DBResult.Success) {
                                            schemaListener.onItemAdded(result.object2);
                                        }
                                    }
                                });
                            }
                        }
                    }
                }
                else{
                    switch (mockingState){
                        case memories:
                            for(Party party: parties.values())
                                schemaListener.onItemAdded(party);
                            break;
                        default:
                            throw new AssertionError("undefined behaviour at lookup of spotify parties");
                    }
                }
            }
        };

        Handler mainHandler = new Handler(Looper.getMainLooper());
        mainHandler.post(lookupProcess);
    }

    /**
     *
     * @return the state of the connection
     */
    @Override
    public DBState getState() {
        return state;
    }

    /**
     * the public methode to have a connection object
     * @return a connection object
     */
    public static Connection getInstance() {
        if (instance == null)
            instance = new MockedConnection();
        return instance;
    }

    /**
     * sign in to the database
     * @param callback the listener to handle the result asynchronously
     */
    @Override
    public void signIn(final CompletionListener<DBResult> callback) {
        if (state != DBState.SignedOut)
            throw new AssertionError("you need to be signed out to sign in");

        Runnable signInProcess = new Runnable() {
            @Override
            public void run() {
                try {
                    Thread.sleep(asyncDelay);
                } catch (Exception ex) {
                    throw new AssertionError("no exception may occur here");
                }
                state = DBState.SignedIn;
                userName = Tools.generateKey(15);
                callback.onCompleted(DBResult.Success);
            }
        };


        Handler mainHandler = new Handler(Looper.getMainLooper());
        mainHandler.post(signInProcess);
    }

    /**
     * sign out of the database
     * @return the result of the signout
     */
    @Override
    public DBResult signOut() {
        if (state != DBState.SignedIn)
            throw new AssertionError("you need to be signed in to sign out");

        state = DBState.SignedOut;
        return DBResult.Success;
    }

    /**
     * create a new party
     * @param callback the listener to handle the result asynchronously
     */
    @Override
    public void createParty(final CompletionListener<Tuple<DBResult, Party>> callback) {

        if (state != DBState.SignedIn)
            throw new AssertionError("you need to be signed in to create a party");

        Runnable creationProcess = new Runnable() {
            @Override
            public void run() {
                try {
                    Thread.sleep(asyncDelay);
                } catch (Exception ex) {
                    throw new AssertionError("no exception may occur here");
                }

                PartySchema partySchema = new PartySchema();
                partySchema.setName("a truly random name");

                HashMap<String, Boolean> users = new HashMap<>();
                String userName = getUserName();
                users.put(userName, true);
                partySchema.setUsers(users);

                partySchema.setTimeStamp(System.currentTimeMillis());

                // generate a new unique key
                String key = null;
                do {
                    key = Tools.generateKey(10);
                } while (parties.containsKey(key));

                Party party = new MockedParty(partySchema, key);
                parties.put(key, party);

                addFirebaseUserToParty(party.getKey());

                callback.onCompleted(new Tuple<DBResult, Party>(DBResult.Success, party));
            }
        };


        Handler mainHandler = new Handler(Looper.getMainLooper());
        mainHandler.post(creationProcess);
    }

    /**
     * connecting to an existing party
     * @param key the key of the party to join
     * @param callback the listener to handle the result asynchronously
     */
    @Override
    public void connectToParty(final String key, final CompletionListener<Tuple<DBResult, Party>> callback) {

        if (state != DBState.SignedIn)
            throw new AssertionError("you need to be signed in to connect to a party");

        Runnable connectionProcess = new Runnable() {
            @Override
            public void run() {
                try {
                    Thread.sleep(asyncDelay);
                } catch (Exception ex) {
                    throw new AssertionError("no exception may occur here");
                }

                // check whether the ID is a proper string
                Pattern pattern = Pattern.compile("^[a-zA-Z0-9-_]+$");
                Matcher matcher = pattern.matcher(key);
                boolean valid = matcher.matches();

                if (!valid) {
                    callback.onCompleted(new Tuple<DBResult, Party>(DBResult.InvalidKey, null));
                } else {

                    if (!parties.containsKey(key)) {
                        callback.onCompleted(new Tuple<DBResult, Party>(DBResult.NotFound, null));
                        return;
                    }

                    Party party = parties.get(key);
                    MockedParty mockedParty = (MockedParty) party;
                    PartySchema partySchema = mockedParty.directAccessPartySchema();
                    partySchema.getUsers().put(MockedConnection.getInstance().getUserName(), true);
                    addFirebaseUserToParty(party.getKey());

                    callback.onCompleted(new Tuple<DBResult, Party>(DBResult.Success, parties.get(key)));
                }
            }
        };


        Handler mainHandler = new Handler(Looper.getMainLooper());
        mainHandler.post(connectionProcess);
    }
    
    private void addFirebaseUserToParty(String partyKey) {
        Set<String> parties;
        if (mapFirebaseUsersToParties.containsKey(getUserName())) {
            parties = mapFirebaseUsersToParties.get(getUserName());
        } else {
            parties = new HashSet<>();
        }
        parties.add(partyKey);

        mapFirebaseUsersToParties.put(getUserName(), parties);
    }
}
