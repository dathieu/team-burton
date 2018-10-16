package ch.epfl.sweng.partyup.util;

import android.support.test.espresso.UiController;
import android.support.test.espresso.ViewAction;
import android.support.test.espresso.contrib.RecyclerViewActions;
import android.support.test.rule.ActivityTestRule;
import android.support.v4.app.FragmentTransaction;
import android.support.v7.widget.RecyclerView;
import android.view.View;

import org.hamcrest.Matcher;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;

import java.util.HashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import ch.epfl.sweng.partyup.DynamicListAdapter;
import ch.epfl.sweng.partyup.DynamicListFragment;
import ch.epfl.sweng.partyup.R;
import ch.epfl.sweng.partyup.containers.Tuple;
import ch.epfl.sweng.partyup.dbstore.Connection;
import ch.epfl.sweng.partyup.dbstore.ConnectionProvider;
import ch.epfl.sweng.partyup.dbstore.Party;
import ch.epfl.sweng.partyup.dbstore.listeners.CompletionListener;
import ch.epfl.sweng.partyup.dbstore.schemas.ProposalSchema;
import ch.epfl.sweng.partyup.dbstore.statemachine.DBResult;
import ch.epfl.sweng.partyup.dbstore.statemachine.DBState;
import ch.epfl.sweng.partyup.eventlisteners.EventListener;
import ch.epfl.sweng.partyup.eventlisteners.ProposalAddListener;
import ch.epfl.sweng.partyup.testhelpers.RecyclerViewMatcher;

import static android.support.test.espresso.Espresso.onView;
import static android.support.test.espresso.assertion.ViewAssertions.matches;
import static android.support.test.espresso.matcher.ViewMatchers.hasDescendant;
import static android.support.test.espresso.matcher.ViewMatchers.withId;
import static android.support.test.espresso.matcher.ViewMatchers.withText;
import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertNotNull;
import static junit.framework.Assert.assertTrue;
import static junit.framework.Assert.fail;
import static org.mockito.Mockito.mock;

//@RunWith(AndroidJUnit4.class)
public class PlaylistTest {

    @Rule
    public final ActivityTestRule<DynamicListFragmentTest> mActivityTestRule =
            new ActivityTestRule<>(DynamicListFragmentTest.class);

    private DynamicListFragment dynamicListFragment = null;
    private Connection dbConnection = null;
    private Party party;
    private DynamicListAdapter dynamicListAdapter = null;

    private static final int TIMEOUT = 30000;

    @BeforeClass
    public static void setupDB() {
        ConnectionProvider.setMode(ConnectionProvider.Mode.RUNTIME);
        Connection connection = ConnectionProvider.getConnection();

        if (connection.getState() != DBState.SignedIn) {
            final CountDownLatch waiter = new CountDownLatch(1);
            connection.signIn(new CompletionListener<DBResult>() {
                @Override
                public void onCompleted(DBResult result) {
                    waiter.countDown();
                    if (result != DBResult.Success) {
                        fail();
                    }
                }
            });
            try {
                waiter.await(TIMEOUT, TimeUnit.MILLISECONDS);
            } catch (InterruptedException exception) {
                throw new AssertionError("we must not be interrupted");
            }
        }
    }

    @Before
    public void init() {

        dbConnection = ConnectionProvider.getConnection();
        if (dbConnection.getState() != DBState.SignedIn) {
            dbConnection.signIn(new CompletionListener<DBResult>() {
                @Override
                public void onCompleted(DBResult result) {
                    if (result != DBResult.Success) {
                        fail();
                    }
                }
            });
        }

        final FragmentTransaction fragmentTransaction = mActivityTestRule.getActivity().getSupportFragmentManager().beginTransaction();

        // wait for the instance to be actually created
        final CountDownLatch onCreateViewWaiter = new CountDownLatch(1);
        final EventListener fragmentCreateListener = new EventListener() {
            @Override
            public void onCreated() {
                onCreateViewWaiter.countDown();
            }

            @Override
            public void onUpdated() {

            }
        };


        final CountDownLatch onCreatePartyWaiter = new CountDownLatch(1);
        dbConnection.createParty(new CompletionListener<Tuple<DBResult, Party>>() {
            @Override
            public void onCompleted(Tuple<DBResult, Party> result) {
                onCreatePartyWaiter.countDown();
                switch (result.object1) {
                    case Success:
                        party = result.object2;
                        dynamicListFragment = DynamicListFragment.newInstance(party);
                        dynamicListFragment.addUIEventListener(fragmentCreateListener);
                        fragmentTransaction.replace(R.id.content, dynamicListFragment).commit();
                        break;
                    default:
                        fail();
                        break;
                }
            }
        });

        boolean finished_in_time=false;
        try {
            finished_in_time = onCreatePartyWaiter.await(TIMEOUT, TimeUnit.MILLISECONDS);
        } catch (InterruptedException exception) {
            throw new AssertionError("we must not be interrupted");
        }
        if (!finished_in_time)
            throw new AssertionError("timeout in init create party");

        try {
            finished_in_time = onCreateViewWaiter.await(TIMEOUT, TimeUnit.MILLISECONDS);
        } catch (InterruptedException exception) {
            throw new AssertionError("we must not be interrupted");
        }
        if (!finished_in_time)
            throw new AssertionError("timeout in init create fragment");

        dynamicListAdapter = dynamicListFragment.dynamicListAdapter;
    }
    @AfterClass
    public static void tearDownDB() {
        Connection connection = ConnectionProvider.getConnection();
        connection.signOut();
    }

