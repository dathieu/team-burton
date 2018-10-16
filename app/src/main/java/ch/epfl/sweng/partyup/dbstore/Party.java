package ch.epfl.sweng.partyup.dbstore;


import android.net.Uri;

import java.util.List;

import ch.epfl.sweng.partyup.containers.Tuple;
import ch.epfl.sweng.partyup.dbstore.listeners.CompletionListener;
import ch.epfl.sweng.partyup.dbstore.listeners.SchemaListener;
import ch.epfl.sweng.partyup.dbstore.schemas.PartySchema;
import ch.epfl.sweng.partyup.dbstore.schemas.ProposalSchema;
import ch.epfl.sweng.partyup.dbstore.schemas.SongSchema;
import ch.epfl.sweng.partyup.dbstore.statemachine.DBResult;

public interface Party {
    void getPartySchema(CompletionListener<Tuple<DBResult, PartySchema>> callback);

    void setName(String name, CompletionListener<DBResult> callback);

    void vote(ProposalSchema proposal, boolean upvote, CompletionListener<DBResult> callback);

    void removeVote(ProposalSchema proposal, CompletionListener<DBResult> callback);

    void addProposal(ProposalSchema proposal, CompletionListener<DBResult> callback);

    void removeProposal(ProposalSchema proposal, CompletionListener<DBResult> callback);

    void updateCurrentSong(SongSchema newSong, boolean isNewSong, CompletionListener<DBResult> callback);

    void addGuestSongInfoListener(SchemaListener<SongSchema> listener);

    void removeGuestSongInfoListener(SchemaListener<SongSchema> listener);

    void addProposalListener(SchemaListener<ProposalSchema> listener);

    void removeProposalListener(SchemaListener<ProposalSchema> listener);

    void addNameListener(SchemaListener<String> listener);

    void removeNameListener(SchemaListener<String> listener);

    void endParty(CompletionListener<DBResult> callback);

    void addEndedListener(SchemaListener<Boolean> listener);

    void removeEndedListener(SchemaListener<Boolean> listener);

    String getKey();

    void uploadPhoto(Uri file, final CompletionListener<DBResult> callback);

    void getPhotoUrl(CompletionListener<List<String>> callback);
}
