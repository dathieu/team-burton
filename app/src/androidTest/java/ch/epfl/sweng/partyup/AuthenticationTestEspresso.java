package ch.epfl.sweng.partyup;

import android.app.Activity;
import android.content.Intent;
import android.support.test.rule.ActivityTestRule;

import com.spotify.sdk.android.authentication.AuthenticationResponse;

import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;

import ch.epfl.sweng.partyup.testhelpers.SpotifyAuthAndDBCoHelper;

import static android.support.test.espresso.Espresso.onView;
import static android.support.test.espresso.action.ViewActions.click;
import static android.support.test.espresso.action.ViewActions.typeText;
import static android.support.test.espresso.assertion.ViewAssertions.matches;
import static android.support.test.espresso.matcher.ViewMatchers.isDisplayed;
import static android.support.test.espresso.matcher.ViewMatchers.withId;
import static android.support.test.espresso.matcher.ViewMatchers.withText;
import static junit.framework.Assert.fail;
import static org.junit.Assert.assertEquals;

public class AuthenticationTestEspresso {

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
    public void AuthenticationLeadsToHostScreen() {
        mActivityRule.launchActivity(intent);
        assertEquals(mActivityRule.getActivity().getLocalClassName(), "HostActivity");
    }




    @Test(expected = IllegalArgumentException.class)
    public void RequestTokenExceptionOnEmptyScopes() {
        String[] scopes = new String[]{};
        Intent intent = new Intent();
        Activity host = mActivityRule.launchActivity(intent);
        SpotAuth.spotRequest(host, scopes, AuthenticationResponse.Type.TOKEN);
    }

    @Test(expected = IllegalArgumentException.class)
    public void RequestTokenExceptionOnNullActivity() {
        String[] scopes = new String[]{"streaming"};
        SpotAuth.spotRequest(null, scopes, AuthenticationResponse.Type.TOKEN);
    }

    @Test
    public void canSetNameOfParty() {
        mActivityRule.launchActivity(intent);

        try {
            Thread.sleep(5000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        onView(withId(R.id.settingButton)).perform(click());
        onView(withText(R.string.set_name)).perform(click());
        onView(withId(R.id.partyNameEdit)).perform(typeText("party test"));
        onView(withText(R.string.set_name)).perform(click());
        onView(withId(R.id.partyName)).check(matches(withText("party test")));
    }

    @Test
    public void canEndPartyAsHost() {
        mActivityRule.launchActivity(intent);

        try {
            Thread.sleep(5000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        onView(withId(R.id.settingButton)).perform(click());
        onView(withText(R.string.end_party)).perform(click());
        onView(withText(R.string.end_party)).perform(click());
        try {
            Thread.sleep(5000); //waiting for the activity to change
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        onView(withId(R.id.HostButton)).check(matches(isDisplayed()));
    }


}


