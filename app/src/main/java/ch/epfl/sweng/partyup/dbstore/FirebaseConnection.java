package ch.epfl.sweng.partyup.dbstore;

import android.support.annotation.NonNull;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import ch.epfl.sweng.partyup.containers.Tuple;
import ch.epfl.sweng.partyup.dbstore.listeners.CompletionListener;
import ch.epfl.sweng.partyup.dbstore.listeners.SchemaListener;
import ch.epfl.sweng.partyup.dbstore.schemas.PartySchema;
import ch.epfl.sweng.partyup.dbstore.statemachine.DBResult;
import ch.epfl.sweng.partyup.dbstore.statemachine.DBState;

public class FirebaseConnection implements Connection {

    static Connection instance = null;
    DBState state;
    HashMap<String, Party> parties;
    private static int numberOfParties;
    // authentication, using an anonymous user
    private FirebaseAuth firebaseAuth;
    private FirebaseUser user;

    private DatabaseReference partiesDatabaseReference;

    private StorageReference albumsReference;
    private DatabaseReference mapSpotifyToFirebaseUsers;
    private DatabaseReference mapFirebaseUsersToParties;

    /**
     * the constuctor that setup a connection to firebase
     */
    private FirebaseConnection() {
        parties = new HashMap<>();
        state = DBState.SignedOut;

        firebaseAuth = FirebaseAuth.getInstance();

        FirebaseDatabase firebaseDatabase = FirebaseDatabase.getInstance();
        firebaseDatabase.setPersistenceEnabled(true);
        firebaseDatabase.goOnline();

        FirebaseStorage storage = FirebaseStorage.getInstance();
        albumsReference = storage.getReference().child("albums");
        partiesDatabaseReference = firebaseDatabase.getReference().child("parties");
        mapSpotifyToFirebaseUsers = firebaseDatabase.getReference().child("mapSpotifyToFirebaseUsers");
        mapFirebaseUsersToParties = firebaseDatabase.getReference().child("mapFirebaseUsersToParties");
    }

    /**
     * the public methode to have a connection object
     * @return a connection object
     */
    public static Connection getInstance() {
        if (instance == null)
            instance = new FirebaseConnection();
        return instance;
    }

    /**
     * register a spotify id to link party to user
     * @param spotifyId the spotify id
     * @param callback the methode to treat the result asynchronously
     */
    @Override
    public void registerSpotifyId(String spotifyId, final CompletionListener<DBResult> callback) {
        if (state != DBState.SignedIn) {
            throw new AssertionError("you need to be signed in to register a spotify ID");
        }

        String condensedId = Tools.mapId(spotifyId);
        mapSpotifyToFirebaseUsers.child(condensedId).child(getUserName()).setValue(true).addOnCompleteListener(new OnCompleteListener<Void>() {
            @Override
            public void onComplete(@NonNull Task<Void> task) {
                if (task.isSuccessful())
                    callback.onCompleted(DBResult.Success);
                else
                    callback.onCompleted(DBResult.Failure);
            }
        });
    }

    /**
     * find party that a user have participated in
     * @param spotifyId the id of the user
     * @param schemaListener a listener to handle the found parties asynchronously
     */
    @Override
    public void lookupParties(String spotifyId, final SchemaListener<Party> schemaListener) {
        if (state != DBState.SignedIn)
            throw new AssertionError("you need to be signed in to lookup parties");

        numberOfParties = 0;
        String condensedId = Tools.mapId(spotifyId);
        mapSpotifyToFirebaseUsers.child(condensedId).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                if (dataSnapshot.exists()) {

                    // retrieve all users connected to this spotify id
                    Set<String> userNames = new HashSet<>();
                    for (DataSnapshot childSnapshot : dataSnapshot.getChildren()) {
                        userNames.add(childSnapshot.getKey());
                    }

                    // go through the usernames and look up the parties
                    for (String userName : userNames) {
                        mapFirebaseUsersToParties.child(userName).addListenerForSingleValueEvent(new ValueEventListener() {
                            @Override
                            public void onDataChange(DataSnapshot dataSnapshot) {
                                if (dataSnapshot.exists()) {
                                    // retrieve all parties connected to this user
                                    Set<String> parties = new HashSet<>();
                                    for (DataSnapshot childSnapshot : dataSnapshot.getChildren()) {
                                        parties.add(childSnapshot.getKey());
                                        numberOfParties ++;
                                    }

                                    // connect to all these parties
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

                            @Override
                            public void onCancelled(DatabaseError databaseError) {

                            }
                        });
                    }
                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

            }
        });
    }

