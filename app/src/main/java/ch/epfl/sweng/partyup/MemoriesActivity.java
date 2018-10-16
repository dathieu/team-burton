package ch.epfl.sweng.partyup;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.UiThread;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.DividerItemDecoration;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.widget.Toast;

import com.android.volley.toolbox.JsonObjectRequest;
import com.spotify.sdk.android.authentication.AuthenticationClient;
import com.spotify.sdk.android.authentication.AuthenticationResponse;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;

import ch.epfl.sweng.partyup.SpotDB.SpotDBRequestQueue;
import ch.epfl.sweng.partyup.SpotDB.SpotDBResponseHandler;
import ch.epfl.sweng.partyup.SpotDB.SpotDBResponseType;
import ch.epfl.sweng.partyup.SpotDB.SpotDBSearchHandler;
import ch.epfl.sweng.partyup.containers.Tuple;
import ch.epfl.sweng.partyup.dbstore.Connection;
import ch.epfl.sweng.partyup.dbstore.ConnectionProvider;
import ch.epfl.sweng.partyup.dbstore.Party;
import ch.epfl.sweng.partyup.dbstore.listeners.CompletionListener;
import ch.epfl.sweng.partyup.dbstore.listeners.SchemaListener;
import ch.epfl.sweng.partyup.dbstore.schemas.PartySchema;
import ch.epfl.sweng.partyup.dbstore.statemachine.DBResult;
import ch.epfl.sweng.partyup.dbstore.statemachine.DBState;

public class MemoriesActivity extends AppCompatActivity implements SpotDBResponseHandler {
    private static String spotifyUserId;
    private Connection dbConnection;
    private RecyclerView memoriesRecyclerView;
    private RecyclerView.Adapter memoriesAdapter;
    private static ArrayList<Tuple<Party, PartySchema>> partyList;
    private int missedParties = 0;
    private Activity thisActivity;

    /**
     * Create a new instanc eof the memories
     *
     * @param savedInstanceState the saved instance
     */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.fragment_memories);
        thisActivity = this;
        UserActivity.lockForLoading(this);
        memoriesRecyclerView = (RecyclerView) findViewById(R.id.memories_recycler_view);
        memoriesRecyclerView.setHasFixedSize(true);

        RecyclerView.LayoutManager memoriesLayoutManager = new LinearLayoutManager(this);
        memoriesRecyclerView.setLayoutManager(memoriesLayoutManager);

        dbConnection = ConnectionProvider.getConnection();

        //User should be signed in to firebase
        if (dbConnection.getState() != DBState.SignedIn) {
            throw new AssertionError("User should be signed in to firebase");
        }

        //No access token -> Sign in to spotify
        SpotAuth.spotRequest(this, new String[]{"user-library-modify"}, AuthenticationResponse.Type.TOKEN);
        //No spotify id -> get current user's profile//We have everything we need -> display parties

    }


    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == SpotAuth.AUTH_TOKEN_REQUEST_CODE) {

            final AuthenticationResponse response = AuthenticationClient.getResponse(resultCode, data);
            if (response.getType() == AuthenticationResponse.Type.TOKEN) {
                SpotAuth.setSpotToken(response.getAccessToken());
                SpotAuth.setAuthenticated(true);
                getCurrentUserProfile();
            } else {
                Toast.makeText(MemoriesActivity.this, R.string.errorMessage, Toast.LENGTH_SHORT).show();
                Intent gobackWelcome = new Intent(this, WelcomeScreen.class);
                gobackWelcome.setFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
                startActivityIfNeeded(gobackWelcome, 0);
                finish();
            }
        }
    }

    /**
     * Launch a request for the profile of the current user
     */
    private void getCurrentUserProfile() {
        //Get current user's profile
        SpotDBSearchHandler searchHandler = new SpotDBSearchHandler(this, SpotAuth.getSpotToken());
        JsonObjectRequest profileRequest = searchHandler.currentProfileRequest();
        SpotDBRequestQueue.getInstance(this).addToRequestQueue(profileRequest);
    }

    /**
     * Handle the response of spotify
     *
     * @param response     the spotify response
     * @param responseType the type of the response
     */
    @Override
    public void onSpotDBResponse(JSONObject response, SpotDBResponseType responseType) {
        if (responseType == SpotDBResponseType.PROFILE) {
            try {
                spotifyUserId = (String) response.get("id");
            } catch (JSONException ex) {
                ex.printStackTrace();
                throw new AssertionError("response should have 'id' field");
            }
            displayParties();
        }
    }

    /**
     * Display the parties in the recylcerview
     */
    private void displayParties() {
        partyList = new ArrayList<>();
        missedParties = 0;
        //Set adapter
        memoriesAdapter = new MemoriesAdapter(this, partyList);
        memoriesRecyclerView.setAdapter(memoriesAdapter);
        memoriesRecyclerView.addItemDecoration(new DividerItemDecoration(this, DividerItemDecoration.VERTICAL));
        dbConnection.lookupParties(spotifyUserId, new SchemaListener<Party>() {
            @Override
            public void onItemAdded(final Party item) {
                item.getPartySchema(new CompletionListener<Tuple<DBResult, PartySchema>>() {
                    @Override
                    public void onCompleted(Tuple<DBResult, PartySchema> result) {
                        if (result.object1 == DBResult.Success) {
                            UserActivity.unlock(thisActivity);
                            partyList.add(new Tuple<>(item, result.object2));
                            Log.e("memory", result.object2.getName() + ", " + memoriesAdapter.getItemCount());
                            sortParties();
                            dataSetChanged();
                        } else if (result.object1 == DBResult.Failure) {
                            missedParties++;
                        }
                    }
                });
            }

            @Override
            public void onItemChanged(Party item) {
                //Parties shouldn't change anymore
            }

            @Override
            public void onItemDeleted(Party item) {
                //Parties shouldn't get deleted
            }
        });
    }

    /**
     * Notifier of data changes
     */
    @UiThread
    private void dataSetChanged() {
        memoriesAdapter.notifyDataSetChanged();
    }

    /**
     * Sort the parties by date
     */
    public void sortParties() {
        Comparator<Tuple<Party, PartySchema>> comparator = new Comparator<Tuple<Party, PartySchema>>() {
            @Override
            public int compare(Tuple<Party, PartySchema> o1, Tuple<Party, PartySchema> o2) {
                long leftDate = o1.object2.getTimeStamp();
                long rightDate = o2.object2.getTimeStamp();
                return (int) (rightDate - leftDate);
            }
        };

        Collections.sort(partyList, comparator);
    }

    /**
     * Set the default spotify userID
     *
     * @param userId the new userID
     */
    public static void setSpotifyUserId(String userId) {
        spotifyUserId = userId;
    }

    /**
     * @return the memories recycler view
     */
    public RecyclerView getMemoriesRecyclerView() {
        return memoriesRecyclerView;
    }
}
