package ch.epfl.sweng.partyup.dbstore;

import android.net.Uri;
import android.os.Handler;
import android.os.Looper;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import ch.epfl.sweng.partyup.containers.Tuple;
import ch.epfl.sweng.partyup.dbstore.listeners.CompletionListener;
import ch.epfl.sweng.partyup.dbstore.listeners.SchemaListener;
import ch.epfl.sweng.partyup.dbstore.schemas.PartySchema;
import ch.epfl.sweng.partyup.dbstore.schemas.ProposalSchema;
import ch.epfl.sweng.partyup.dbstore.schemas.SongSchema;
import ch.epfl.sweng.partyup.dbstore.statemachine.DBResult;

public class MockedParty implements Party {

    private PartySchema partySchema;
    private String key;

    private int asyncDelay = 200;

    private HashMap<String, ProposalSchema> proposals;

    private ArrayList<SchemaListener<ProposalSchema>> proposalEventListeners;
    private ArrayList<SchemaListener<String>> nameEventListeners;
    private ArrayList<SchemaListener<Boolean>> endedEventListeners;
    ArrayList<SchemaListener<SongSchema>> currentSongEventListeners;

    private ArrayList<Uri> imageUrl = new ArrayList<>();

    /**
     * MockedParty used to run the tests locally
     * @param partySchema the schema corresponding to the party
     * @param key corresponding to the firebase key
     */
    protected MockedParty(PartySchema partySchema, String key) {

        proposalEventListeners = new ArrayList<>();
        nameEventListeners = new ArrayList<>();
        endedEventListeners = new ArrayList<>();
        currentSongEventListeners = new ArrayList<>();

        this.partySchema = partySchema;
        this.key = key;
        this.imageUrl = new ArrayList<>();
    }

    /**
     * @return the firebase key
     */
    public String getKey() {
        return key;
    }

    /**
     * Return the partyschema asynchronously
     * @param callback return Success and the partySchema
     */
    @Override
    public void getPartySchema(final CompletionListener<Tuple<DBResult, PartySchema>> callback) {
        Runnable gettingProcess = new Runnable() {
            @Override
            public void run() {
                try {
                    Thread.sleep(asyncDelay);
                } catch (Exception ex) {
                    throw new AssertionError("no exception may occur here");
                }
                callback.onCompleted(new Tuple<>(DBResult.Success, partySchema));
            }
        };

        Handler mainHandler = new Handler(Looper.getMainLooper());
        mainHandler.post(gettingProcess);
    }

    protected PartySchema directAccessPartySchema() {
        return partySchema;
    }

    /**
     * Set the name of the party and notify all the name listeners
     * @param name of the party
     * @param callback returns Success in case the name has correctly been set
     */
    @Override
    public void setName(final String name, final CompletionListener<DBResult> callback) {
        Runnable namingProcess = new Runnable() {
            @Override
            public void run() {
                try {
                    Thread.sleep(asyncDelay);
                } catch (Exception ex) {
                    throw new AssertionError("no exception may occur here");
                }
                partySchema.setName(name);

                for (SchemaListener<String> nameEventListener : nameEventListeners)
                    nameEventListener.onItemChanged(partySchema.getName());

                callback.onCompleted(DBResult.Success);
            }
        };


        Handler mainHandler = new Handler(Looper.getMainLooper());
        mainHandler.post(namingProcess);
    }

    /**
     * Add a proposal to the party and notify all the proposal listeners
     * @param proposal the proposal to add
     * @param callback returns Success if the proposal has correctly been added
     *                 or if the proposal was already in the list of proposals
     */
    @Override
    public void addProposal(final ProposalSchema proposal, final CompletionListener<DBResult> callback) {
        Runnable addingProcess = new Runnable() {
            @Override
            public void run() {
                try {
                    Thread.sleep(asyncDelay);
                } catch (Exception ex) {
                    throw new AssertionError("no exception may occur here.");
                }
                if (partySchema.getProposals().containsKey(proposal.getSong_id()))
                    callback.onCompleted(DBResult.Success);

                partySchema.getProposals().put(proposal.getSong_id(), proposal);

                // notify everyone of the new proposal
                for (SchemaListener<ProposalSchema> proposalEventLister : proposalEventListeners)
                    proposalEventLister.onItemAdded(proposal);

                callback.onCompleted(DBResult.Success);
            }
        };


        Handler mainHandler = new Handler(Looper.getMainLooper());
        mainHandler.post(addingProcess);
    }

