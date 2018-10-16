package ch.epfl.sweng.partyup;

import android.content.Intent;
import android.support.test.rule.ActivityTestRule;
import android.support.test.uiautomator.UiDevice;
import android.view.KeyEvent;

import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;

import ch.epfl.sweng.partyup.testhelpers.SpotifyAuthAndDBCoHelper;

import static android.support.test.InstrumentationRegistry.getInstrumentation;
import static android.support.test.espresso.Espresso.onView;
import static android.support.test.espresso.action.ViewActions.click;
import static android.support.test.espresso.action.ViewActions.pressKey;
import static android.support.test.espresso.action.ViewActions.typeText;
import static android.support.test.espresso.assertion.ViewAssertions.matches;
import static android.support.test.espresso.matcher.ViewMatchers.withId;
import static android.support.test.espresso.matcher.ViewMatchers.withText;
import static ch.epfl.sweng.partyup.testhelpers.SpotifyAuthAndDBCoHelper.withIndex;
import static junit.framework.Assert.fail;
import static org.junit.Assert.assertTrue;

public class PlayerTest {

    private static Intent intent = new Intent();

    @ClassRule
    public static final ActivityTestRule<HostActivity> mActivityRule =
            new ActivityTestRule<>(HostActivity.class, true, false);

    @BeforeClass
    public static void setupDB() {
        SpotAuth.logOut();
        SpotifyAuthAndDBCoHelper.DBCo();
        mActivityRule.launchActivity(intent);
        try {
            SpotifyAuthAndDBCoHelper.startActivity();
        } catch (Exception e) {
            fail();
        }
    }

    @Test
    public void playerWorks() {
        mActivityRule.launchActivity(intent);
        pleaseWait();
        onView(withId(R.id.searchButton)).perform(click());
        onView(withId(R.id.search_field)).perform(typeText("Apache"));
        onView(withId(R.id.search_field)).perform(pressKey(KeyEvent.KEYCODE_ENTER));
        pleaseWait();
        onView(withIndex(withId(R.id.addSongButton), 0)).perform(click());
        onView(withIndex(withId(R.id.addSongButton), 1)).perform(click());


        UiDevice mDevice = UiDevice.getInstance(getInstrumentation());
        mDevice.pressBack();

        onView(withId(R.id.playPause)).perform(click());
        assertTrue(mActivityRule.getActivity().getPlayer().isPlaying());
        onView(withId(R.id.playPause)).perform(click());
        assertTrue(!mActivityRule.getActivity().getPlayer().isPlaying());
        onView(withId(R.id.playPause)).perform(click());
        assertTrue(mActivityRule.getActivity().getPlayer().isPlaying());
        onView(withId(R.id.nextButton)).perform(click());
        onView(withId(R.id.song_name_player)).check(matches(withText("Apache")));
    }

    @Test
    public void canSaveTrackToSpotify() {
        mActivityRule.launchActivity(intent);
        pleaseWait();
        onView(withId(R.id.searchButton)).perform(click());
        onView(withId(R.id.search_field)).perform(typeText("Apache"));
        onView(withId(R.id.search_field)).perform(pressKey(KeyEvent.KEYCODE_ENTER));
        pleaseWait();
        onView(withIndex(withId(R.id.addSongButton), 0)).perform(click());
        UiDevice mDevice = UiDevice.getInstance(getInstrumentation());
        mDevice.pressBack();
        onView(withId(R.id.playPause)).perform(click());

        onView(withId(R.id.SaveButon)).perform(click());
    }

    @Test
    public void canSkipPartOfTheSong() {
        mActivityRule.launchActivity(intent);
        pleaseWait();

        onView(withId(R.id.searchButton)).perform(click());
        onView(withId(R.id.search_field)).perform(typeText("Apache"));
        onView(withId(R.id.search_field)).perform(pressKey(KeyEvent.KEYCODE_ENTER));
        pleaseWait();
        onView(withIndex(withId(R.id.addSongButton), 0)).perform(click());
        UiDevice mDevice = UiDevice.getInstance(getInstrumentation());
        mDevice.pressBack();
        onView(withId(R.id.playPause)).perform(click());

        onView(withId(R.id.track_progress_bar)).perform(click());

    }

    private void pleaseWait() {
        try {
            Thread.sleep(3000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
}

