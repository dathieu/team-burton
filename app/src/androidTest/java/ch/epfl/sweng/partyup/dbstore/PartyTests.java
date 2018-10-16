package ch.epfl.sweng.partyup.dbstore;

import android.content.Intent;
import android.support.test.rule.ActivityTestRule;
import android.support.test.runner.AndroidJUnit4;
import android.util.Log;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import ch.epfl.sweng.partyup.CreateImageFileActivityForTest;
import ch.epfl.sweng.partyup.containers.Tuple;
import ch.epfl.sweng.partyup.dbstore.listeners.CompletionListener;
import ch.epfl.sweng.partyup.dbstore.listeners.SchemaListener;
import ch.epfl.sweng.partyup.dbstore.schemas.PartySchema;
import ch.epfl.sweng.partyup.dbstore.schemas.ProposalSchema;
import ch.epfl.sweng.partyup.dbstore.schemas.SongSchema;
import ch.epfl.sweng.partyup.dbstore.statemachine.DBResult;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

@RunWith(AndroidJUnit4.class)
abstract public class PartyTests {

    protected final int TIMEOUT = 10000;

    private Intent intent = new Intent();

    protected Connection connection;
    protected Party party;

    @Rule
    public final ActivityTestRule<CreateImageFileActivityForTest> mActivityRule
            = new ActivityTestRule<>(CreateImageFileActivityForTest.class, false, false);

    @Before
    public abstract void init();

    @Test
    public void initWorks() {
        assertTrue(connection != null);
        assertTrue(party != null);
    }

    @Test
    public void canSetName() throws Exception {

        final CountDownLatch waiter = new CountDownLatch(1);

        final String newName = "new name for the party";

        party.addNameListener(new SchemaListener<String>() {
            @Override
            public void onItemAdded(String item) {

            }

            @Override
            public void onItemChanged(String item) {
                assertEquals("name should be new name", item, newName);
                waiter.countDown();
            }

            @Override
            public void onItemDeleted(String item) {

            }
        });

        party.setName(newName, new CompletionListener<DBResult>() {
            @Override
            public void onCompleted(DBResult result) {

            }
        });

        boolean finished_in_time;
        try {
            finished_in_time = waiter.await(TIMEOUT, TimeUnit.MILLISECONDS);
        } catch (InterruptedException ex) {
            throw new AssertionError("we have been interrupted");
        }
        if (!finished_in_time)
            throw new AssertionError("timeout in name setting");
    }

    @Test
    public void canEndParty() throws Exception {

        final CountDownLatch waiter = new CountDownLatch(2);

        party.addEndedListener(new SchemaListener<Boolean>() {
            @Override
            public void onItemAdded(Boolean item) {
            }

            @Override
            public void onItemChanged(Boolean item) {
                assertTrue(item);
                waiter.countDown();
            }

            @Override
            public void onItemDeleted(Boolean item) {
            }
        });

        party.endParty(new CompletionListener<DBResult>() {
            @Override
            public void onCompleted(DBResult result) {
                assertTrue(result == DBResult.Success);
                waiter.countDown();
            }
        });

        boolean finished_in_time;
        try {
            finished_in_time = waiter.await(TIMEOUT, TimeUnit.MILLISECONDS);
        } catch (InterruptedException ex) {
            throw new AssertionError("we have been interrupted");
        }
        if (!finished_in_time)
            throw new AssertionError("timeout in ending of party setting");
    }


    @Test
    public void canUpdateSong() throws Exception {

        final CountDownLatch waiter = new CountDownLatch(2);
        party.addGuestSongInfoListener(new SchemaListener<SongSchema>() {
            @Override
            public void onItemAdded(SongSchema item) {

            }

            @Override
            public void onItemChanged(SongSchema item) {
                waiter.countDown();
                assertEquals("testSong", item.getName());
            }

            @Override
            public void onItemDeleted(SongSchema item) {

            }
        });

        SongSchema newSong = new SongSchema();
        newSong.setName("testSong");

        party.updateCurrentSong(newSong, true, new CompletionListener<DBResult>() {
            @Override
            public void onCompleted(DBResult result) {
                assertTrue(result == DBResult.Success);
                waiter.countDown();
            }
        });

        boolean finished_in_time;
        try {
            finished_in_time = waiter.await(TIMEOUT, TimeUnit.MILLISECONDS);
        } catch (InterruptedException ex) {
            throw new AssertionError("we have been interrupted");
        }
        if (!finished_in_time)
            throw new AssertionError("timeout in ending of party setting");
    }

