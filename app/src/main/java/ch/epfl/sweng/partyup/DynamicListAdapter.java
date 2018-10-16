package ch.epfl.sweng.partyup;

import android.app.Activity;
import android.support.constraint.ConstraintLayout;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.ToggleButton;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Locale;
import java.util.Map;

import ch.epfl.sweng.partyup.containers.Tuple;
import ch.epfl.sweng.partyup.dbstore.Connection;
import ch.epfl.sweng.partyup.dbstore.ConnectionProvider;
import ch.epfl.sweng.partyup.dbstore.Party;
import ch.epfl.sweng.partyup.dbstore.listeners.CompletionListener;
import ch.epfl.sweng.partyup.dbstore.listeners.SchemaListener;
import ch.epfl.sweng.partyup.dbstore.schemas.ProposalSchema;
import ch.epfl.sweng.partyup.dbstore.statemachine.DBResult;
import ch.epfl.sweng.partyup.eventlisteners.EventListener;
import ch.epfl.sweng.partyup.eventlisteners.ProposalAddListener;

public class DynamicListAdapter extends RecyclerView.Adapter {

    private ArrayList<Tuple<ProposalSchema, String>> mDataset;
    private static Connection dbConnection;
    private static Party party;
    public static final int MINIMUM_SCORE = -10;


    public boolean isOnHost = false;
    private ArrayList<EventListener> eventListeners;

    private ArrayList<ProposalAddListener> proposalAddListeners;

    /**
     * @return the proposals listeners
     */
    public ArrayList<ProposalAddListener> getProposalAddListeners() {
        return proposalAddListeners;
    }

    /**
     * Add a proposal listener
     *
     * @param proposalAddListener the listener to add
     */
    public void addProposalAddListener(ProposalAddListener proposalAddListener) {
        proposalAddListeners.add(proposalAddListener);
    }

    /**
     * Remove a listener
     *
     * @param proposalAddListener the listener to remove
     */
    public void removeProposalAddListener(ProposalAddListener proposalAddListener) {
        if (proposalAddListeners.contains(proposalAddListener))
            proposalAddListeners.remove(proposalAddListener);
    }

    /**
     * Add an event listener
     *
     * @param uiEventListener the listener to add
     */
    public void addEventListener(EventListener uiEventListener) {
        eventListeners.add(uiEventListener);
    }

    /**
     * Remove a event listener
     *
     * @param uiEventListener the listener to remove
     */
    public void removeEventListener(EventListener uiEventListener) {
        if (eventListeners.contains(uiEventListener)) {
            eventListeners.remove(uiEventListener);
        }
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        public TextView titleTextView;
        public TextView artistTextView;
        public TextView countView;
        public ToggleButton upvoteToggle;
        public ToggleButton downvoteToggle;
        public Button deleteProposalButton;
        public ConstraintLayout deleteLayout;

        public ViewHolder(View v) {
            super(v);
            titleTextView = (TextView) v.findViewById(R.id.titleTextView);
            artistTextView = (TextView) v.findViewById(R.id.artistTextView);
            countView = (TextView) v.findViewById(R.id.count);
            upvoteToggle = (ToggleButton) v.findViewById(R.id.likeButton);
            downvoteToggle = (ToggleButton) v.findViewById(R.id.dislikeButton);

            if (v.findViewById(R.id.deleteButton) != null) {
                deleteProposalButton = (Button) v.findViewById(R.id.deleteButton);
            }
        }
    }

    /**
     * Sort the proposal on the database
     */
    public void sortDatabase() {
        Comparator<Tuple<ProposalSchema, String>> comparator = new Comparator<Tuple<ProposalSchema, String>>() {
            @Override
            public int compare(Tuple<ProposalSchema, String> o1, Tuple<ProposalSchema, String> o2) {
                ProposalSchema left = o1.object1;
                ProposalSchema right = o2.object1;
                int leftScore = left.getScore();
                int rightScore = right.getScore();

                return rightScore - leftScore;
            }
        };

        Collections.sort(mDataset, comparator);
    }

    /**
     * Create a new adapter
     *
     * @param party      the party who host this adatper
     * @param activityUI the activity calling this method
     */
    public DynamicListAdapter(final Party party, final Activity activityUI) {
        DynamicListAdapter.party = party;
        dbConnection = ConnectionProvider.getConnection();
        mDataset = new ArrayList<>();
        eventListeners = new ArrayList<>();
        proposalAddListeners = new ArrayList<>();
        //Add to the party a new proposal
        DynamicListAdapter.party.addProposalListener(new SchemaListener<ProposalSchema>() {
            @Override
            public void onItemAdded(ProposalSchema item) {
                Log.e("adapter", "added child in firebase");
                mDataset.add(new Tuple<>(item, item.getSong_id()));
                sortDatabase();
                activityUI.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        notifyDataSetChanged();
                    }
                });

            }

