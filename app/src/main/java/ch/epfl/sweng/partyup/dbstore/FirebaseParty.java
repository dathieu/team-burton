package ch.epfl.sweng.partyup.dbstore;

import android.net.Uri;
import android.support.annotation.NonNull;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.storage.StorageMetadata;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import ch.epfl.sweng.partyup.containers.Tuple;
import ch.epfl.sweng.partyup.dbstore.listeners.CompletionListener;
import ch.epfl.sweng.partyup.dbstore.listeners.SchemaListener;
import ch.epfl.sweng.partyup.dbstore.schemas.PartySchema;
import ch.epfl.sweng.partyup.dbstore.schemas.ProposalSchema;
import ch.epfl.sweng.partyup.dbstore.schemas.SongSchema;
import ch.epfl.sweng.partyup.dbstore.statemachine.DBResult;

public class FirebaseParty implements Party {

    private PartySchema partySchema;
    private String key;

    private HashMap<ProposalSchema, String> proposals;

    private SongSchema currentSong;

    ArrayList<SchemaListener<ProposalSchema>> proposalEventListeners;
    ArrayList<SchemaListener<String>> nameEventListeners;
    ArrayList<SchemaListener<Boolean>> endedEventListeners;
    ArrayList<SchemaListener<SongSchema>> currentSongEventListeners;

    // our party and the tables within our party
    private DatabaseReference ourPartyDatabaseReference;
    private DatabaseReference playedSongsDatabaseReference;
    private DatabaseReference proposalsDatabaseReference;
    private DatabaseReference usersDatabaseReference;
    private DatabaseReference currentSongDatabaseReference;
    private DatabaseReference imageUrlDatabaseReference;

    private StorageReference imagesReference;

