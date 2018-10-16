package ch.epfl.sweng.partyup;

import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import ch.epfl.sweng.partyup.dbstore.schemas.Playlist;

public class PlaylistAdapter extends RecyclerView.Adapter<PlaylistAdapter.ViewHolder> {
    private Playlist[] playlistResults;

    private PlaylistFragment playlistFragment;

    private AppCompatActivity mActivity;

    public static class ViewHolder extends RecyclerView.ViewHolder {
        public TextView titleTextView;
        public ImageView imageView;
        public View playlistView;

        public ViewHolder(View playlistDisplay) {
            super(playlistDisplay);
            titleTextView = (TextView) playlistDisplay.findViewById(R.id.title_text_view);
            playlistView = playlistDisplay.findViewById(R.id.playlist_display_layout);
            imageView = (ImageView) playlistDisplay.findViewById(R.id.playlist_image_display);
        }
    }

    public PlaylistAdapter(Playlist[] playlistsResults,PlaylistFragment pf, AppCompatActivity mActivity) {
        this.playlistResults = playlistsResults;
        this.playlistFragment = pf;
        this.mActivity = mActivity;
    }

    /**
     * Create the view Holder
     *
     * @param parent   the parent view
     * @param viewType the type of the view
     * @return a view holder for the adapter
     */
    @Override
    public PlaylistAdapter.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.playlist_display, parent, false);
        return new ViewHolder(v);
    }

    /**
     * Bind the view holder
     *
     * @param holder   the holder
     * @param position its static position
     */
    @Override
    public void onBindViewHolder(final ViewHolder holder, final int position) {
        holder.titleTextView.setText(playlistResults[holder.getAdapterPosition()].getName());
        if (playlistResults[holder.getAdapterPosition()].getCover_url() != null) {
            new DownloadImageTask(holder.imageView)
                    .execute(playlistResults[holder.getAdapterPosition()].getCover_url());
        }
        holder.playlistView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Toast.makeText(mActivity, R.string.playlist_added, Toast.LENGTH_SHORT).show();
                playlistFragment.proposeSongsFromPlaylist(playlistResults[holder.getAdapterPosition()]);
            }
        });
    }

    /**
     * @return the number of items in the adapter
     */
    @Override
    public int getItemCount() {
        return playlistResults.length;
    }
}
