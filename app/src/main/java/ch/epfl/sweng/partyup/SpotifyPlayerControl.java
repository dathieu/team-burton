package ch.epfl.sweng.partyup;

import android.app.Activity;
import android.content.Context;
import android.util.Log;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import com.android.volley.toolbox.JsonObjectRequest;
import com.spotify.sdk.android.authentication.AuthenticationResponse;
import com.spotify.sdk.android.player.Config;
import com.spotify.sdk.android.player.ConnectionStateCallback;
import com.spotify.sdk.android.player.Error;
import com.spotify.sdk.android.player.Player;
import com.spotify.sdk.android.player.PlayerEvent;
import com.spotify.sdk.android.player.Spotify;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import ch.epfl.sweng.partyup.SpotDB.SpotDBRequestQueue;
import ch.epfl.sweng.partyup.SpotDB.SpotDBResponseHandler;
import ch.epfl.sweng.partyup.SpotDB.SpotDBResponseType;
import ch.epfl.sweng.partyup.SpotDB.SpotDBSearchHandler;
import ch.epfl.sweng.partyup.dbstore.Party;
import ch.epfl.sweng.partyup.dbstore.Tools;
import ch.epfl.sweng.partyup.dbstore.listeners.CompletionListener;
import ch.epfl.sweng.partyup.dbstore.schemas.SongSchema;
import ch.epfl.sweng.partyup.dbstore.statemachine.DBResult;

public class SpotifyPlayerControl implements Player.NotificationCallback, ConnectionStateCallback, Player.OperationCallback, SpotDBResponseHandler {

    private Player mPlayer;
    private SpotifyPlayerControl spotifyPlayerControl;
    private boolean playing = false;
    private ImageView albumView;

    private static String currentTrackUri;
    private static Boolean currentlyPlaying = false;
    private Context context;

    private TrackTimer timer;
    private SeekBar trackProgressBar;

    private TextView artistNameText;
    private TextView songNameText;
    private String songName;
    private String artistName;
    private Button playButton;

    private Party party;
    private HostActivity hostActivity;

    private SongSchema currentSong;

    /**
     * Initialize the player
     *
     * @param response     the spotify response to create the player
     * @param hostActivity the activity that called this method
     * @param party        the party on which the player will be created
     */
    public SpotifyPlayerControl(AuthenticationResponse response, final Activity hostActivity, Party party) {
        initializePlayer(response, hostActivity, party);
    }

    /**
     * @return the state of the player
     */
    public static Boolean getCurrentlyPlaying() {
        return currentlyPlaying;
    }

    /**
     * @param currentlyPlaying the new playing state
     */
    public static void setCurrentlyPlaying(Boolean currentlyPlaying) {
        SpotifyPlayerControl.currentlyPlaying = currentlyPlaying;
    }

