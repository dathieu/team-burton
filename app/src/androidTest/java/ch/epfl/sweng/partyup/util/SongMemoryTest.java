package ch.epfl.sweng.partyup.util;

import android.support.test.espresso.UiController;
import android.support.test.espresso.ViewAction;
import android.support.test.espresso.contrib.RecyclerViewActions;
import android.support.test.rule.ActivityTestRule;
import android.support.v4.app.FragmentTransaction;
import android.support.v7.widget.RecyclerView;
import android.view.View;

import org.hamcrest.Matcher;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import java.util.HashMap;
import java.util.Random;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import ch.epfl.sweng.partyup.R;
import ch.epfl.sweng.partyup.SongListAdapter;
import ch.epfl.sweng.partyup.SongListFragment;
import ch.epfl.sweng.partyup.dbstore.Tools;
import ch.epfl.sweng.partyup.dbstore.schemas.PartySchema;
import ch.epfl.sweng.partyup.dbstore.schemas.SongSchema;
import ch.epfl.sweng.partyup.eventlisteners.EventListener;
import ch.epfl.sweng.partyup.testhelpers.RecyclerViewMatcher;

import static android.support.test.espresso.Espresso.onView;
import static android.support.test.espresso.assertion.ViewAssertions.matches;
import static android.support.test.espresso.matcher.ViewMatchers.hasDescendant;
import static android.support.test.espresso.matcher.ViewMatchers.withId;
import static android.support.test.espresso.matcher.ViewMatchers.withText;
import static junit.framework.Assert.assertNotNull;
import static junit.framework.Assert.assertTrue;

//@RunWith(AndroidJUnit4.class)
public class SongMemoryTest {

    @Rule
    public final ActivityTestRule<DynamicListFragmentTest> mActivityTestRule =
            new ActivityTestRule<>(DynamicListFragmentTest.class);

    private SongListFragment songListFragment = null;
    private PartySchema partySchema = null;
    private SongListAdapter songListAdapter = null;

    private static final int TIMEOUT = 30000;
    private int itemCount = 10;


    @Before
    public void init() {


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

        partySchema = new PartySchema();
        partySchema.setName("name");
        partySchema.setEnded(false);

        Random random = new Random();

        HashMap<String, SongSchema> playedSongs = new HashMap<>();

        for (int i = 0; i < itemCount; i++) {
            SongSchema songSchema = new SongSchema();
            songSchema.setAlbum_name(Tools.generateKey(5));
            songSchema.setAlbum_url(Tools.generateKey(5));
            songSchema.setArtist_name(Tools.generateKey(5));
            songSchema.setName("song nr. " + i);
            songSchema.setSpotify_id(Tools.generateKey(5));
            songSchema.setTimestamp(i);

            playedSongs.put(Tools.generateKey(15), songSchema);
        }

        partySchema.setPlayed_songs(playedSongs);


        songListFragment = SongListFragment.newInstance(partySchema, mActivityTestRule.getActivity());
        songListFragment.addUIEventListener(fragmentCreateListener);
        fragmentTransaction.replace(R.id.content, songListFragment).commit();

        boolean finished_in_time = false;
        try {
            finished_in_time = onCreateViewWaiter.await(TIMEOUT, TimeUnit.MILLISECONDS);
        } catch (InterruptedException exception) {
            throw new AssertionError("we must not be interrupted");
        }
        if (!finished_in_time)
            throw new AssertionError("timeout in init create fragment");

        songListAdapter = songListFragment.dynamicListAdapter;
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

        songListFragment.addUIEventListener(eventListener);
        songListFragment.removeUIEventListener(eventListener);
    }

    @Test
    public void initWorks() {

        // check whether these references have been created correctly
        assertNotNull(songListFragment);
    }

    @Test
    public void canDisplayOneItem() {

        // check that we really have this item
        RecyclerView recyclerView = songListFragment.dynamicListView;
        RecyclerView.Adapter adapter = recyclerView.getAdapter();
        assertTrue(adapter.getItemCount() > 0);
    }


    @Test
    public void canSort() {

        // check that we really have this item
        RecyclerView recyclerView = songListFragment.dynamicListView;

        onView((new RecyclerViewMatcher(recyclerView.getId())).atPosition(0))
                .check(matches(hasDescendant(withText("song nr. 0"))));
    }

    @Test
    public void canTapAddToSpotify() {

        // check that we really have this item
        RecyclerView recyclerView = songListFragment.dynamicListView;

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
                View button = view.findViewById(R.id.addToSpotifyButton);
                button.performClick();
            }
        };

        onView(withId(recyclerView.getId()))
                .perform(RecyclerViewActions.actionOnItemAtPosition(0, myViewAction));
    }
}