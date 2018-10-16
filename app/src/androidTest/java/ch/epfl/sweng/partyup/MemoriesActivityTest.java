package ch.epfl.sweng.partyup;

import android.app.Activity;
import android.content.Intent;
import android.support.test.rule.ActivityTestRule;
import android.support.test.runner.AndroidJUnit4;
import android.support.v7.widget.RecyclerView;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import ch.epfl.sweng.partyup.dbstore.ConnectionProvider;
import ch.epfl.sweng.partyup.dbstore.MockedConnection;
import ch.epfl.sweng.partyup.dbstore.listeners.CompletionListener;
import ch.epfl.sweng.partyup.dbstore.statemachine.DBResult;
import ch.epfl.sweng.partyup.dbstore.statemachine.DBState;
import ch.epfl.sweng.partyup.testhelpers.SpotifyAuthAndDBCoHelper;

import static android.support.test.espresso.Espresso.onView;
import static android.support.test.espresso.action.ViewActions.click;
import static android.support.test.espresso.matcher.ViewMatchers.withId;
import static ch.epfl.sweng.partyup.testhelpers.TestHelper.first;
import static ch.epfl.sweng.partyup.testhelpers.TestHelper.getActivityInstance;
import static junit.framework.Assert.assertTrue;
import static junit.framework.Assert.fail;

@RunWith(AndroidJUnit4.class)
public class MemoriesActivityTest {
    private static final String DEFAULT_ID = "orne_8-1.0.1";
    private static final int TIMEOUT = 20000;

    @ClassRule
    public final static ActivityTestRule<MemoriesActivity> mActivityRule
            = new ActivityTestRule<>(MemoriesActivity.class, false, false);
    private static Intent intent = new Intent();

    @BeforeClass
    public static void setupDB() {
        SpotAuth.logOut();
        ConnectionProvider.setMode(ConnectionProvider.Mode.TEST);
        MockedConnection connection = (MockedConnection) ConnectionProvider.getConnection();

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

        connection.mockMemoriesActivity();

        mActivityRule.launchActivity(intent);
        try {
            SpotifyAuthAndDBCoHelper.startActivity();
        } catch (Exception e) {
            e.printStackTrace();
        }
        MemoriesActivity.setSpotifyUserId(DEFAULT_ID);
        SpotAuth.setAuthenticated(true);
    }

    @AfterClass
    public static void resetDB() {
        MockedConnection connection = (MockedConnection) ConnectionProvider.getConnection();
        connection.resetMockingState();


        ConnectionProvider.setMode(ConnectionProvider.Mode.RUNTIME);
    }

    @Test
    public void partiesAreDisplayed() {
        mActivityRule.launchActivity(intent);
        MemoriesActivity activity = mActivityRule.getActivity();

        pleaseWait();

        RecyclerView memoriesRecyclerView = activity.getMemoriesRecyclerView();
        RecyclerView.Adapter adapter = memoriesRecyclerView.getAdapter();
        assertTrue(adapter.getItemCount() > 0);

        onView(first(withId(R.id.party_holder_layout))).perform(click());
        pleaseWait();
        Activity currentActivity = getActivityInstance();
        assertTrue(currentActivity.getClass().isAssignableFrom(MemoryActivity.class));
    }

    @Test
    public void galleryDisplayed() {
        mActivityRule.launchActivity(intent);
        MemoriesActivity activity = mActivityRule.getActivity();

        pleaseWait();

        RecyclerView memoriesRecyclerView = activity.getMemoriesRecyclerView();
        RecyclerView.Adapter adapter = memoriesRecyclerView.getAdapter();
        assertTrue(adapter.getItemCount() > 0);

        onView(first(withId(R.id.party_holder_layout))).perform(click());
        pleaseWait();
        onView(withId(R.id.memory_navigation_photos)).perform(click());
        //Should be nice to have photos/songs in the mocked parties in order to click on them and be able to check if they are displayed
    }

    private void pleaseWait() {
        try {
            Thread.sleep(5000       );
        } catch (InterruptedException ex) {
            ex.printStackTrace();
            fail();
        }
    }

}