    /**
     * @param response     the response from spotify
     * @param hostActivity the activity calling this method
     * @param party        the party on which the player will be created
     * @return true if the player has been correctly created
     */
    private boolean initializePlayer(AuthenticationResponse response, final Activity hostActivity, Party party) {
        Config playerConfig = new Config(hostActivity, response.getAccessToken(), "d98c4f3e380346ee8ac0e19fd16e9749");

        context = hostActivity;
        spotifyPlayerControl = this;
        playButton = (Button) hostActivity.findViewById(R.id.playPause);
        //Get the player from spotify
        Spotify.getPlayer(playerConfig, this, new com.spotify.sdk.android.player.SpotifyPlayer.InitializationObserver() {
            @Override
            public void onInitialized(com.spotify.sdk.android.player.SpotifyPlayer spotifyPlayer) {
                mPlayer = spotifyPlayer;
                mPlayer.addConnectionStateCallback(spotifyPlayerControl);
                mPlayer.addNotificationCallback(spotifyPlayerControl);
            }

            @Override
            public void onError(Throwable throwable) {
                Log.e("MainActivity", "Could not initialize player: " + throwable.getMessage());
            }
        });

        //get the different player elements
        albumView = (ImageView) hostActivity.findViewById(R.id.AlbumPreview);
        songNameText = (TextView) hostActivity.findViewById(R.id.song_name_player);
        artistNameText = (TextView) hostActivity.findViewById(R.id.artist_name_player);

        //get progress bar and add appropriate listener
        trackProgressBar = (SeekBar) hostActivity.findViewById(R.id.track_progress_bar);
        trackProgressBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            //The listener will skip to the corresponding part of the song when the user modifies progress
            @Override
            public void onProgressChanged(final SeekBar seekBar, final int progress, boolean fromUser) {
                if (fromUser) {
                    mPlayer.seekToPosition(new Player.OperationCallback() {
                        @Override
                        public void onSuccess() {
                            seekBar.setProgress(progress);
                        }

                        @Override
                        public void onError(Error error) {
                            Log.e("timer", "Error:" + error.toString());
                        }
                    }, progress);
                }
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {

            }
        });
        this.party = party;
        //Initialize the timer to communicate with the progress bar
        timer = new TrackTimer(trackProgressBar);
        this.hostActivity = (HostActivity) hostActivity;
        return true;

    }

    /**
     * @return true if the player is actually playing
     */
    public boolean isPlaying() {
        return playing;
    }

    /**
     * Play a new Song
     *
     * @param song the new track to play
     */
    public void playNewTrack(SongSchema song) {
        setSongName(song.getName());
        setArtistName(song.getArtist_name());
        setCurrentSongTitleText(songName, artistName);
        startPlaying(song.getSpotify_id());
        currentSong = song;
        party.updateCurrentSong(song, true, new CompletionListener<DBResult>() {
            @Override
            public void onCompleted(DBResult result) {

            }
        });
        playButton.setBackgroundResource(R.mipmap.pause_button150dpi);
        playing = true;
    }

    /**
     * Pauses the player
     */
    public void pause() {
        mPlayer.pause(spotifyPlayerControl);
        timer.pause();
        playButton.setBackgroundResource(R.mipmap.play_button150dpi);
        playing = false;
    }

    /**
     * Resume the player
     */
    public void resume() {
        mPlayer.resume(spotifyPlayerControl);
        timer.start();
        playButton.setBackgroundResource(R.mipmap.pause_button150dpi);
        playing = true;
    }

    /**
     * Play or pause the player
     */
    public void playPause() {
        if (isPlaying()) {
            pause();
        } else {
            resume();
        }

    }

    /**
     * Start a song for the first time
     *
     * @param spotifyUri the new song to play
     */
    public void startPlaying(String spotifyUri) {
        timer.reInitialize();
        mPlayer.playUri(spotifyPlayerControl, spotifyUri, 0, 0);
        setCurrentAlbumCover(spotifyUri);
        playing = true;
    }

    /**
     * @return the state of the player
     */
    public Boolean currentlyPlaying() {
        return mPlayer.getMetadata().currentTrack != null;
    }

    /**
     * Set a new album cover
     *
     * @param uri the uri of the new album cover
     */
    private void setCurrentAlbumCover(String uri) {
        uri = uri.split(":")[2];

        JsonObjectRequest trackRequest = new SpotDBSearchHandler(this, SpotAuth.getSpotToken()).fullTrackRequest(uri);
        Log.e("REQUEST", trackRequest.toString());
        SpotDBRequestQueue.getInstance(context).addToRequestQueue(trackRequest);

    }

    /**
     * Set a new song on the player
     *
     * @param songName   the name of the song
     * @param artistName the name of the artist
     */
    private void setCurrentSongTitleText(String songName, String artistName) {
        songNameText.setText(songName);
        artistNameText.setText(artistName);
    }

    /**
     * Save the current track to the spotify library of the user
     *
     * @param userActivity the activity calling this method
     */
    public static void saveCurrentTrackToLibrary(UserActivity userActivity) {

        if (getCurrentlyPlaying()) {
            Log.e("SAVED", "try");
            String uri = getCurrentTrackUri();
            String id = uri.split(":")[2];
            saveTrackToLibrary(id, userActivity);
            Button saveButton = (Button) userActivity.findViewById(R.id.SaveButon);
            saveButton.setBackgroundResource(R.drawable.ic_favorite_black_24dp);
            Toast.makeText(userActivity, R.string.added_to_spotify, Toast.LENGTH_SHORT).show();

        }
    }

    /**
     * Save any track to the user's spotify library
     *
     * @param id           id of the track
     * @param userActivity the activity calling this
     */
    public static void saveTrackToLibrary(String id, UserActivity userActivity) {
        JsonObjectRequest saveTrackRequest = new SpotDBSearchHandler(userActivity, SpotAuth.getSpotToken()).saveToLibraryRequest(SpotDBResponseType.TRACK_SAVE, id);
        SpotDBRequestQueue.getInstance(userActivity).addToRequestQueue(saveTrackRequest);

    }

    /**
     * @return the uri of the current track
     */
    public static String getCurrentTrackUri() {
        return currentTrackUri;
    }

    /**
     * set the current track
     *
     * @param currentTrackUri uri of the new track
     */
    public static void setCurrentTrackUri(String currentTrackUri) {
        SpotifyPlayerControl.currentTrackUri = currentTrackUri;
    }

    @Override
    public void onLoggedIn() {


    }

    @Override
    public void onLoggedOut() {

    }

    @Override
    public void onLoginFailed(Error error) {

    }

    @Override
    public void onTemporaryError() {

    }

    @Override
    public void onConnectionMessage(String message) {

    }

    @Override
    public void onPlaybackEvent(PlayerEvent playerEvent) {
        Log.e("PLAYEREVENT", playerEvent.toString());

        if (playerEvent == PlayerEvent.kSpPlaybackNotifyMetadataChanged) {
            int duration = (int) mPlayer.getMetadata().currentTrack.durationMs;
            setCurrentTrackUri(mPlayer.getMetadata().currentTrack.uri);
            trackProgressBar.setMax(duration);
            timer.start();
            setCurrentlyPlaying(true);
        }
        if (playerEvent == PlayerEvent.kSpPlaybackNotifyAudioDeliveryDone) {
            setCurrentlyPlaying(false);
            hostActivity.next(null);
        }

    }


    @Override
    public void onPlaybackError(Error error) {

    }

    @Override
    public void onSuccess() {

    }

    @Override
    public void onError(Error error) {

    }

    /**
     * Handle the responses of spotify
     *
     * @param response the spotify response
     * @param type     the type of the spotify response
     */
    @Override
    public void onSpotDBResponse(JSONObject response, SpotDBResponseType type) {
        //Rsponse to update the album cover
        try {
            JSONArray images = (JSONArray) response.getJSONObject("album").get("images");
            String imageUrl = (String) images.getJSONObject(1).get("url");
            currentSong.setAlbum_url(imageUrl);
            party.updateCurrentSong(currentSong, false, new CompletionListener<DBResult>() {
                @Override
                public void onCompleted(DBResult result) {

                }
            });
            new DownloadImageTask(albumView)
                    .execute(imageUrl);
        } catch (JSONException exception) {
            exception.printStackTrace();
        }
    }

    /**
     * Set the name of the song on the player
     *
     * @param songName the title of the song
     */
    public void setSongName(String songName) {
        this.songName = songName;
    }

    /**
     * Set the name of the artist on the player
     *
     * @param artistName the name of the artist
     */
    public void setArtistName(String artistName) {
        this.artistName = artistName;
    }
}


