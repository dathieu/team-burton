package ch.epfl.sweng.partyup.dbstore;


import ch.epfl.sweng.partyup.containers.Tuple;
import ch.epfl.sweng.partyup.dbstore.listeners.CompletionListener;
import ch.epfl.sweng.partyup.dbstore.listeners.SchemaListener;
import ch.epfl.sweng.partyup.dbstore.statemachine.DBResult;
import ch.epfl.sweng.partyup.dbstore.statemachine.DBState;

public interface Connection {

    //!!! need to signIn only once, think to check the state
    void signIn(CompletionListener<DBResult> callback);

    DBResult signOut();

    void createParty(CompletionListener<Tuple<DBResult, Party>> callback);

    void connectToParty(String key, CompletionListener<Tuple<DBResult, Party>> callback);

    void registerSpotifyId(String spotifyId, CompletionListener<DBResult> callback);

    void lookupParties(String spotifyId, SchemaListener<Party> schemaListener);

    String getUserName();

    int getPartiesSize();

    DBState getState();
}