            @Override
            public void onItemChanged(ProposalSchema item) {
                String key = item.getSong_id();

                if (item.getVoters() == null || item.getVoters().size() == 0 || item.getScore() < MINIMUM_SCORE) {
                    //dbConnection.getProposalsDatabaseReference().child(key).removeValue();
                    DynamicListAdapter.party.removeProposal(item, new CompletionListener<DBResult>() {
                        @Override
                        public void onCompleted(DBResult result) {

                        }
                    });
                }

                for (Tuple<ProposalSchema, String> existing : mDataset) {
                    ProposalSchema existingProposal = existing.object1;
                    String existingKey = existing.object2;
                    if (key.equals(existingKey)) {
                        existingProposal.setVoters(item.getVoters());
                        sortDatabase();
                        notifyDataSetChanged();
                        break;
                    }
                }
            }

            @Override
            public void onItemDeleted(ProposalSchema item) {
                String key = item.getSong_id();
                Tuple<ProposalSchema, String> toRemove = null;
                for (Tuple<ProposalSchema, String> existing : mDataset) {
                    String existingKey = existing.object2;
                    if (key.equals(existingKey)) {
                        toRemove = existing;
                        break;
                    }
                }
                if (toRemove != null) {
                    mDataset.remove(toRemove);
                }
                sortDatabase();
                activityUI.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        notifyDataSetChanged();
                    }
                });
            }
        });
    }

    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
      View proposalView = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_proposal_new, parent, false);
        if(!isOnHost){
            View delete=  proposalView.findViewById(R.id.deleteLayout);
            delete.setLayoutParams(new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.MATCH_PARENT,0));
            proposalView.findViewById(R.id.deleteButton).setVisibility(View.GONE);
        }

        return new ViewHolder(proposalView);
    }

    @Override
    public void onBindViewHolder(RecyclerView.ViewHolder viewHolder, int position) {
        Tuple<ProposalSchema, String> entry = mDataset.get(position);
        final ProposalSchema proposal = entry.object1;

        int numberVoters = 0;
        boolean likes = false;
        boolean dislikes = false;

        Map<String, Boolean> voters = proposal.getVoters();
        if (voters == null)
            numberVoters = 0;
        else {
            for (String voter : voters.keySet())
                if (voters.get(voter))
                    numberVoters++;
                else
                    numberVoters--;

            if (voters.containsKey(dbConnection.getUserName())) {
                likes = voters.get(dbConnection.getUserName());
                dislikes = !likes;
            }
        }

        String songName = proposal.getSong_name();
        String artistName = proposal.getArtist_name();

        final ViewHolder vh = (ViewHolder) viewHolder;

        vh.titleTextView.setText(songName);
        vh.artistTextView.setText(artistName);
        vh.countView.setText(String.format(Locale.US, "%d", numberVoters));

        vh.upvoteToggle.setChecked(likes);
        vh.downvoteToggle.setChecked(dislikes);

        vh.upvoteToggle.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                ToggleButton clicked = (ToggleButton) view;
                boolean checked = clicked.isChecked();

                if (checked) {
                    party.vote(proposal, true, new CompletionListener<DBResult>() {
                        @Override
                        public void onCompleted(DBResult result) {
                            switch (result) {
                                case Success:
                                    // nothing to do, all button are updated through the database update at redraw
                                    break;
                                default:
                                    //Todo
                                    break;
                            }
                        }
                    });
                } else {
                    party.removeVote(proposal, new CompletionListener<DBResult>() {
                        @Override
                        public void onCompleted(DBResult result) {
                            //if we removed the vote it is ok
                            //if it failed
                            switch (result) {
                                case Success:
                                    break;
                                default:
                                    //Todo
                                    break;
                            }
                        }
                    });
                }
            }
        });

        vh.downvoteToggle.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View clickedView) {
                ToggleButton clicked = (ToggleButton) clickedView;
                boolean checked = clicked.isChecked();

                if (checked) {
                    party.vote(proposal, false, new CompletionListener<DBResult>() {
                        @Override
                        public void onCompleted(DBResult result) {
                            switch (result) {
                                case Success:
                                    // nothing to do, all button are updated through the database update at redraw
                                    break;
                                default:
                                    //Todo
                                    break;
                            }
                        }
                    });
                } else {
                    party.removeVote(proposal, new CompletionListener<DBResult>() {
                        @Override
                        public void onCompleted(DBResult result) {
                            //if we removed the vote it is ok
                            //if it failed
                            switch (result) {
                                case Success:
                                    break;
                                default:
                                    //Todo
                                    break;
                            }
                        }
                    });
                }
            }
        });

        if (isOnHost) {
            vh.deleteProposalButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View clickedView) {

                    party.removeProposal(proposal, new CompletionListener<DBResult>() {
                        @Override
                        public void onCompleted(DBResult result) {

                        }
                    });
                }
            });
        }
        for (EventListener eventListener : eventListeners)
            eventListener.onUpdated();
    }

    /**
     * @return the number of items in this adapter
     */
    @Override
    public int getItemCount() {
        return mDataset.size();
    }

    /**
     * Remove the most uptoved song
     */
    public void removeTopSong() {


        party.removeProposal(mDataset.get(0).object1, new CompletionListener<DBResult>() {
            @Override
            public void onCompleted(DBResult result) {
            }
        });

    }

    /**
     * @return the most voted song
     */
    public ProposalSchema getTopVoted() {

        if (getItemCount() > 0) {

            return mDataset.get(0).object1;

        } else {
            return null;
        }
    }
}
