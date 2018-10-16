package ch.epfl.sweng.partyup;

import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.support.v7.widget.PopupMenu;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import com.android.volley.toolbox.JsonObjectRequest;
import com.google.zxing.WriterException;
import com.spotify.sdk.android.authentication.AuthenticationClient;
import com.spotify.sdk.android.authentication.AuthenticationResponse;

import ch.epfl.sweng.partyup.SpotDB.SpotDBRequestQueue;
import ch.epfl.sweng.partyup.SpotDB.SpotDBSearchHandler;
import ch.epfl.sweng.partyup.containers.Tuple;
import ch.epfl.sweng.partyup.dbstore.ConnectionProvider;
import ch.epfl.sweng.partyup.dbstore.Party;
import ch.epfl.sweng.partyup.dbstore.listeners.CompletionListener;
import ch.epfl.sweng.partyup.dbstore.listeners.SchemaListener;
import ch.epfl.sweng.partyup.dbstore.schemas.PartySchema;
import ch.epfl.sweng.partyup.dbstore.schemas.ProposalSchema;
import ch.epfl.sweng.partyup.dbstore.schemas.SongSchema;
import ch.epfl.sweng.partyup.dbstore.statemachine.DBResult;


public class HostActivity extends UserActivity implements SetNameFragment.SetNameListener, ConfirmEndPartyFragment.ConfirmEndPartyListener {
    /**
     * Create the HostActivity
     *
     * @param savedInstanceState the bundle to use
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        //request access Token
        String[] scopes = new String[]{"streaming", "playlist-read-private", "playlist-read-collaborative", "user-library-modify"};
        SpotAuth.spotRequest(this, scopes, AuthenticationResponse.Type.TOKEN);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        finish();
    }


    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == SpotAuth.AUTH_TOKEN_REQUEST_CODE) {

            final AuthenticationResponse response = AuthenticationClient.getResponse(resultCode, data);
            if (response.getType() == AuthenticationResponse.Type.TOKEN) {
                SpotAuth.setSpotToken(response.getAccessToken());

                Toast.makeText(HostActivity.this, "Login Successful", Toast.LENGTH_SHORT).show();

                SpotDBSearchHandler spotDBSearchHandler = new SpotDBSearchHandler(this, SpotAuth.getSpotToken());
                JsonObjectRequest jsonObjectRequest = spotDBSearchHandler.currentProfileRequest();
                SpotDBRequestQueue.getInstance(this.getApplicationContext()).addToRequestQueue(jsonObjectRequest);


                if (SharedAndSavedData.connectedParty != null) {
                    setupParty();
                    SharedAndSavedData.connectedParty.getPartySchema(new CompletionListener<Tuple<DBResult, PartySchema>>() {
                        @Override
                        public void onCompleted(Tuple<DBResult, PartySchema> result) {
                            if (result.object1 == DBResult.Success) {
                                ((TextView) findViewById(R.id.partyName)).setText(result.object2.getName());
                            }
                        }
                    });

                    spotifyPlayer = new SpotifyPlayerControl(response, HostActivity.this, SharedAndSavedData.connectedParty);
                    unlock(this);
                } else {
                    //Create party now
                    ConnectionProvider.getConnection().createParty(new CompletionListener<Tuple<DBResult, Party>>() {
                        @Override
                        public void onCompleted(Tuple<DBResult, Party> result) {
                            switch (result.object1) {
                                case Success:
                                    SharedAndSavedData.connectedParty = result.object2;
                                    SharedAndSavedData.connectedPartyID = SharedAndSavedData.connectedParty.getKey();
                                    SharedAndSavedData.setSavedConnectedInfo(HostActivity.this, SharedAndSavedData.connectedPartyID, true);

                                    Log.e("created party: ", SharedAndSavedData.connectedPartyID);

                                    setupParty();

                                    spotifyPlayer = new SpotifyPlayerControl(response, HostActivity.this, SharedAndSavedData.connectedParty);
                                    unlock(HostActivity.this);
                                    break;
                                default:
                                    break;
                            }
                        }
                    });
                }


            } else {
                Toast.makeText(HostActivity.this, R.string.errorMessage, Toast.LENGTH_SHORT).show();
                Intent gobackWelcome = new Intent(this, WelcomeScreen.class);
                gobackWelcome.setFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
                startActivityIfNeeded(gobackWelcome, 0);
                finish();
            }
        }
    }

    /**
     * Set the qrcode, and the database for the party
     */
    private void setupParty() {
        try {
            bitmap = generateQRCode(SharedAndSavedData.connectedPartyID);
        } catch (WriterException exception) {
            exception.printStackTrace();
        }
        QRCodeFragment.newInstance(bitmap);

        // we have created a party, create a fragment for the voting
        // and put it in center
        DynamicListFragment.newInstance(SharedAndSavedData.connectedParty);

        SearchFragment.newInstance(SharedAndSavedData.connectedParty);

        FragmentManager fragmentManager = getSupportFragmentManager();
        FragmentTransaction transaction = fragmentManager.beginTransaction();
        transaction.replace(R.id.content, proposalFragment = new DynamicListFragment()).commit();

        SharedAndSavedData.connectedParty.addProposalListener(new SchemaListener<ProposalSchema>() {

            @Override
            public void onItemAdded(ProposalSchema item) {
            }

            @Override
            public void onItemChanged(ProposalSchema item) {

            }

            @Override
            public void onItemDeleted(ProposalSchema item) {

            }

        });

        SharedAndSavedData.connectedParty.addNameListener(new SchemaListener<String>() {
            @Override
            public void onItemAdded(String item) {
            }

            @Override
            public void onItemChanged(String item) {
                ((TextView) findViewById(R.id.partyName)).setText(item);
            }

            @Override
            public void onItemDeleted(String item) {
            }

        });
    }

