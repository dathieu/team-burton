package ch.epfl.sweng.partyup;


import android.content.Intent;
import android.support.test.rule.ActivityTestRule;

import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;

import ch.epfl.sweng.partyup.dbstore.Connection;
import ch.epfl.sweng.partyup.dbstore.ConnectionProvider;
import ch.epfl.sweng.partyup.dbstore.statemachine.DBState;

import static android.support.test.espresso.Espresso.onView;
import static android.support.test.espresso.assertion.ViewAssertions.matches;
import static android.support.test.espresso.matcher.ViewMatchers.isEnabled;
import static android.support.test.espresso.matcher.ViewMatchers.withId;

public class NoConnectionTest
{
    private static Intent intent = new Intent();

    @ClassRule
    public static final ActivityTestRule<WelcomeScreen> mActivityRule =
            new ActivityTestRule<>(WelcomeScreen.class, true, false);


    @BeforeClass
    public static void authentication() {
        SpotAuth.logOut();
        ConnectionProvider.setMode(ConnectionProvider.Mode.TEST);
        final Connection connection = ConnectionProvider.getConnection();
        if(connection.getState() == DBState.SignedIn){
            connection.signOut();
        }

    }

    @Test
    public void shouldNotBeAbleToClickOnButton(){
        //This is used to run through the welcomeScreen onCreate. (the if branch where we are not signedIn.
       mActivityRule.launchActivity(intent);
        //Now we should be signedIn, let's check that the buttons are enabled:
        onView(withId(R.id.HostButton)).check(matches(isEnabled()));
        onView(withId(R.id.GuestButton)).check(matches(isEnabled()));
        onView(withId(R.id.MemoriesButton)).check(matches(isEnabled()));
    }
}