    /**
     * get the database user id
     * @return the id of the user
     */
    @Override
    public String getUserName() {
        if (state != DBState.SignedIn)
            throw new AssertionError("you need to be signed in to get the user name");
        return user.getUid();
    }

    /**
     *
     * @return the number of parties
     */
    @Override
    public int getPartiesSize() {
        return numberOfParties;
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
     * sign in to the database
     * @param callback the listener to handle the result asynchronously
     */
    @Override
    public void signIn(final CompletionListener<DBResult> callback) {
        if (state != DBState.SignedOut)
            throw new AssertionError("you need to be signed out to sign in");

        if (firebaseAuth.getCurrentUser() != null) {
            firebaseAuth.getCurrentUser().reload();
            user = firebaseAuth.getCurrentUser();
            state = DBState.SignedIn;
            callback.onCompleted(DBResult.Success);
        } else {
            firebaseAuth.signInAnonymously().addOnCompleteListener(new OnCompleteListener<AuthResult>() {
                Task<AuthResult> instanceTask;

                @Override
                public void onComplete(@NonNull Task<AuthResult> task) {
                    instanceTask = task;
                    if (task.isSuccessful()) {
                        user = firebaseAuth.getCurrentUser();
                        state = DBState.SignedIn;
                        callback.onCompleted(DBResult.Success);
                    } else {
                        callback.onCompleted(DBResult.Failure);
                    }
                }
            });

        }
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
        firebaseAuth.signOut();
        user = null;
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


        final PartySchema partySchema = new PartySchema();
        partySchema.setName("My Party !");

        HashMap<String, Boolean> users = new HashMap<>();
        String userName = getUserName();
        users.put(userName, true);
        partySchema.setUsers(users);

        partySchema.setTimeStamp(System.currentTimeMillis());

        final DatabaseReference newPartyReference = partiesDatabaseReference.push();
        final StorageReference imagesReference = albumsReference.child(newPartyReference.getKey());
        newPartyReference.setValue(partySchema).addOnCompleteListener(new OnCompleteListener<Void>() {
            @Override
            public void onComplete(@NonNull Task<Void> task) {
                if (task.isSuccessful()) {
                    FirebaseParty party = new FirebaseParty(partySchema, newPartyReference, imagesReference);
                    parties.put(newPartyReference.getKey(), party);
                    mapFirebaseUsersToParties.child(getUserName()).child(party.getKey()).setValue(true);

                    callback.onCompleted(new Tuple<DBResult, Party>(DBResult.Success, party));
                } else {
                    callback.onCompleted(new Tuple<DBResult, Party>(DBResult.Failure, null));
                }
            }
        });
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

        if (parties.containsKey(key))
            callback.onCompleted(new Tuple<>(DBResult.Success, parties.get(key)));
        else {
            // check whether the ID is a proper string
            Pattern pattern = Pattern.compile("^[a-zA-Z0-9-_]+$");
            Matcher matcher = pattern.matcher(key);
            boolean valid = matcher.matches();

            if (!valid) {
                callback.onCompleted(new Tuple<DBResult, Party>(DBResult.InvalidKey, null));
            } else {
                final DatabaseReference newPartyReference = partiesDatabaseReference.child(key);
                final StorageReference imagesReference = albumsReference.child(newPartyReference.getKey());
                newPartyReference.addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(DataSnapshot dataSnapshot) {
                        if (dataSnapshot.exists()) {
                            PartySchema partySchema = dataSnapshot.getValue(PartySchema.class);

                            // add current user
                            Map<String, Boolean> users = partySchema.getUsers();
                            users.put(getUserName(), true);
                            partySchema.setUsers(users);

                            FirebaseParty party = new FirebaseParty(partySchema, newPartyReference, imagesReference);
                            parties.put(newPartyReference.getKey(), party);

                            // register us as participating in this party
                            partiesDatabaseReference.child(dataSnapshot.getKey()).child("users").child(getUserName()).setValue(true);
                            mapFirebaseUsersToParties.child(getUserName()).child(party.getKey()).setValue(true);

                            callback.onCompleted(new Tuple<DBResult, Party>(DBResult.Success, party));
                        } else {
                            callback.onCompleted(new Tuple<DBResult, Party>(DBResult.NotFound, null));
                        }
                    }

                    @Override
                    public void onCancelled(DatabaseError databaseError) {
                        callback.onCompleted(new Tuple<DBResult, Party>(DBResult.Failure, null));
                    }
                });
            }
        }
    }

}