    @Test
    public void canAddRemoveNameListener() {
        SchemaListener<String> nameListener = new SchemaListener<String>() {
            @Override
            public void onItemAdded(String item) {

            }

            @Override
            public void onItemChanged(String item) {

            }

            @Override
            public void onItemDeleted(String item) {

            }
        };

        party.addNameListener(nameListener);
        party.removeNameListener(nameListener);

        // make sure that nothing crashes if we remove it twice
        party.removeNameListener(nameListener);
    }


    @Test
    public void canAddRemoveCurrentSongListener() {
        SchemaListener<SongSchema> songListener = new SchemaListener<SongSchema>() {
            @Override
            public void onItemAdded(SongSchema item) {

            }

            @Override
            public void onItemChanged(SongSchema item) {

            }

            @Override
            public void onItemDeleted(SongSchema item) {

            }
        };

        party.addGuestSongInfoListener(songListener);
        party.removeGuestSongInfoListener(songListener);

        // make sure that nothing crashes if we remove it twice
        party.removeGuestSongInfoListener(songListener);
    }

    @Test
    public void canAddRemoveEndedListener() {
        SchemaListener<Boolean> endedListener = new SchemaListener<Boolean>() {
            @Override
            public void onItemAdded(Boolean item) {

            }

            @Override
            public void onItemChanged(Boolean item) {

            }

            @Override
            public void onItemDeleted(Boolean item) {

            }
        };

        party.addEndedListener(endedListener);
        party.removeEndedListener(endedListener);

        // make sure that nothing crashes if we remove it twice
        party.removeEndedListener(endedListener);
    }

    @Test
    public void canAddRemoveProposalListener() {
        SchemaListener<ProposalSchema> proposalListener = new SchemaListener<ProposalSchema>() {
            @Override
            public void onItemAdded(ProposalSchema item) {

            }

            @Override
            public void onItemChanged(ProposalSchema item) {

            }

            @Override
            public void onItemDeleted(ProposalSchema item) {

            }
        };

        party.addProposalListener(proposalListener);
        party.removeProposalListener(proposalListener);

        // make sure that nothing crashes if we remove it twice
        party.removeProposalListener(proposalListener);
    }

    @Test
    public void canAddProposal() throws Exception {

        final CountDownLatch waiter = new CountDownLatch(1);

        final ProposalSchema proposal = ProposalSchema.getSample(connection);

        SchemaListener<ProposalSchema> listener = new SchemaListener<ProposalSchema>() {
            @Override
            public void onItemAdded(ProposalSchema item) {
                assertTrue(item.equals(proposal));
                waiter.countDown();
            }

            @Override
            public void onItemChanged(ProposalSchema item) {

            }

            @Override
            public void onItemDeleted(ProposalSchema item) {

            }
        };

        party.addProposalListener(listener);

        party.addProposal(proposal, new CompletionListener<DBResult>() {
            @Override
            public void onCompleted(DBResult result) {

            }
        });


        boolean finished_in_time;
        try {
            finished_in_time = waiter.await(TIMEOUT, TimeUnit.MILLISECONDS);
        } catch (InterruptedException ex) {
            throw new AssertionError("we have been interrupted");
        }
        if (!finished_in_time)
            throw new AssertionError("timeout in name setting");

        party.removeProposalListener(listener);
    }

