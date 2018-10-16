package ch.epfl.sweng.partyup;

import android.app.Activity;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.android.volley.toolbox.JsonObjectRequest;

import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;

import ch.epfl.sweng.partyup.SpotDB.SpotDBRequestQueue;
import ch.epfl.sweng.partyup.SpotDB.SpotDBResponseHandler;
import ch.epfl.sweng.partyup.SpotDB.SpotDBResponseType;
import ch.epfl.sweng.partyup.SpotDB.SpotDBSearchHandler;
import ch.epfl.sweng.partyup.dbstore.schemas.PartySchema;
import ch.epfl.sweng.partyup.dbstore.schemas.SongSchema;

public class SongListAdapter extends RecyclerView.Adapter {

    private ArrayList<SongSchema> mDataset = new ArrayList<>();
    private SongListFragment songListFragment;
    private Activity activity;

    public static class ViewHolder extends RecyclerView.ViewHolder {
        public TextView titleTextView;
        public TextView artistTextView;
        public TextView countView;
        public ImageView addToSpotifyButton;


        public ViewHolder(View v) {
            super(v);
            titleTextView = (TextView) v.findViewById(R.id.titleTextView);
            artistTextView = (TextView) v.findViewById(R.id.artistTextView);
            countView = (TextView) v.findViewById(R.id.count);
            addToSpotifyButton = (ImageView) v.findViewById(R.id.addToSpotifyButton);
        }
    }

    /**
     * The adapter constructor
     *
     * @param partySchema      the party schema to use
     * @param activity         the activity needing this adapter
     * @param songListFragment the song list fragment
     */
    public SongListAdapter(PartySchema partySchema, Activity activity, SongListFragment songListFragment) {
        this.activity = activity;
        this.songListFragment = songListFragment;
        for (SongSchema song : partySchema.getPlayed_songs().values()) {
            mDataset.add(song);
        }

        Collections.sort(mDataset, new Comparator<SongSchema>() {
            @Override
            public int compare(SongSchema o1, SongSchema o2) {
                return Long.signum(o1.getTimestamp() - o2.getTimestamp());
            }
        });
    }

    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View proposalView = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_song_memory, parent, false);
        return new ViewHolder(proposalView);
    }

    @Override
    public void onBindViewHolder(RecyclerView.ViewHolder viewHolder, final int position) {
        final SongSchema entry = mDataset.get(viewHolder.getAdapterPosition());
        String songName = entry.getName();
        String artistName = entry.getArtist_name();
        final ViewHolder vh = (ViewHolder) viewHolder;

        if (songListFragment.savedSongs.contains(vh.getAdapterPosition())) {
            vh.addToSpotifyButton.setImageDrawable(activity.getDrawable(R.drawable.ic_favorite_black_24dp));
        } else {
            vh.addToSpotifyButton.setImageDrawable(activity.getDrawable(R.drawable.ic_favorite_border_black_24dp));
        }
        vh.titleTextView.setText(songName);
        vh.artistTextView.setText(artistName);
        vh.addToSpotifyButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Log.e("ADDED", entry.getSpotify_id());
                String uri = entry.getSpotify_id();
                String id;
                if (uri.split(":").length == 3) {

                    id = uri.split(":")[2];
                    Log.e("ADDED", id);
                    vh.addToSpotifyButton.setImageDrawable(activity.getDrawable(R.drawable.ic_favorite_black_24dp));
                    JsonObjectRequest saveTrackRequest = new SpotDBSearchHandler(new SpotDBResponseHandler() {
                        @Override
                        public void onSpotDBResponse(JSONObject response, SpotDBResponseType type) {
                            Log.e("ADDED", "worked");
                        }
                    },
                            SpotAuth.getSpotToken()).saveToLibraryRequest(SpotDBResponseType.TRACK_SAVE, id);

                    SpotDBRequestQueue.getInstance(activity).addToRequestQueue(saveTrackRequest);
                    Toast.makeText(activity, R.string.added_to_spotify, Toast.LENGTH_SHORT).show();

                    songListFragment.savedSongs.add(vh.getAdapterPosition());
                }
            }

        });
    }

    /**
     * @return the numbers of items in this adaptere
     */
    @Override
    public int getItemCount() {
        return mDataset.size();
    }

}
