package ch.epfl.sweng.partyup;

import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import java.util.HashMap;
import java.util.Map;

import ch.epfl.sweng.partyup.containers.Tuple;
import ch.epfl.sweng.partyup.dbstore.Connection;
import ch.epfl.sweng.partyup.dbstore.ConnectionProvider;
import ch.epfl.sweng.partyup.dbstore.Party;
import ch.epfl.sweng.partyup.dbstore.listeners.CompletionListener;
import ch.epfl.sweng.partyup.dbstore.schemas.PartySchema;
import ch.epfl.sweng.partyup.dbstore.schemas.ProposalSchema;
import ch.epfl.sweng.partyup.dbstore.schemas.SongSchema;
import ch.epfl.sweng.partyup.dbstore.statemachine.DBResult;

public class SearchAdapter extends RecyclerView.Adapter<SearchAdapter.ViewHolder> {
    private SongSchema[] trackResults;
    private static Connection dbConnection;
    private Party party;


    public static class ViewHolder extends RecyclerView.ViewHolder {
        public TextView titleTextView;
        public TextView artistTextView;
        public ImageView addSongView;
        public View holderView;

        public ViewHolder(View songDisplay) {
            super(songDisplay);
            titleTextView = (TextView) songDisplay.findViewById(R.id.title_text_view);
            artistTextView = (TextView) songDisplay.findViewById(R.id.artist_text_view);
            addSongView = (ImageView) songDisplay.findViewById(R.id.addSongButton);
            holderView = songDisplay.findViewById(R.id.holder_layout);
        }
    }

    /**
     * Create a new adapter
     *
     * @param trackResults the list of tracks
     * @param party        the party to host the adapter
     */
    public SearchAdapter(SongSchema[] trackResults, Party party) {
        this.trackResults = trackResults;
        dbConnection = ConnectionProvider.getConnection();
        this.party = party;
    }

    @Override
    public SearchAdapter.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View songView = LayoutInflater.from(parent.getContext()).inflate(R.layout.song_display, parent, false);
        return new ViewHolder(songView);
    }

    @Override
    public void onBindViewHolder(final ViewHolder holder, final int position) {
        holder.titleTextView.setText(trackResults[holder.getAdapterPosition()].getName());
        holder.artistTextView.setText(trackResults[holder.getAdapterPosition()].getArtist_name());
        SongSchema proposalSong = trackResults[holder.getAdapterPosition()];
        canProposeSong(party, proposalSong, new CompletionListener<Boolean>() {
            @Override
            public void onCompleted(Boolean result) {
                if (!result) {
                    holder.addSongView.setImageResource(R.drawable.ic_check_circle_24dp);

                } else {
                    holder.addSongView.setImageResource(R.drawable.ic_add_circle_outline_24dp);
                }

            }
        });

        holder.addSongView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                SongSchema proposalSong = trackResults[holder.getAdapterPosition()];
                proposeSong(party, proposalSong, new CompletionListener<Boolean>() {
                    @Override
                    public void onCompleted(Boolean result) {
                        if (result) {
                            holder.addSongView.setImageResource(R.drawable.ic_check_circle_24dp);
                        }
                    }
                });
            }
        });
    }

    /**
     * Check if we can propose a song
     *
     * @param party    the current party
     * @param song     the song to propose
     * @param listener the listener
     */
    public static void canProposeSong(final Party party, final SongSchema song, final CompletionListener<Boolean> listener) {
        party.getPartySchema(new CompletionListener<Tuple<DBResult, PartySchema>>() {
            @Override
            public void onCompleted(Tuple<DBResult, PartySchema> result) {
                if (canPropose(result.object2, song)) {

                    listener.onCompleted(true);


                } else {
                    listener.onCompleted(false);
                }
            }
        });
    }

    /**
     * Propose a song to the proposal list of the party
     *
     * @param party    the current party
     * @param song     the song to propose
     * @param listener the listner
     */
    public static void proposeSong(final Party party, final SongSchema song, final CompletionListener<Boolean> listener) {
        final ProposalSchema proposal = new ProposalSchema();
        party.getPartySchema(new CompletionListener<Tuple<DBResult, PartySchema>>() {
            @Override
            public void onCompleted(Tuple<DBResult, PartySchema> result) {
                if (result.object1 == DBResult.Success && canPropose(result.object2, song)) {
                    proposal.setSong_name(song.getName());
                    proposal.setArtist_name(song.getArtist_name());

                    Map<String, Boolean> votes = new HashMap<>();
                    votes.put(dbConnection.getUserName(), true);
                    proposal.setVoters(votes);

                    proposal.setSong_id(song.getSpotify_id());

                    party.addProposal(proposal, new CompletionListener<DBResult>() {
                        @Override
                        public void onCompleted(DBResult result) {
                            listener.onCompleted(true);
                        }
                    });

                } else {
                    listener.onCompleted(false);
                }
            }
        });
    }

    /**
     * @return the number of items in the adapter
     */
    @Override
    public int getItemCount() {
        return trackResults.length;
    }

    /**
     * Check if we can porpose a song, to disallow duplicates
     *
     * @param partySchema the party schema
     * @param song        the song to propose
     * @return if true if there is no duplicates
     */
    private static boolean canPropose(PartySchema partySchema, SongSchema song) {
        boolean canPropose = true;
        String songName = song.getName();
        String artist = song.getArtist_name();
        Map<String, ProposalSchema> proposals = partySchema.getProposals();
        for (ProposalSchema proposal : proposals.values()) {
            if (proposal.getSong_name().equals(songName) && proposal.getArtist_name().equals(artist)) {
                canPropose = false;
            }
        }
        return canPropose;
    }
}