    @Test
    public void canRemoveProposal() throws Exception {

        final CountDownLatch firstWaiter = new CountDownLatch(1);
        final CountDownLatch secondWaiter = new CountDownLatch(1);

        final ProposalSchema proposal = ProposalSchema.getSample(connection);

        SchemaListener<ProposalSchema> listener = new SchemaListener<ProposalSchema>() {
            @Override
            public void onItemAdded(ProposalSchema item) {
                assertTrue(item.equals(proposal));
                firstWaiter.countDown();
            }

            @Override
            public void onItemChanged(ProposalSchema item) {

            }

            @Override
            public void onItemDeleted(ProposalSchema item) {
                assertTrue(item.equals(proposal));
                secondWaiter.countDown();
            }
        };

        party.addProposalListener(listener);

        party.addProposal(proposal, new CompletionListener<DBResult>() {
            @Override
            public void onCompleted(DBResult result) {

            }
        });


        boolean finished_in_time;
        try {
            finished_in_time = firstWaiter.await(TIMEOUT, TimeUnit.MILLISECONDS);
        } catch (InterruptedException ex) {
            throw new AssertionError("we have been interrupted");
        }
        if (!finished_in_time)
            throw new AssertionError("timeout in name setting");

        party.removeProposal(proposal, new CompletionListener<DBResult>() {
            @Override
            public void onCompleted(DBResult result) {

            }
        });

        try {
            finished_in_time = secondWaiter.await(TIMEOUT, TimeUnit.MILLISECONDS);
        } catch (InterruptedException ex) {
            throw new AssertionError("we have been interrupted");
        }
        if (!finished_in_time)
            throw new AssertionError("timeout in name setting");

        party.removeProposalListener(listener);

    }

    @Test
    public void canVote() throws Exception {

        final CountDownLatch firstWaiter = new CountDownLatch(1);
        final CountDownLatch secondWaiter = new CountDownLatch(1);
        final String voterId = connection.getUserName();

        final ProposalSchema proposal = ProposalSchema.getSample(connection);

        SchemaListener<ProposalSchema> listener = new SchemaListener<ProposalSchema>() {
            @Override
            public void onItemAdded(ProposalSchema item) {
                assertTrue(item.equals(proposal));
                firstWaiter.countDown();
            }

            @Override
            public void onItemChanged(ProposalSchema item) {
                assertTrue(item.getVoters().containsKey(voterId));
                assertTrue(!item.getVoters().get(voterId));
                secondWaiter.countDown();
            }

            @Override
            public void onItemDeleted(ProposalSchema item) {

            }
        };

        party.addProposalListener(listener);

        party.addProposal(proposal, new CompletionListener<DBResult>() {
            @Override
            public void onCompleted(DBResult result) {

            }
        });


        boolean finished_in_time;
        try {
            finished_in_time = firstWaiter.await(TIMEOUT, TimeUnit.MILLISECONDS);
        } catch (InterruptedException ex) {
            throw new AssertionError("we have been interrupted");
        }
        if (!finished_in_time)
            throw new AssertionError("timeout in name setting");

        party.vote(proposal, false, new CompletionListener<DBResult>() {
            @Override
            public void onCompleted(DBResult result) {

            }
        });

        try {
            finished_in_time = secondWaiter.await(TIMEOUT, TimeUnit.MILLISECONDS);
        } catch (InterruptedException ex) {
            throw new AssertionError("we have been interrupted");
        }
        if (!finished_in_time)
            throw new AssertionError("timeout in name setting");

        party.removeProposalListener(listener);
    }