    /**
     * Remove a proposal from the party, notify all the proposal listeners
     * @param proposal the proposal
     * @param callback returns NotFound in case the proposal doesn't exist,
     *                 returns Success if the proposal has correctly been removed
     */
    @Override
    public void removeProposal(final ProposalSchema proposal, final CompletionListener<DBResult> callback) {
        Runnable removalProcess = new Runnable() {
            @Override
            public void run() {
                try {
                    Thread.sleep(asyncDelay);
                } catch (Exception ex) {
                    throw new AssertionError("no exception may occur here");
                }
                if (!partySchema.getProposals().containsKey(proposal.getSong_id()))
                    callback.onCompleted(DBResult.NotFound);

                partySchema.getProposals().remove(proposal.getSong_id());

                // notify everyone of the new proposal
                for (SchemaListener<ProposalSchema> proposalEventLister : proposalEventListeners)
                    proposalEventLister.onItemDeleted(proposal);

                callback.onCompleted(DBResult.Success);
            }
        };


        Handler mainHandler = new Handler(Looper.getMainLooper());
        mainHandler.post(removalProcess);
    }

    /**
     * Update the currently played song, add it to the played songs of the party and notify all song listeners
     * @param newSong the new song that is played
     * @param isNewSong to signal if the song should be added to the played songs
     * @param callback returns Success in case the update is correctly executed
     */
    @Override
    public void updateCurrentSong(final SongSchema newSong, final boolean isNewSong, final CompletionListener<DBResult> callback) {
        Runnable updatingProcess = new Runnable() {
            @Override
            public void run() {
                try {
                    Thread.sleep(asyncDelay);
                } catch (Exception ex) {
                    throw new AssertionError("no exception may occur here");
                }

                partySchema.setCurrentSong(newSong);
                if (isNewSong) {
                    partySchema.getPlayed_songs().put(Tools.generateKey(20), newSong);
                }

                for (SchemaListener<SongSchema> listener : currentSongEventListeners) {
                    listener.onItemChanged(newSong);
                }

                callback.onCompleted(DBResult.Success);
            }
        };


        Handler mainHandler = new Handler(Looper.getMainLooper());
        mainHandler.post(updatingProcess);
    }

    /**
     * Add the song info (currently played song) listener for the guest
     * @param listener the listener
     */
    @Override
    public void addGuestSongInfoListener(SchemaListener<SongSchema> listener) {
        if (!currentSongEventListeners.contains(listener))
            currentSongEventListeners.add(listener);
    }

    /**
     * Remove the song info (currently played song) listener for the guest
     * @param listener the listener
     */
    @Override
    public void removeGuestSongInfoListener(SchemaListener<SongSchema> listener) {
        currentSongEventListeners.remove(listener);
    }

    /**
     * Vote for your song
     * @param proposal for which we vote
     * @param value true means upvote, false means downvote
     * @param callback callback returns NotFound in case the proposal doesn't exist,
     *                 returns Success if the vote has correctly been set
     */
    @Override
    public void vote(final ProposalSchema proposal, final boolean value, final CompletionListener<DBResult> callback) {
        Runnable votingProcess = new Runnable() {
            @Override
            public void run() {
                try {
                    Thread.sleep(asyncDelay);
                } catch (Exception ex) {
                    throw new AssertionError("no exception may occur here");
                }
                if (!partySchema.getProposals().containsKey(proposal.getSong_id()))
                    callback.onCompleted(DBResult.NotFound);

                ProposalSchema found = partySchema.getProposals().get(proposal.getSong_id());
                Map<String, Boolean> voters = found.getVoters();
                voters.put(MockedConnection.getInstance().getUserName(), value);

                // notify everyone of the new proposal
                for (SchemaListener<ProposalSchema> proposalEventLister : proposalEventListeners)
                    proposalEventLister.onItemChanged(proposal);

                callback.onCompleted(DBResult.Success);
            }
        };


        Handler mainHandler = new Handler(Looper.getMainLooper());
        mainHandler.post(votingProcess);
    }