    /**
     * Set up all the listeners
     * @param partySchema the schema of the party
     * @param ourPartyDatabaseReference the party database reference
     * @param imagesReference the storage reference for the photos
     */
    protected FirebaseParty(final PartySchema partySchema, DatabaseReference ourPartyDatabaseReference, StorageReference imagesReference) {

        this.ourPartyDatabaseReference = ourPartyDatabaseReference;
        playedSongsDatabaseReference = ourPartyDatabaseReference.child("played_songs");
        proposalsDatabaseReference = ourPartyDatabaseReference.child("proposals");
        usersDatabaseReference = ourPartyDatabaseReference.child("users");
        currentSongDatabaseReference = ourPartyDatabaseReference.child("currentSong");
        imageUrlDatabaseReference = ourPartyDatabaseReference.child("imageUrl");

        playedSongsDatabaseReference.keepSynced(true);

        this.imagesReference = imagesReference;

        proposals = new HashMap<>();
        currentSong=new SongSchema();
        proposalEventListeners = new ArrayList<>();
        nameEventListeners = new ArrayList<>();
        endedEventListeners = new ArrayList<>();
        currentSongEventListeners= new ArrayList<>();

        this.partySchema = partySchema;
        this.key = ourPartyDatabaseReference.getKey();

        proposalsDatabaseReference.addChildEventListener(new ChildEventListener() {
            @Override
            public void onChildAdded(DataSnapshot dataSnapshot, String previousChildName) {
                ProposalSchema proposalSchema = dataSnapshot.getValue(ProposalSchema.class);
                proposals.put(proposalSchema, dataSnapshot.getKey());
                for(SchemaListener<ProposalSchema> proposalSchemaListener : proposalEventListeners)
                    proposalSchemaListener.onItemAdded(proposalSchema);
            }

            @Override
            public void onChildChanged(DataSnapshot dataSnapshot, String previousChildName) {
                ProposalSchema proposalSchema = dataSnapshot.getValue(ProposalSchema.class);
                proposals.put(proposalSchema, dataSnapshot.getKey());
                for(SchemaListener<ProposalSchema> proposalSchemaListener : proposalEventListeners)
                    proposalSchemaListener.onItemChanged(proposalSchema);
            }

            @Override
            public void onChildRemoved(DataSnapshot dataSnapshot) {
                ProposalSchema proposalSchema = dataSnapshot.getValue(ProposalSchema.class);
                proposals.remove(proposalSchema);
                for(SchemaListener<ProposalSchema> proposalSchemaListener : proposalEventListeners)
                    proposalSchemaListener.onItemDeleted(proposalSchema);
            }

            @Override
            public void onChildMoved(DataSnapshot dataSnapshot, String previousChildName) {

            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

            }
        });

        // when current song is changed
        currentSongDatabaseReference.addValueEventListener(new ValueEventListener() {


            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                SongSchema songSchema = dataSnapshot.getValue(SongSchema.class);
                currentSong=songSchema;
                SongSchema storedSong = partySchema.getCurrentSong();

                // we always want the partySchema to reflect what is online
                // so we update it
                partySchema.setCurrentSong(songSchema);

                // but we only alert the listeners, if the change is interesting
                // that means the new song is not null
                // and it is different from the song we had stored so far
                // this check is necessary since firebase likes to generate duplicate messages
                // that actually alert us about the same change twice.
                if(songSchema != null){
                    if(storedSong==null || !currentSong.equals((storedSong))){
                        for(SchemaListener<SongSchema> songSchemaListener : currentSongEventListeners)
                            songSchemaListener.onItemChanged(songSchema);
                    }
                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

            }


        });

        // for changes in the name
        ourPartyDatabaseReference.child("name").addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                String name = dataSnapshot.getValue(String.class);
                if(name!=null) {
                    if (!name.equals(partySchema.getName())) {
                        partySchema.setName(name);
                        for (SchemaListener<String> nameListener : nameEventListeners)
                            nameListener.onItemChanged(name);
                    }
                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

            }
        });

        ourPartyDatabaseReference.child("ended").addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                if(dataSnapshot!=null) {
                    boolean ended = dataSnapshot.getValue(Boolean.class);
                    if (ended != partySchema.getEnded()) {
                        partySchema.setEnded(ended);
                        for (SchemaListener<Boolean> listener : endedEventListeners)
                            listener.onItemChanged(ended);
                    }
                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

            }
        });
    }

    /**
     * Return the partyschema asynchronously
     * @param callback returns Success and the partySchema
     *                 returns NotFound or Failure otherwise
     */
    @Override
    public void getPartySchema(final CompletionListener<Tuple<DBResult, PartySchema>> callback) {
        ourPartyDatabaseReference.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                if(dataSnapshot.exists()){
                    PartySchema partySchema = dataSnapshot.getValue(PartySchema.class);
                    callback.onCompleted(new Tuple<>(DBResult.Success, partySchema));
                }
                else{
                    callback.onCompleted(new Tuple<DBResult, PartySchema>(DBResult.NotFound, null));
                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                callback.onCompleted(new Tuple<DBResult, PartySchema>(DBResult.Failure, null));
            }
        });
    }

    /**
     * @return the firebase key
     */
    public String getKey(){
        return key;
    }

    /**
     * Set the name of the party
     * @param name of the party
     * @param callback returns Success in case the name has correctly been set
     */
    @Override
    public void setName(final String name, final CompletionListener<DBResult> callback) {

        ourPartyDatabaseReference.child("name").setValue(name).addOnCompleteListener(new OnCompleteListener<Void>() {
            @Override
            public void onComplete(@NonNull Task<Void> task) {
                partySchema.setName(name);

                callback.onCompleted(DBResult.Success);
            }
        });
    }

    /**
     * Add a proposal to the party
     * @param proposal the proposal to add
     * @param callback returns Success if the proposal has correctly been added
     *                 or if the proposal was already in the list of proposals
     */
    @Override
    public void addProposal(final ProposalSchema proposal, final CompletionListener<DBResult> callback) {

        if (proposals.containsKey(proposal))
            callback.onCompleted(DBResult.Success);

        final DatabaseReference newProposalReference = proposalsDatabaseReference.push();
        newProposalReference.setValue(proposal).addOnCompleteListener(new OnCompleteListener<Void>() {
            @Override
            public void onComplete(@NonNull Task<Void> task) {
                proposals.put(proposal, newProposalReference.getKey());

                callback.onCompleted(DBResult.Success);
            }
        });
    }

    /**
     * Remove a proposal from the party
     * @param proposal the proposal
     * @param callback returns NotFound in case the proposal doesn't exist,
     */
    @Override
    public void removeProposal(final ProposalSchema proposal, final CompletionListener<DBResult> callback) {

        if (!proposals.containsKey(proposal))
            callback.onCompleted(DBResult.NotFound);

        String key = proposals.get(proposal);
        proposalsDatabaseReference.child(key).removeValue().addOnCompleteListener(new OnCompleteListener<Void>() {
            @Override
            public void onComplete(@NonNull Task<Void> task) {
                proposals.remove(proposal);
            }
        });
    }

    /**
     * Update the currently played song and add it to the played songs of the party
     * @param newSong the new song that is played
     * @param isNewSong to signal if the song should be added to the played songs
     * @param callback returns Success in case the update is correctly executed
     */
    @Override
    public void updateCurrentSong(final SongSchema newSong, final boolean isNewSong, final CompletionListener<DBResult> callback) {

        OnCompleteListener listener= new OnCompleteListener<Void>() {
            int waitingTask= (isNewSong)?2:1;
            @Override
            public void onComplete(@NonNull Task<Void> task) {
                waitingTask--;
                if(waitingTask<1){
                    currentSong=newSong;
                    callback.onCompleted(DBResult.Success);
                }
            }
        };

        if (isNewSong){
            playedSongsDatabaseReference.push().setValue(newSong).addOnCompleteListener(listener);
        }
        currentSongDatabaseReference.setValue(newSong).addOnCompleteListener(listener);
    }

    /**
     * Add the song info (currently played song) listener for the guest
     * @param listener the listener
     */
    @Override
    public void addGuestSongInfoListener(SchemaListener<SongSchema> listener) {
        if(! currentSongEventListeners.contains(listener))
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

        if (!proposals.containsKey(proposal))
            callback.onCompleted(DBResult.NotFound);

        String key = proposals.get(proposal);
        DatabaseReference votersReference = proposalsDatabaseReference.child(key).child("voters");
        DatabaseReference newVoterReference = votersReference.child(FirebaseConnection.getInstance().getUserName());

        newVoterReference.setValue(value).addOnCompleteListener(new OnCompleteListener<Void>() {
            @Override
            public void onComplete(@NonNull Task<Void> task) {
                callback.onCompleted(DBResult.Success);
            }
        });
    }

    /**
     * Remove a vote
     * @param proposal for which we need to remove the vote
     * @param callback returns NotFound in case the proposal doesn't exist,
     *                 returns Success if the vote has correctly been removed
     */
    @Override
    public void removeVote(final ProposalSchema proposal, final CompletionListener<DBResult> callback) {

        if (!proposals.containsKey(proposal))
            callback.onCompleted(DBResult.NotFound);

        String key = proposals.get(proposal);
        DatabaseReference votersReference = proposalsDatabaseReference.child(key).child("voters");
        DatabaseReference newVoterReference = votersReference.child(FirebaseConnection.getInstance().getUserName());

        newVoterReference.removeValue().addOnCompleteListener(new OnCompleteListener<Void>() {
            @Override
            public void onComplete(@NonNull Task<Void> task) {
                callback.onCompleted(DBResult.Success);
            }
        });
    }

    /**
     * End the party
     * @param callback returns success in case the party has correctly been ended, otherwise it returns failure
     */
    @Override
    public void endParty(final CompletionListener<DBResult> callback) {
        ourPartyDatabaseReference.child("ended").setValue(true).addOnCompleteListener(new OnCompleteListener<Void>() {
            @Override
            public void onComplete(@NonNull Task<Void> task) {
                if(task.isSuccessful())
                    callback.onCompleted(DBResult.Success);
                else
                    callback.onCompleted(DBResult.Failure);
            }
        });
    }

    /**
     * Add a listener for the proposals
     * @param listener the listener
     */
    @Override
    public void addProposalListener(SchemaListener<ProposalSchema> listener) {
        this.proposalEventListeners.add(listener);

        // firebase fires the onAdded event for every item when this is added
        for (ProposalSchema proposal : proposals.keySet())
            listener.onItemAdded(proposal);
    }

    /**
     * Remove a listener for the proposals
     * @param listener the listener
     */
    @Override
    public void removeProposalListener(SchemaListener<ProposalSchema> listener) {
        if (this.proposalEventListeners.contains(listener))
            this.proposalEventListeners.remove(listener);
    }

    /**
     * Add a listener for the name of the party
     * @param listener the listener
     */
    @Override
    public void addNameListener(SchemaListener<String> listener) {
        if(! nameEventListeners.contains(listener))
            nameEventListeners.add(listener);
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
        if(!endedEventListeners.contains(listener))
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
     * Upload the photo asynchronously and add the download url to the list of image urls
     * @param file the uri of the file in the device
     * @param callback returns Success if the upload is successful otherwise it returns Failure
     */
    @Override
    public void uploadPhoto(Uri file, final CompletionListener<DBResult> callback){
        StorageMetadata metadata = new StorageMetadata.Builder().build();

        StorageReference imageRef = imagesReference.child(file.getLastPathSegment());
        UploadTask uploadTask = imageRef.putFile(file, metadata);

        // Register observers to listen for when the download is done or if it fails
        uploadTask.addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception exception) {
                callback.onCompleted(DBResult.Failure);
            }
        }).addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() {
            @Override
            public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {
                // taskSnapshot.getMetadata() contains file metadata such as size, content-type, and download URL.
                Uri downloadUrl = taskSnapshot.getDownloadUrl();
                DatabaseReference newImageUrlReference = imageUrlDatabaseReference.push();
                newImageUrlReference.setValue(downloadUrl.toString()).addOnCompleteListener(new OnCompleteListener<Void>() {
                    @Override
                    public void onComplete(@NonNull Task<Void> task) {
                        if (task.isSuccessful()) {
                            callback.onCompleted(DBResult.Success);
                        } else {
                            callback.onCompleted(DBResult.Failure);
                        }
                    }
                });
            }
        });;
    }

    /**
     * Get all the urls of all the photo taken during the party to download them.
     * @param callback returns the list of the urls asynchronously
     */
    @Override
    public void getPhotoUrl(final CompletionListener<List<String>> callback) {
        imageUrlDatabaseReference.addListenerForSingleValueEvent(new ValueEventListener() {
            List<String> urlImageList = new ArrayList<>();
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                if(dataSnapshot.exists()){
                    for(DataSnapshot childSnapshot : dataSnapshot.getChildren()){
                        String url = childSnapshot.getValue(String.class);
                        urlImageList.add(url);
                    }
                }
                callback.onCompleted(urlImageList);
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

            }
        });
    }
}