    /**
     * Show the poprp to end the party of set the name
     *
     * @param v the view calling this method
     */
    public void showPopup(View v) {
        PopupMenu popup = new PopupMenu(this, v);
        popup.setOnMenuItemClickListener(this);
        popup.inflate(R.menu.settings_popup_host);
        popup.show();
    }

    /**
     * Handle to change tabs
     *
     * @param item the item cicked by the host
     * @return true if the fragment transaction was successful
     */
    @Override
    public boolean onMenuItemClick(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.set_name_item:
                DialogFragment newFragment = new SetNameFragment();
                newFragment.show(getSupportFragmentManager(), "setNameTag");
                return true;
            case R.id.end_party_item:
                DialogFragment newEndFragment = new ConfirmEndPartyFragment();
                newEndFragment.show(getSupportFragmentManager(), "confirmEndPartyTag");
                return true;
            default:
                return false;
        }
    }

    /**
     * Set the name of the party
     *
     * @param dialog       the dialog containing the set name
     * @param newPartyName the new name for the party
     */
    @Override
    public void onDialogSetNameClick(DialogFragment dialog, String newPartyName) {
        final String newName = newPartyName;
        final TextView textView = (TextView) findViewById(R.id.partyName);
        SharedAndSavedData.connectedParty.setName(newPartyName, new CompletionListener<DBResult>() {
            @Override
            public void onCompleted(DBResult result) {
                if (result == DBResult.Success) {
                    textView.setText(newName);
                }
            }
        });
    }

    /**
     * End the party, exclude all guests.
     *
     * @param dialog    the dialog containing the end party
     * @param confirmed the confirmation
     */
    @Override
    public void onDialogConfirmEndPartyClick(DialogFragment dialog, boolean confirmed) {
        SharedAndSavedData.connectedParty.endParty(new CompletionListener<DBResult>() {
            @Override
            public void onCompleted(DBResult result) {
                if (result == DBResult.Success) {
                    SharedAndSavedData.connectedParty = null;
                    SharedAndSavedData.connectedPartyID = null;
                    SharedAndSavedData.setSavedConnectedInfo(HostActivity.this, null, false);
                    Intent gobackWelcome = new Intent(HostActivity.this, WelcomeScreen.class);
                    gobackWelcome.setFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
                    startActivityIfNeeded(gobackWelcome, 0);
                    HostActivity.this.finish();
                }
            }
        });
    }

    /**
     * @return the player
     */
    public SpotifyPlayerControl getPlayer() {
        return spotifyPlayer;
    }

    /**
     * Create a Song from a proposal
     *
     * @param p the schema
     * @return the song
     */
    public SongSchema makeSongFromProposal(ProposalSchema p) {
        return new SongSchema(p.getSong_name(), p.getArtist_name(), null, p.getSong_id(), null, System.currentTimeMillis());
    }

    /**
     * PLay of pause the player
     *
     * @param view the view calling this method
     */
    public void playPause(View view) {
        ProposalSchema topProp = null;
        if (proposalFragment != null) {
            topProp = DynamicListFragment.getTopVoted();
        }
        if (spotifyPlayer.currentlyPlaying()) {
            spotifyPlayer.playPause();
        } else {
            if (topProp != null) {
                DynamicListFragment.removeTopSong();
                spotifyPlayer.playNewTrack(makeSongFromProposal(topProp));
            }
        }
    }

    /**
     * Play the next song in the proposal list
     *
     * @param view the viex calling this method
     */
    public void next(View view) {
        ProposalSchema topProp = null;
        if (proposalFragment != null) {
            topProp = DynamicListFragment.getTopVoted();
        }
        if (topProp != null) {
            DynamicListFragment.removeTopSong();
            spotifyPlayer.playNewTrack(makeSongFromProposal(topProp));
            (findViewById(R.id.SaveButon)).setBackgroundResource(R.drawable.ic_favorite_border_black_24dp);
        }


    }

    /**
     * opens the playlist fragment
     *
     * @param view the view calling this method
     */
    public void onPlaylistClick(View view) {
        playlistFragment = PlaylistFragment.newInstance(SharedAndSavedData.connectedParty);
        changeView(playlistFragment, false, true);
    }

    /**
     * Opens the search fragment
     *
     * @param view th eview calling this method
     */
    @Override
    public void onSearchButtonClick(View view) {
        changeView(new SearchFragment(), false, true);
    }

}