    /**
     * Remove a vote and notify the proposalEventListener
     * @param proposal for which we need to remove the vote
     * @param callback returns NotFound in case the proposal doesn't exist,
     *                 returns Success if the vote has correctly been removed
     */
    @Override
    public void removeVote(final ProposalSchema proposal, final CompletionListener<DBResult> callback) {
        Runnable votingProcess = new Runnable() {
            @Override
            public void run() {
                try {
                    Thread.sleep(asyncDelay);
                } catch (Exception ex) {
                    throw new AssertionError("no exception may occur here");
                }
                if (!partySchema.getProposals().containsKey(proposal.getSong_id()))
                    callback.onCompleted(DBResult.NotFound);

                ProposalSchema found = partySchema.getProposals().get(proposal.getSong_id());
                Map<String, Boolean> voters = found.getVoters();
                voters.remove(MockedConnection.getInstance().getUserName());

                // notify everyone of the new proposal
                for (SchemaListener<ProposalSchema> proposalEventLister : proposalEventListeners)
                    proposalEventLister.onItemChanged(proposal);

                callback.onCompleted(DBResult.Success);
            }
        };


        Handler mainHandler = new Handler(Looper.getMainLooper());
        mainHandler.post(votingProcess);
    }

    /**
     * End the party
     * @param callback returns success in case the party has correctly been ended
     */
    @Override
    public void endParty(final CompletionListener<DBResult> callback) {
        final Runnable endingProcess = new Runnable() {
            @Override
            public void run() {
                try {
                    Thread.sleep(asyncDelay);
                } catch (Exception ex) {
                    throw new AssertionError("no exception may occur here");
                }

                partySchema.setEnded(true);

                // notify everyone of the change in the party
                for (SchemaListener<Boolean> listener : endedEventListeners)
                    listener.onItemChanged(true);

                callback.onCompleted(DBResult.Success);
            }
        };


        Handler mainHandler = new Handler(Looper.getMainLooper());
        mainHandler.post(endingProcess);
    }

    /**
     * Add a listener for the proposals
     * @param listener the listener
     */
    @Override
    public void addProposalListener(SchemaListener<ProposalSchema> listener) {
        if (!this.proposalEventListeners.contains(listener))
            this.proposalEventListeners.add(listener);

        for (ProposalSchema proposal : partySchema.getProposals().values())
            listener.onItemAdded(proposal);
    }

    /**
     * Remove a listener for the proposals
     * @param listener the listener
     */
    @Override
    public void removeProposalListener(SchemaListener<ProposalSchema> listener) {
        this.proposalEventListeners.remove(listener);
    }

    /**
     * Add a listener for the name of the party
     * @param nameListener the listener
     */
    @Override
    public void addNameListener(SchemaListener<String> nameListener) {
        if (!nameEventListeners.contains(nameListener))
            nameEventListeners.add(nameListener);
    }

    /**
     * Remove a listener for the name of the party
     * @param listener the listener
     */
    @Override
    public void removeNameListener(SchemaListener<String> listener) {
        nameEventListeners.remove(listener);
    }

    /**
     * Add a listener for the end of the party
     * @param listener the listener
     */
    @Override
    public void addEndedListener(SchemaListener<Boolean> listener) {
        if (!endedEventListeners.contains(listener))
            endedEventListeners.add(listener);
    }

    /**
     * Remove the listener for the end of the party
     * @param listener the listener
     */
    @Override
    public void removeEndedListener(SchemaListener<Boolean> listener) {
        endedEventListeners.remove(listener);
    }

    /**
     * Upload the photo asynchronously
     * @param file the uri of the file in the device
     * @param callback returns Success if the upload is successful
     */
    @Override
    public void uploadPhoto(final Uri file, final CompletionListener<DBResult> callback) {
        Runnable uploadPhotoProcess = new Runnable() {
            @Override
            public void run() {
                try {
                    Thread.sleep(asyncDelay);
                } catch (Exception ex) {
                    throw new AssertionError("no exception may occur here");
                }
                imageUrl.add(file);
                callback.onCompleted(DBResult.Success);
            }
        };

        Handler mainHandler = new Handler(Looper.getMainLooper());
        mainHandler.post(uploadPhotoProcess);
    }

    /**
     * Get all the urls of all the photo taken during the party to download them afterwards.
     * @param callback returns the list of the urls asynchronously
     */
    @Override
    public void getPhotoUrl(final CompletionListener<List<String>> callback) {
        Runnable downloadPhotoUrlProcess = new Runnable() {
            @Override
            public void run() {
                try {
                    Thread.sleep(asyncDelay);
                } catch (Exception ex) {
                    throw new AssertionError("no exception may occur here");
                }
                List<String> urlImageList = new ArrayList<>();
                for (Uri childSnapshot : imageUrl) {
                    String url = childSnapshot.toString();
                    urlImageList.add(url);

                }
                callback.onCompleted(urlImageList);
            }
        };
        Handler mainHandler = new Handler(Looper.getMainLooper());
        mainHandler.post(downloadPhotoUrlProcess);
    }

}
