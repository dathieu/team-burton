package ch.epfl.sweng.partyup;

import android.content.Intent;
import android.support.test.rule.ActivityTestRule;
import android.support.test.runner.AndroidJUnit4;
import android.support.test.uiautomator.UiDevice;
import android.view.KeyEvent;

import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import ch.epfl.sweng.partyup.testhelpers.SpotifyAuthAndDBCoHelper;

import static android.support.test.InstrumentationRegistry.getInstrumentation;
import static android.support.test.espresso.Espresso.onView;
import static android.support.test.espresso.action.ViewActions.click;
import static android.support.test.espresso.action.ViewActions.pressKey;
import static android.support.test.espresso.action.ViewActions.typeText;
import static android.support.test.espresso.assertion.ViewAssertions.matches;
import static android.support.test.espresso.matcher.ViewMatchers.isDisplayed;
import static android.support.test.espresso.matcher.ViewMatchers.withId;
import static android.support.test.espresso.matcher.ViewMatchers.withText;
import static ch.epfl.sweng.partyup.testhelpers.SpotifyAuthAndDBCoHelper.withIndex;
import static org.junit.Assert.assertEquals;


@RunWith(AndroidJUnit4.class)
public class GuestActivityTest {

    private static final int timeout = 30;
    private Intent intent = new Intent();

    @Rule
    public final ActivityTestRule<GuestActivity> mActivityRule
            = new ActivityTestRule<>(GuestActivity.class, false, false);

    @BeforeClass
    public static void dbSetup() {
        SpotifyAuthAndDBCoHelper.GuestDBCO();
    }

    //patry created we can join: -KxRy6e_2gI1kyh_4SpM
    @Before
    public void setup() {
        mActivityRule.launchActivity(intent);
    }


    @Test
    public void show_qr_fragment_when_tab_qr_is_clicked() {
        //to be sure the qr should not be display first
        onView(withId(R.id.navigation_playlist)).perform(click());
        //and then we display it
        onView(withId(R.id.navigation_qrcode)).perform(click());

        onView(withId(R.id.qr_view)).check(matches(isDisplayed()));

    }

    @Test
    public void show_playlist_fragment_when_tab_playlist_is_clicked() {
        //to be sure the list should not be display first
        onView(withId(R.id.navigation_qrcode)).perform(click());
        //and then we display it
        onView(withId(R.id.navigation_playlist)).perform(click());

        onView(withId(R.id.frameLayout)).check(matches(isDisplayed()));

    }

    public void pleaseWait() {
        try {
            Thread.sleep(5000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    @Test
    public void canSearchAsGuest() {
        SpotAuth.logOut();
        pleaseWait();
        try {
            SpotifyAuthAndDBCoHelper.startActivity();
        } catch (Exception e) {
            e.printStackTrace();
        }

        onView(withId(R.id.searchButton)).perform(click());
        pleaseWait();
        onView(withId(R.id.search_field)).perform(typeText("Apache"));
        onView(withId(R.id.search_field)).perform(pressKey(KeyEvent.KEYCODE_ENTER));
        pleaseWait();

        onView(withIndex(withId(R.id.addSongButton), 0)).perform(click());
        UiDevice mDevice = UiDevice.getInstance(getInstrumentation());
        mDevice.pressBack();
        onView(withId(R.id.titleTextView)).check(matches(withText("Apache")));
    }

    @Test
    public void canLeaveParty() {
        onView(withId(R.id.settingButton)).perform(click());
        onView(withText(R.string.leave_party)).perform(click());
        pleaseWait();
        onView(withId(R.id.HostButton)).check(matches(isDisplayed()));
        SpotifyAuthAndDBCoHelper.GuestDBCO();
    }

    @Test
    public void additionIsCorrect() {
        assertEquals(2, 1 + 1);
    }
}