    @Test
    public void canRemoveVote() throws Exception {

        final CountDownLatch firstWaiter = new CountDownLatch(1);
        final CountDownLatch secondWaiter = new CountDownLatch(1);
        final String voterId = connection.getUserName();

        final ProposalSchema proposal = ProposalSchema.getSample(connection);

        SchemaListener<ProposalSchema> listener = new SchemaListener<ProposalSchema>() {
            @Override
            public void onItemAdded(ProposalSchema item) {
                assertTrue(item.equals(proposal));
                firstWaiter.countDown();
            }

            @Override
            public void onItemChanged(ProposalSchema item) {
                assertTrue(!item.getVoters().containsKey(voterId));
                secondWaiter.countDown();
            }

            @Override
            public void onItemDeleted(ProposalSchema item) {

            }
        };

        party.addProposalListener(listener);

        party.addProposal(proposal, new CompletionListener<DBResult>() {
            @Override
            public void onCompleted(DBResult result) {

            }
        });


        boolean finished_in_time;
        try {
            finished_in_time = firstWaiter.await(TIMEOUT, TimeUnit.MILLISECONDS);
        } catch (InterruptedException ex) {
            throw new AssertionError("we have been interrupted");
        }
        if (!finished_in_time)
            throw new AssertionError("timeout in name setting");

        party.removeVote(proposal, new CompletionListener<DBResult>() {
            @Override
            public void onCompleted(DBResult result) {

            }
        });

        try {
            finished_in_time = secondWaiter.await(TIMEOUT, TimeUnit.MILLISECONDS);
        } catch (InterruptedException ex) {
            throw new AssertionError("we have been interrupted");
        }
        if (!finished_in_time)
            throw new AssertionError("timeout in name setting");

        party.removeProposalListener(listener);
    }

    @Test
    public void canUpdateCurrentSong() throws Exception {

        final CountDownLatch waiter1 = new CountDownLatch(1);
        final CountDownLatch waiter2 = new CountDownLatch(1);
        final CountDownLatch waiterPrepare = new CountDownLatch(1);
        final PartySchema partySchema=new PartySchema();

        final SongSchema addSong=new SongSchema("test update name","test update artist name",
                "test update album name","test spot id","test update album url",1010101010);

        Log.d("party key", "canUpdateCurrentSong: "+party.getKey());

        final Tuple<Integer,Integer> size=new Tuple<>(0,3);
        party.getPartySchema(new CompletionListener<Tuple<DBResult, PartySchema>>() {
            @Override
            public void onCompleted(Tuple<DBResult, PartySchema> result) {
                size.object1=size.object2=result.object2.getPlayed_songs().size();
                waiterPrepare.countDown();
            }
        });

        boolean finished_in_time;
        try {
            finished_in_time = waiterPrepare.await(TIMEOUT, TimeUnit.MILLISECONDS);
        } catch (InterruptedException ex) {
            throw new AssertionError("we have been interrupted");
        }
        if (!finished_in_time)
            throw new AssertionError("timeout in preparation of current song test");


        party.updateCurrentSong(addSong,true, new CompletionListener<DBResult>() {
            @Override
            public void onCompleted(DBResult result) {
                assertTrue(result == DBResult.Success);
                waiter1.countDown();
            }
        });

        try {
            finished_in_time = waiter1.await(TIMEOUT, TimeUnit.MILLISECONDS);
        } catch (InterruptedException ex) {
            throw new AssertionError("we have been interrupted");
        }
        if (!finished_in_time)
            throw new AssertionError("timeout in updating of current song");

        party.getPartySchema(new CompletionListener<Tuple<DBResult, PartySchema>>() {
            @Override
            public void onCompleted(Tuple<DBResult, PartySchema> result) {
                assertTrue(result.object1 == DBResult.Success);
                assertTrue(result.object2.getCurrentSong().getSpotify_id().equals(addSong.getSpotify_id()));
                assertTrue(result.object2.getPlayed_songs().size()==(size.object1+1));

                partySchema.setPlayed_songs(result.object2.getPlayed_songs());
                waiter2.countDown();
            }
        });

        try {
            finished_in_time = waiter2.await(TIMEOUT, TimeUnit.MILLISECONDS);
        } catch (InterruptedException ex) {
            throw new AssertionError("we have been interrupted");
        }
        if (!finished_in_time)
            throw new AssertionError("timeout in ending of current song test");



    }

