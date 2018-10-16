package ch.epfl.sweng.partyup;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import org.json.JSONException;
import org.json.JSONObject;

import ch.epfl.sweng.partyup.SpotDB.SpotDBRequestQueue;
import ch.epfl.sweng.partyup.SpotDB.SpotDBResponseHandler;
import ch.epfl.sweng.partyup.SpotDB.SpotDBResponseType;
import ch.epfl.sweng.partyup.SpotDB.SpotDBSearchHandler;
import ch.epfl.sweng.partyup.dbstore.Party;
import ch.epfl.sweng.partyup.dbstore.listeners.CompletionListener;
import ch.epfl.sweng.partyup.dbstore.schemas.Playlist;
import ch.epfl.sweng.partyup.dbstore.schemas.SongSchema;


public class PlaylistFragment extends Fragment implements SpotDBResponseHandler {
    private RecyclerView.LayoutManager playlistLayoutManager;
    private View rootView;
    private SpotDBSearchHandler searchHandler;
    private String user_id;
    private Playlist[] playlists;
    private PlaylistAdapter playlistAdapter;
    private RecyclerView recyclerView;
    private static Party party;

    /**
     * Handle the responses from spotify
     *
     * @param response the spotify response
     * @param type     the type of the response
     */
    @Override
    public void onSpotDBResponse(JSONObject response, SpotDBResponseType type) {
        //If the request was for the porfile of the user
        if (type.equals(SpotDBResponseType.PROFILE)) {
            try {
                user_id = (String) response.get("id");
                SpotDBRequestQueue.getInstance(getActivity()).addToRequestQueue(searchHandler.userPlaylistsRequest(user_id));

            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
        //If the request was for the palylists
        if (type.equals(SpotDBResponseType.PLAYLISTS)) {
            try {
                Log.e("ANSWER", "playlists");
                playlists = SpotDBSearchHandler.makePlaylistArray(response);
                Log.e("ANSWER", "" + playlists.length);
                playlistAdapter = new PlaylistAdapter(playlists, this, (UserActivity)getActivity());
                recyclerView.setAdapter(playlistAdapter);
            } catch (JSONException e) {
                e.printStackTrace();
            }

        }
        //If the request was for the tracklost of a playlist
        if (type.equals(SpotDBResponseType.TRACKLIST)) {
            try {
                SongSchema[] songs = SpotDBSearchHandler.makeSongArray(response, true);
                for (SongSchema s : songs) {
                    SearchAdapter.proposeSong(party, s, new CompletionListener<Boolean>() {
                        @Override
                        public void onCompleted(Boolean result) {

                        }
                    });
                }


            } catch (JSONException e) {
                e.printStackTrace();
            }
        }

    }

    /**
     * Create a new instance of the fragment
     *
     * @param party the current party
     * @return a new instance of our fragment
     */
    public static PlaylistFragment newInstance(Party party) {
        PlaylistFragment fragment = new PlaylistFragment();
        PlaylistFragment.party = party;
        return fragment;
    }

    /**
     * Propose in the proposal list all the songs from a given playlist
     *
     * @param playlist the playlist
     */
    public void proposeSongsFromPlaylist(Playlist playlist) {
        Log.e("ANSWER", playlist.getName());
        searchHandler = new SpotDBSearchHandler(this, SpotAuth.getSpotToken());
        SpotDBRequestQueue.getInstance(getActivity()).
                addToRequestQueue(searchHandler.playlistTracksRequest(playlist.getSpotify_id(), playlist.getUser_id()));

    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    /**
     * Create the view for our fragment
     *
     * @param inflater           the inflater used for the fragment
     * @param container          the container used for the fragment
     * @param savedInstanceState the saved instance
     * @return return the viex of the fragment
     */
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        rootView = inflater.inflate(R.layout.fragment_playlist, container, false);
        recyclerView = (RecyclerView) rootView.findViewById(R.id.recyclerPlaylist);

        playlistLayoutManager = new LinearLayoutManager(this.getContext());
        recyclerView.setLayoutManager(playlistLayoutManager);


        playlistAdapter = new PlaylistAdapter(new Playlist[]{},this,(UserActivity)getActivity());

        recyclerView.setAdapter(playlistAdapter);

        searchHandler = new SpotDBSearchHandler(this, SpotAuth.getSpotToken());
        SpotDBRequestQueue.getInstance(getActivity()).addToRequestQueue(searchHandler.currentProfileRequest());

        return rootView;
    }


}