    @Test
    public void initWorks() {

        // check whether these references have been created correctly
        assertNotNull(dynamicListFragment);

        // have we been correctly registered as listener for the proposal events ?
        // the fragment needs to inspect its parent at creation
        // and dynamically set up this communication channel
        assertTrue(dynamicListAdapter.getProposalAddListeners().contains(mActivityTestRule.getActivity()));
    }

    @Test
    public void addRemoveUIListeners() {
        EventListener eventListener = new EventListener() {
            @Override
            public void onCreated() {

            }

            @Override
            public void onUpdated() {

            }
        };

        dynamicListFragment.addUIEventListener(eventListener);
        dynamicListFragment.removeUIEventListener(eventListener);

        dynamicListAdapter.addEventListener(eventListener);
        dynamicListAdapter.removeEventListener(eventListener);
    }

    @Test
    public void canDisplayOneItem() {
        final CountDownLatch updateLatch = new CountDownLatch(1);

        dynamicListAdapter.addEventListener(new EventListener() {
            @Override
            public void onCreated() {

            }

            @Override
            public void onUpdated() {
                updateLatch.countDown();
            }
        });


        ProposalSchema firstProposal = new ProposalSchema();
        firstProposal.setSong_id("s");
        firstProposal.setArtist_name("df");
        firstProposal.setSong_name("sd");
        HashMap<String, Boolean> voters = new HashMap<>();
        voters.put(dbConnection.getUserName(), true);

        firstProposal.setVoters(voters);

        // add a new song, this will be shown in the recyclerView
        //Task addFirstProposal = dbConnection.getProposalsDatabaseReference().push().setValue(firstProposal);
        party.addProposal(firstProposal, new CompletionListener<DBResult>() {
            @Override
            public void onCompleted(DBResult result) {
            }
        });

        // now wait for the UI to be updated 2 times
        boolean finished_in_time=false;
        try {
            finished_in_time = updateLatch.await(TIMEOUT, TimeUnit.MILLISECONDS);
        } catch (InterruptedException exception) {
            throw new AssertionError("we must not be interrupted");
        }
        if (!finished_in_time)
            throw new AssertionError("timeout in canDisplayOneItem");

        // check that we really have this item
        RecyclerView recyclerView = dynamicListFragment.dynamicListView;
        RecyclerView.Adapter adapter = recyclerView.getAdapter();
        assertEquals(adapter.getItemCount(), 1);
    }