    @Test
    public void canGetSchema() throws Exception {

        // 3 countdowns
        // for setting the name
        // for adding a proposal and voting
        // for ending the party
        final CountDownLatch waiter = new CountDownLatch(3);

        party.setName("updated name", new CompletionListener<DBResult>() {
            @Override
            public void onCompleted(DBResult result) {
                waiter.countDown();
            }
        });

        final ProposalSchema proposalSchema = ProposalSchema.getSample(connection.getUserName());

        party.addProposal(proposalSchema, new CompletionListener<DBResult>() {
            @Override
            public void onCompleted(DBResult result) {
                party.vote(proposalSchema, false, new CompletionListener<DBResult>() {
                    @Override
                    public void onCompleted(DBResult result) {
                        party.vote(proposalSchema, true, new CompletionListener<DBResult>() {
                            @Override
                            public void onCompleted(DBResult result) {
                                waiter.countDown();
                            }
                        });
                    }
                });
            }
        });

        party.endParty(new CompletionListener<DBResult>() {
            @Override
            public void onCompleted(DBResult result) {
                waiter.countDown();
            }
        });

        boolean finished_in_time;
        try {
            finished_in_time = waiter.await(TIMEOUT, TimeUnit.MILLISECONDS);
        } catch (InterruptedException ex) {
            throw new AssertionError("we have been interrupted");
        }
        if (!finished_in_time)
            throw new AssertionError("timeout in updating of party");

        final CountDownLatch waiter2 = new CountDownLatch(1);


        party.getPartySchema(new CompletionListener<Tuple<DBResult, PartySchema>>() {
            @Override
            public void onCompleted(Tuple<DBResult, PartySchema> result) {
                if (result.object1 != DBResult.Success) {
                    throw new AssertionError("unsuccessfull acess");
                }
                PartySchema partySchema = result.object2;

                if (!partySchema.getName().equals("updated name"))
                    throw new AssertionError("name is wrong");

                if (!(partySchema.getEnded()))
                    throw new AssertionError("ended is wrong");

                if (!(partySchema.getProposals().size() == 1))
                    throw new AssertionError("wrong number of proposals");

                Map<String, ProposalSchema> proposals = partySchema.getProposals();
                ProposalSchema proposal = proposals.values().iterator().next();

                if (!proposal.getVoters().keySet().contains(connection.getUserName()))
                    throw new AssertionError("voter not stored in schema");


                if (!proposal.getVoters().get(connection.getUserName()))
                    throw new AssertionError("voting not stored in schema");

                waiter2.countDown();
            }
        });

        boolean finished_in_time2;
        try {
            finished_in_time2 = waiter2.await(TIMEOUT, TimeUnit.MILLISECONDS);
        } catch (InterruptedException ex) {
            throw new AssertionError("we have been interrupted");
        }
        if (!finished_in_time2)
            throw new AssertionError("timeout in checking of schema");

    }

    @Test
    public void canUploadPhotoDownloadPhotoUri() {
        mActivityRule.launchActivity(intent);
        final CountDownLatch waiter = new CountDownLatch(1);
        party.uploadPhoto(mActivityRule.getActivity().photoURI, new CompletionListener<DBResult>() {
            @Override
            public void onCompleted(DBResult result) {
                assertTrue(result == DBResult.Success);
                waiter.countDown();
            }
        });
        boolean finished_in_time;
        try {
            finished_in_time = waiter.await(50000, TimeUnit.MILLISECONDS);
        } catch (InterruptedException ex) {
            throw new AssertionError("we have been interrupted");
        }
        if (!finished_in_time)
            throw new AssertionError("timeout in uploading photo");

        final CountDownLatch waiter2 = new CountDownLatch(1);
        party.getPhotoUrl(new CompletionListener<List<String>>() {
            @Override
            public void onCompleted(List<String> result) {
                assertTrue(result.get(0).contains(mActivityRule.getActivity().photoURI.getLastPathSegment()));
                waiter2.countDown();
            }
        });
        boolean finished_in_time2;
        try {
            finished_in_time2 = waiter2.await(50000, TimeUnit.MILLISECONDS);
        } catch (InterruptedException ex) {
            throw new AssertionError("we have been interrupted");
        }
        if (!finished_in_time2)
            throw new AssertionError("timeout in downloading photo ");
    }

}