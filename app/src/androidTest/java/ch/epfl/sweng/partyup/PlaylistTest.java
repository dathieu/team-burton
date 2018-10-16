package ch.epfl.sweng.partyup;


import android.content.Intent;
import android.support.test.rule.ActivityTestRule;
import android.support.test.uiautomator.UiDevice;
import android.view.KeyEvent;
import android.view.View;

import org.hamcrest.Description;
import org.hamcrest.Matcher;
import org.hamcrest.TypeSafeMatcher;
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

public class PlaylistTest {

    private static Intent intent = new Intent();

    @ClassRule
    public static final ActivityTestRule<HostActivity> mActivityRule =
            new ActivityTestRule<>(HostActivity.class, true, false);


    @BeforeClass
    public static void authentication() {
        SpotAuth.logOut();
        SpotifyAuthAndDBCoHelper.DBCo();
        mActivityRule.launchActivity(intent);
        try {
            SpotifyAuthAndDBCoHelper.startActivity();
        } catch (Exception e) {
            fail();
        }

    }

    private void pleaseWait() {
        try {
            Thread.sleep(5000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    @Test
    public void clickingPlaylistButtonTest() {
        mActivityRule.launchActivity(intent);

        pleaseWait();
        onView(withId(R.id.searchButton)).perform(click());
        pleaseWait();
        onView(withId(R.id.playlistButton)).perform(click());
        pleaseWait();
        onView(withId(R.id.recyclerPlaylist)).perform(click());
    }

    @Test
    public void canResearchSongs() {
        mActivityRule.launchActivity(intent);

        pleaseWait();

        onView(withId(R.id.searchButton)).perform(click());
        onView(withId(R.id.search_field)).perform(typeText("Apache"));
        onView(withId(R.id.search_field)).perform(pressKey(KeyEvent.KEYCODE_ENTER));
        pleaseWait();

        onView(withIndex(withId(R.id.addSongButton), 0)).perform(click());

        UiDevice mDevice = UiDevice.getInstance(getInstrumentation());
        mDevice.pressBack();
        onView(withId(R.id.titleTextView)).check(matches(withText("Apache")));

        onView(withId(R.id.settingButton)).perform(click());
        onView(withText(R.string.end_party)).perform(click());
        onView(withText(R.string.end_party)).perform(click());
    }
}