    @Test
    public void canDisplayTwoItems() {

        final CountDownLatch updateLatch = new CountDownLatch(2);

        dynamicListAdapter.addEventListener(new EventListener() {
            @Override
            public void onCreated() {

            }

            @Override
            public void onUpdated() {
                updateLatch.countDown();
            }
        });


        ProposalSchema firstProposal = new ProposalSchema();
        firstProposal.setSong_id("s");
        firstProposal.setArtist_name("df");
        firstProposal.setSong_name("sd");
        HashMap<String, Boolean> voters = new HashMap<>();
        voters.put(dbConnection.getUserName(), true);

        firstProposal.setVoters(voters);

        ProposalSchema secondProposal = new ProposalSchema();
        secondProposal.setSong_id("s2");
        secondProposal.setArtist_name("df");
        secondProposal.setSong_name("second song name");
        HashMap<String, Boolean> secondVoters = new HashMap<>();
        secondVoters.put(dbConnection.getUserName(), true);
        secondVoters.put("dfpoiu", true);

        secondProposal.setVoters(secondVoters);

        final CountDownLatch waiter = new CountDownLatch(2);

        party.addProposal(firstProposal, new CompletionListener<DBResult>() {
            @Override
            public void onCompleted(DBResult result) {
                waiter.countDown();
            }
        });
        party.addProposal(secondProposal, new CompletionListener<DBResult>() {
            @Override
            public void onCompleted(DBResult result) {
                waiter.countDown();
            }
        });

        // now wait for the UI to be updated 2 times
        boolean finished_in_time=false;
        try {
            finished_in_time = updateLatch.await(TIMEOUT, TimeUnit.MILLISECONDS);
        } catch (InterruptedException exception) {
            throw new AssertionError("we must not be interrupted");
        }
        if (!finished_in_time)
            throw new AssertionError("timeout in canDisplayTwoItem");

        // check that we really have this item
        RecyclerView recyclerView = dynamicListFragment.dynamicListView;
        RecyclerView.Adapter adapter = recyclerView.getAdapter();

        onView((new RecyclerViewMatcher(recyclerView.getId())).atPosition(0))
                .check(matches(hasDescendant(withText("second song name"))));
    }

