package ch.epfl.sweng.partyup;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.widget.PopupMenu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.TextView;

import com.android.volley.toolbox.JsonObjectRequest;
import com.google.zxing.WriterException;
import com.spotify.sdk.android.authentication.AuthenticationClient;
import com.spotify.sdk.android.authentication.AuthenticationResponse;

import ch.epfl.sweng.partyup.SpotDB.SpotDBRequestQueue;
import ch.epfl.sweng.partyup.SpotDB.SpotDBSearchHandler;
import ch.epfl.sweng.partyup.containers.Tuple;
import ch.epfl.sweng.partyup.dbstore.listeners.CompletionListener;
import ch.epfl.sweng.partyup.dbstore.listeners.SchemaListener;
import ch.epfl.sweng.partyup.dbstore.schemas.PartySchema;
import ch.epfl.sweng.partyup.dbstore.schemas.SongSchema;
import ch.epfl.sweng.partyup.dbstore.statemachine.DBResult;

public class GuestActivity extends UserActivity {

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Button playPause = (Button) findViewById(R.id.playPause);
        Button nextButton = (Button) findViewById(R.id.nextButton);

        //Set the player for the guest

        SeekBar trackProgressBar = (SeekBar) findViewById(R.id.track_progress_bar);
        playPause.setVisibility(View.GONE);
        nextButton.setVisibility(View.GONE);
        trackProgressBar.setVisibility(View.GONE);
        unlock(this);
        //Generate the QrCode
        try {
            bitmap = generateQRCode(SharedAndSavedData.connectedPartyID);
        } catch (WriterException exception) {
            exception.printStackTrace();
        }

        DynamicListFragment.newInstance(SharedAndSavedData.connectedParty);
        SearchFragment.newInstance(SharedAndSavedData.connectedParty);
        //Connect to the party
        SharedAndSavedData.connectedParty.addGuestSongInfoListener(new SchemaListener<SongSchema>() {
            @Override
            public void onItemAdded(SongSchema item) {

            }

            @Override
            public void onItemChanged(SongSchema item) {
                if (item != null) {
                    SpotifyPlayerControl.setCurrentlyPlaying(true);
                    SpotifyPlayerControl.setCurrentTrackUri(item.getSpotify_id());
                    ((TextView) findViewById(R.id.song_name_player)).setText(item.getName());
                    ((TextView) findViewById(R.id.artist_name_player)).setText(item.getArtist_name());
                    (findViewById(R.id.SaveButon)).setBackgroundResource(R.drawable.ic_favorite_border_black_24dp);

                    if (!item.getAlbum_url().equals("")) {
                        ImageView albumView = (ImageView) findViewById(R.id.AlbumPreview);
                        new DownloadImageTask(albumView)
                                .execute(item.getAlbum_url());

                    }
                }
            }

            @Override
            public void onItemDeleted(SongSchema item) {

            }
        });

        final TextView nameview = ((TextView) findViewById(R.id.partyName));
        SharedAndSavedData.connectedParty.addNameListener(new SchemaListener<String>() {
            @Override
            public void onItemAdded(String item) {

            }

            @Override
            public void onItemChanged(String item) {
                nameview.setText(item);
            }

            @Override
            public void onItemDeleted(String item) {

            }
        });

        SharedAndSavedData.connectedParty.addEndedListener(new SchemaListener<Boolean>() {
            @Override
            public void onItemAdded(Boolean item) {

            }

            @Override
            public void onItemChanged(Boolean item) {
                if (item) {
                    SharedAndSavedData.connectedParty = null;
                    SharedAndSavedData.connectedPartyID = null;
                    SharedAndSavedData.setSavedConnectedInfo(GuestActivity.this, null, false);
                    Intent gobackWelcome = new Intent(GuestActivity.this, WelcomeScreen.class);
                    gobackWelcome.setFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
                    startActivityIfNeeded(gobackWelcome, 0);
                    GuestActivity.this.finish();
                }
            }

            @Override
            public void onItemDeleted(Boolean item) {

            }
        });
        //Goes on the proposal fragment
        changeView(new DynamicListFragment(), true, false);
        SharedAndSavedData.connectedParty.getPartySchema(new CompletionListener<Tuple<DBResult, PartySchema>>() {
            @Override
            public void onCompleted(Tuple<DBResult, PartySchema> result) {
                if (result.object1 == DBResult.Success) {
                    nameview.setText(result.object2.getName());
                }
            }
        });
    }


    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == SpotAuth.AUTH_TOKEN_REQUEST_CODE) {

            final AuthenticationResponse response = AuthenticationClient.getResponse(resultCode, data);
            if (response.getType() == AuthenticationResponse.Type.TOKEN) {
                SpotAuth.setSpotToken(response.getAccessToken());
                SpotAuth.setAuthenticated(true);

            }
        }
    }

    /**
     * Opens the search fragment, launch a spotify authentication if the guest is not authenticated yet
     *
     * @param view the view calling this method
     */
    public void onSearchButtonClick(View view) {
        if (SpotAuth.getSpotToken() == null) {
            String[] scopes = new String[]{"user-library-modify"};
            SpotAuth.spotRequest(this, scopes, AuthenticationResponse.Type.TOKEN);
        } else {
            SpotDBSearchHandler spotDBSearchHandler = new SpotDBSearchHandler(this, SpotAuth.getSpotToken());
            JsonObjectRequest jsonObjectRequest = spotDBSearchHandler.currentProfileRequest();
            SpotDBRequestQueue.getInstance(this.getApplicationContext()).addToRequestQueue(jsonObjectRequest);

            SearchFragment.newInstance(SharedAndSavedData.connectedParty);

            changeView(new SearchFragment(), false, true);
        }

    }

    /**
     * Show the popup to leave the party
     *
     * @param v the view calling this method
     */
    public void showPopup(View v) {
        PopupMenu popup = new PopupMenu(this, v);
        popup.setOnMenuItemClickListener(this);
        popup.inflate(R.menu.settings_popup_guest);
        popup.show();
    }

    /**
     * Allow the guest to leave the party
     *
     * @param item the item selected
     * @return true if the guest quit the party sucessfully
     */
    @Override
    public boolean onMenuItemClick(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.leave_party_item:
                SharedAndSavedData.connectedParty = null;
                SharedAndSavedData.connectedPartyID = null;
                SharedAndSavedData.setSavedConnectedInfo(this, null, false);
                Intent gobackWelcome = new Intent(this, WelcomeScreen.class);
                gobackWelcome.setFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
                startActivityIfNeeded(gobackWelcome, 0);
                finish();
                return true;
            default:
                return false;
        }
    }
}
