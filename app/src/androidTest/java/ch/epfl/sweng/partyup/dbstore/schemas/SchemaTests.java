package ch.epfl.sweng.partyup.dbstore.schemas;

import android.support.test.runner.AndroidJUnit4;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

@RunWith(AndroidJUnit4.class)
public class SchemaTests{

    FirebaseDatabase firebaseDatabase;
    DatabaseReference partiesDatabaseReference;
    int TIMEOUT = 1000000;

    @Before
    public void init(){
        firebaseDatabase = FirebaseDatabase.getInstance();
        partiesDatabaseReference = firebaseDatabase.getReference().child("parties");
    }

    @Test
    public void testPartySchema(){
        final CountDownLatch waiter = new CountDownLatch(1);
        PartySchema partySchema = PartySchema.getSample();

        partiesDatabaseReference.goOnline();
        partiesDatabaseReference.keepSynced(true);

        DatabaseReference newPartyReference = partiesDatabaseReference.push();


        ValueEventListener valueEventListener = new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                if(dataSnapshot.exists()){
                    PartySchema addedSchema = dataSnapshot.getValue(PartySchema.class);

                    // make sure that the fields have been correctly parsed and are not null
                    Map<String, Boolean> users = addedSchema.getUsers();
                    if(users==null){
                        throw new AssertionError("no users could be parsed");
                    }
                    if(users.size()==0){
                        throw new AssertionError("the sample schema should contain a user");
                    }

                    Map<String, ProposalSchema> proposals = addedSchema.getProposals();
                    if(proposals == null){
                        throw new AssertionError("no proposals could be parsed");
                    }
                    if(proposals.size()==0){
                        throw new AssertionError("the sample schema should contain a proposal");
                    }

                    Map<String, SongSchema> songs = addedSchema.getPlayed_songs();
                    if(songs==null){
                        throw new AssertionError("no songs could be parsed");
                    }
                    if(songs.size()==0){
                        throw new AssertionError("the sample schema should contain a song");
                    }

                    waiter.countDown();
                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

            }
        };
        newPartyReference.addValueEventListener(valueEventListener);
        newPartyReference.setValue(partySchema);

        boolean finished_in_time=false;
        try {
            finished_in_time = waiter.await(TIMEOUT, TimeUnit.MILLISECONDS);
        } catch (InterruptedException ex) {
            throw new AssertionError("we have been interrupted");
        }
        if (!finished_in_time)
            throw new AssertionError("timeout in name setting");

        newPartyReference.removeEventListener(valueEventListener);
    }
}