    @Test
    public void canTapVote() {

        // this is for the initial loading of the two items
        final CountDownLatch twoInteractions = new CountDownLatch(2);
        // this is for the reordering after tapping the screen
        final CountDownLatch threeInteractions = new CountDownLatch(3);

        dynamicListAdapter.addEventListener(new EventListener() {
            @Override
            public void onCreated() {

            }

            @Override
            public void onUpdated() {
                twoInteractions.countDown();
                threeInteractions.countDown();
            }
        });


        ProposalSchema firstProposal = new ProposalSchema();
        firstProposal.setSong_id("s");
        firstProposal.setArtist_name("df");
        firstProposal.setSong_name("first song name");
        HashMap<String, Boolean> voters = new HashMap<>();
        voters.put("sdfg", true);

        firstProposal.setVoters(voters);

        ProposalSchema secondProposal = new ProposalSchema();
        secondProposal.setSong_id("s");
        secondProposal.setArtist_name("df");
        secondProposal.setSong_name("second song name");
        HashMap<String, Boolean> secondVoters = new HashMap<>();
        secondVoters.put("sdfg", true);

        secondProposal.setVoters(secondVoters);


        final CountDownLatch waiter = new CountDownLatch(2);

        party.addProposal(firstProposal, new CompletionListener<DBResult>() {
            @Override
            public void onCompleted(DBResult result) {
                waiter.countDown();
            }
        });
        party.addProposal(secondProposal, new CompletionListener<DBResult>() {
            @Override
            public void onCompleted(DBResult result) {
                waiter.countDown();
            }
        });

        // now wait for the UI to be updated 2 times
        boolean finished_in_time=false;
        try {
            finished_in_time = twoInteractions.await(TIMEOUT, TimeUnit.MILLISECONDS);
        } catch (InterruptedException exception) {
            throw new AssertionError("we must not be interrupted");
        }
        if (!finished_in_time)
            throw new AssertionError("timeout in canTapVote");

        // check that we really have this item
        RecyclerView recyclerView = dynamicListFragment.dynamicListView;
        RecyclerView.Adapter adapter = recyclerView.getAdapter();

        onView((new RecyclerViewMatcher(recyclerView.getId())).atPosition(0))
                .check(matches(hasDescendant(withText("first song name"))));

        ViewAction myViewAction = new ViewAction() {
            @Override
            public Matcher<View> getConstraints() {
                return null;
            }

            @Override
            public String getDescription() {
                return null;
            }

            @Override
            public void perform(UiController uiController, View view) {
                View button = view.findViewById(R.id.dislikeButton);
                button.performClick();
            }
        };

        onView(withId(recyclerView.getId()))
                .perform(RecyclerViewActions.actionOnItemAtPosition(0, myViewAction));

        try {
            threeInteractions.await(TIMEOUT, TimeUnit.MILLISECONDS);
        } catch (InterruptedException exception) {
            throw new AssertionError("we must not be interrupted");
        }

        try {
            Thread.sleep(1000);
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        onView((new RecyclerViewMatcher(recyclerView.getId())).atPosition(0))
                .check(matches(hasDescendant(withText("second song name"))));
    }

    @Test
    public void canTapVoteTwice() {

        // this is for the initial loading of the two items
        final CountDownLatch firstInteraction = new CountDownLatch(2);
        // this is after tapping the screen
        final CountDownLatch secondInteraction = new CountDownLatch(3);

        dynamicListAdapter.addEventListener(new EventListener() {
            @Override
            public void onCreated() {

            }

            @Override
            public void onUpdated() {
                firstInteraction.countDown();
                secondInteraction.countDown();
            }
        });


        ProposalSchema firstProposal = new ProposalSchema();
        firstProposal.setSong_id("s");
        firstProposal.setArtist_name("df");
        firstProposal.setSong_name("first song name");
        HashMap<String, Boolean> voters = new HashMap<>();
        voters.put("sdfg", true);

        firstProposal.setVoters(voters);

        ProposalSchema secondProposal = new ProposalSchema();
        secondProposal.setSong_id("s");
        secondProposal.setArtist_name("df");
        secondProposal.setSong_name("second song name");
        HashMap<String, Boolean> secondVoters = new HashMap<>();
        secondVoters.put("sdfg", true);

        secondProposal.setVoters(secondVoters);

        final CountDownLatch waiter = new CountDownLatch(2);

        party.addProposal(firstProposal, new CompletionListener<DBResult>() {
            @Override
            public void onCompleted(DBResult result) {
                waiter.countDown();
            }
        });
        party.addProposal(secondProposal, new CompletionListener<DBResult>() {
            @Override
            public void onCompleted(DBResult result) {
                waiter.countDown();
            }
        });

        // now wait for the UI to be updated 2 times
        boolean finished_in_time=false;
        try {
            finished_in_time = firstInteraction.await(TIMEOUT, TimeUnit.MILLISECONDS);
        } catch (InterruptedException exception) {
            throw new AssertionError("we must not be interrupted");
        }
        if (!finished_in_time)
            throw new AssertionError("timeout in canTapVoteTwice");

        // check that we really have this item
        RecyclerView recyclerView = dynamicListFragment.dynamicListView;
        RecyclerView.Adapter adapter = recyclerView.getAdapter();

        onView((new RecyclerViewMatcher(recyclerView.getId())).atPosition(0))
                .check(matches(hasDescendant(withText("first song name"))));

        ViewAction myViewAction = new ViewAction() {
            @Override
            public Matcher<View> getConstraints() {
                return null;
            }

            @Override
            public String getDescription() {
                return null;
            }

            @Override
            public void perform(UiController uiController, View view) {
                View button = view.findViewById(R.id.likeButton);
                button.performClick();
            }
        };

        //  cycle the items
        onView(withId(recyclerView.getId()))
                .perform(RecyclerViewActions.actionOnItemAtPosition(1, myViewAction));


        onView(withId(recyclerView.getId()))
                .perform(RecyclerViewActions.actionOnItemAtPosition(0, myViewAction));


        onView(withId(recyclerView.getId()))
                .perform(RecyclerViewActions.actionOnItemAtPosition(1, myViewAction));

        try {
            secondInteraction.await(TIMEOUT, TimeUnit.MILLISECONDS);
        } catch (InterruptedException exception) {
            throw new AssertionError("we must not be interrupted");
        }


        onView((new RecyclerViewMatcher(recyclerView.getId())).atPosition(0))
                .check(matches(hasDescendant(withText("first song name"))));
    }

    @Test
    public void canDelete() {

        // this is for the initial loading of the item
        final CountDownLatch oneInteraction = new CountDownLatch(1);
        // this is for the deletion of the item after tapping the screen
        // updating the voters is one interaction, deleting is another one
        final CountDownLatch threeInteractions = new CountDownLatch(3);

        dynamicListAdapter.addEventListener(new EventListener() {
            @Override
            public void onCreated() {

            }

            @Override
            public void onUpdated() {
                oneInteraction.countDown();
                threeInteractions.countDown();
            }
        });


        ProposalSchema firstProposal = new ProposalSchema();
        firstProposal.setSong_id("s");
        firstProposal.setArtist_name("df");
        firstProposal.setSong_name("first song name");
        HashMap<String, Boolean> voters = new HashMap<>();
        voters.put(dbConnection.getUserName(), true);

        firstProposal.setVoters(voters);


        final CountDownLatch waiter = new CountDownLatch(1);

        party.addProposal(firstProposal, new CompletionListener<DBResult>() {
            @Override
            public void onCompleted(DBResult result) {
                waiter.countDown();
            }
        });

        // now wait for the UI to be updated 2 times
        try {
            oneInteraction.await(TIMEOUT, TimeUnit.MILLISECONDS);
        } catch (InterruptedException exception) {
            throw new AssertionError("we must not be interrupted");
        }

        // check that we really have this item
        RecyclerView recyclerView = dynamicListFragment.dynamicListView;
        RecyclerView.Adapter adapter = recyclerView.getAdapter();

        onView((new RecyclerViewMatcher(recyclerView.getId())).atPosition(0))
                .check(matches(hasDescendant(withText("first song name"))));

        ViewAction myViewAction = new ViewAction() {
            @Override
            public Matcher<View> getConstraints() {
                return null;
            }

            @Override
            public String getDescription() {
                return null;
            }

            @Override
            public void perform(UiController uiController, View view) {
                View button = view.findViewById(R.id.likeButton);
                button.performClick();
            }
        };

        onView(withId(recyclerView.getId()))
                .perform(RecyclerViewActions.actionOnItemAtPosition(0, myViewAction));

        try {
            threeInteractions.await(TIMEOUT, TimeUnit.MILLISECONDS);
        } catch (InterruptedException exception) {
            throw new AssertionError("we must not be interrupted");
        }

        // now the item should have been deleted
        assertEquals(0, adapter.getItemCount());
    }

    @Test
    public void deletedIfUnpopular() {


        // this is for the initial loading of the item
        final CountDownLatch oneInteraction = new CountDownLatch(1);
        // this is for the deletion of the item after tapping the screen
        // updating the voters is one interaction, deleting is another one
        final CountDownLatch threeInteractions = new CountDownLatch(3);

        dynamicListAdapter.addEventListener(new EventListener() {
            @Override
            public void onCreated() {

            }

            @Override
            public void onUpdated() {
                oneInteraction.countDown();
                threeInteractions.countDown();
            }
        });


        ProposalSchema firstProposal = new ProposalSchema();
        firstProposal.setSong_id("s");
        firstProposal.setArtist_name("df");
        firstProposal.setSong_name("first song name");
        HashMap<String, Boolean> voters = new HashMap<>();

        //Set the votes so that one more dislike would remove the proposal
        for(int i = 0; i < -DynamicListAdapter.MINIMUM_SCORE - 1; ++i){
            voters.put(dbConnection.getUserName(), false);
        }

        firstProposal.setVoters(voters);

        final CountDownLatch waiter = new CountDownLatch(1);

        party.addProposal(firstProposal, new CompletionListener<DBResult>() {
            @Override
            public void onCompleted(DBResult result) {
                waiter.countDown();
            }
        });

        // now wait for the UI to be updated 2 times
        try {
            oneInteraction.await(TIMEOUT, TimeUnit.MILLISECONDS);
        } catch (InterruptedException exception) {
            throw new AssertionError("we must not be interrupted");
        }

        // check that we really have this item
        RecyclerView recyclerView = dynamicListFragment.dynamicListView;
        RecyclerView.Adapter adapter = recyclerView.getAdapter();

        onView((new RecyclerViewMatcher(recyclerView.getId())).atPosition(0))
                .check(matches(hasDescendant(withText("first song name"))));

        ViewAction myViewAction = new ViewAction() {
            @Override
            public Matcher<View> getConstraints() {
                return null;
            }

            @Override
            public String getDescription() {
                return null;
            }

            @Override
            public void perform(UiController uiController, View view) {
                View button = view.findViewById(R.id.dislikeButton);
                button.performClick();
            }
        };

        onView(withId(recyclerView.getId()))
                .perform(RecyclerViewActions.actionOnItemAtPosition(0, myViewAction));

        try {
            threeInteractions.await(TIMEOUT, TimeUnit.MILLISECONDS);
        } catch (InterruptedException exception) {
            throw new AssertionError("we must not be interrupted");
        }

        // now the item should have been deleted
        assertEquals(0, adapter.getItemCount());
    }

    @Test
    public void canVeto() {

        // this is for the initial loading of the item
        final CountDownLatch oneInteraction = new CountDownLatch(1);
        // this is for the deletion of the item after tapping the screen
        // updating the voters is one interaction, deleting is another one
        final CountDownLatch threeInteractions = new CountDownLatch(3);

        dynamicListAdapter.addEventListener(new EventListener() {
            @Override
            public void onCreated() {

            }

            @Override
            public void onUpdated() {
                oneInteraction.countDown();
                threeInteractions.countDown();
            }
        });


        ProposalSchema firstProposal = new ProposalSchema();
        firstProposal.setSong_id("s");
        firstProposal.setArtist_name("df");
        firstProposal.setSong_name("first song name");
        HashMap<String, Boolean> voters = new HashMap<>();
        voters.put(dbConnection.getUserName(), true);

        firstProposal.setVoters(voters);


        final CountDownLatch waiter = new CountDownLatch(1);

        party.addProposal(firstProposal, new CompletionListener<DBResult>() {
            @Override
            public void onCompleted(DBResult result) {
                waiter.countDown();
            }
        });

        // now wait for the UI to be updated 2 times
        try {
            oneInteraction.await(TIMEOUT, TimeUnit.MILLISECONDS);
        } catch (InterruptedException exception) {
            throw new AssertionError("we must not be interrupted");
        }

        // check that we really have this item
        RecyclerView recyclerView = dynamicListFragment.dynamicListView;
        RecyclerView.Adapter adapter = recyclerView.getAdapter();

        onView((new RecyclerViewMatcher(recyclerView.getId())).atPosition(0))
                .check(matches(hasDescendant(withText("first song name"))));

        ViewAction myViewAction = new ViewAction() {
            @Override
            public Matcher<View> getConstraints() {
                return null;
            }

            @Override
            public String getDescription() {
                return null;
            }

            @Override
            public void perform(UiController uiController, View view) {
                View button = view.findViewById(R.id.dislikeButton);
                button.performClick();
            }
        };

        onView(withId(recyclerView.getId()))
                .perform(RecyclerViewActions.actionOnItemAtPosition(0, myViewAction));

        try {
            threeInteractions.await(TIMEOUT, TimeUnit.MILLISECONDS);
        } catch (InterruptedException exception) {
            throw new AssertionError("we must not be interrupted");
        }

        // the item is still there
        assertEquals(adapter.getItemCount(), 1);

        // but now the vote count should be -1
        try {
            Thread.sleep(1000);
        } catch (Exception ex) {
            ex.printStackTrace();
        }

        onView((new RecyclerViewMatcher(recyclerView.getId())).atPosition(0))
                .check(matches(hasDescendant(withText("-1"))));
    }

    @Test
    public void canAddRemoveProposalListeners() {
        // mock the addProposalListener
        ProposalAddListener proposalAddListener = mock(ProposalAddListener.class);
        dynamicListAdapter.addProposalAddListener(proposalAddListener);
        dynamicListAdapter.removeProposalAddListener(proposalAddListener);
    }

    @Test
    public void canPropose() {

        // mock the addProposalListener
        ProposalAddListener proposalAddListener = mock(ProposalAddListener.class);
        dynamicListAdapter.addProposalAddListener(proposalAddListener);

        // this is for the initial loading of the item
        final CountDownLatch oneInteraction = new CountDownLatch(1);

        dynamicListAdapter.addEventListener(new EventListener() {
            @Override
            public void onCreated() {

            }

            @Override
            public void onUpdated() {
                oneInteraction.countDown();
            }
        });


        ProposalSchema firstProposal = new ProposalSchema();
        firstProposal.setSong_id("s");
        firstProposal.setArtist_name("df");
        firstProposal.setSong_name("first song name");
        HashMap<String, Boolean> voters = new HashMap<>();
        voters.put(dbConnection.getUserName(), true);

        firstProposal.setVoters(voters);

        party.addProposal(firstProposal, new CompletionListener<DBResult>() {
            @Override
            public void onCompleted(DBResult result) {
            }
        });

        // now wait for the UI to be updated
        try {
            oneInteraction.await(TIMEOUT, TimeUnit.MILLISECONDS);
        } catch (InterruptedException exception) {
            throw new AssertionError("we must not be interrupted");
        }

        // check that we really have this item
        RecyclerView recyclerView = dynamicListFragment.dynamicListView;
        RecyclerView.Adapter adapter = recyclerView.getAdapter();

        onView((new RecyclerViewMatcher(recyclerView.getId())).atPosition(0))
                .check(matches(hasDescendant(withText("first song name"))));

        ViewAction myViewAction = new ViewAction() {
            @Override
            public Matcher<View> getConstraints() {
                return null;
            }

            @Override
            public String getDescription() {
                return null;
            }

            @Override
            public void perform(UiController uiController, View view) {
                //View v = view.findViewById(R.id.addProposalButton);
                // v.performClick();
            }
        };

        onView(withId(recyclerView.getId()))
                .perform(RecyclerViewActions.actionOnItemAtPosition(0, myViewAction));

        // now we should have received the event
        //verify(proposalAddListener, timeout(TIMEOUT)).addProposal(firstProposal);
    }
}