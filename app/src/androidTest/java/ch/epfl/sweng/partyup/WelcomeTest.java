package ch.epfl.sweng.partyup;


import android.content.Intent;
import android.support.test.InstrumentationRegistry;
import android.support.test.uiautomator.UiDevice;

import com.github.jksiezni.permissive.testing.PermissiveTestRule;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import ch.epfl.sweng.partyup.dbstore.Connection;
import ch.epfl.sweng.partyup.dbstore.ConnectionProvider;
import ch.epfl.sweng.partyup.dbstore.MockedConnection;
import ch.epfl.sweng.partyup.dbstore.listeners.CompletionListener;
import ch.epfl.sweng.partyup.dbstore.statemachine.DBResult;
import ch.epfl.sweng.partyup.dbstore.statemachine.DBState;

import static android.support.test.espresso.Espresso.onView;
import static android.support.test.espresso.action.ViewActions.click;
import static android.support.test.espresso.assertion.ViewAssertions.matches;
import static android.support.test.espresso.matcher.ViewMatchers.isDisplayed;
import static android.support.test.espresso.matcher.ViewMatchers.withId;
import static junit.framework.Assert.fail;

public class WelcomeTest {
    private static Intent intent = new Intent();
    private static String partyKey;
    private static int TIMEOUT = 20000;
    @ClassRule
    public static final PermissiveTestRule<WelcomeScreen> mActivityRule =
            new PermissiveTestRule<>(WelcomeScreen.class).grantedAll();

    @BeforeClass
    public static void setupDB() {
        SpotAuth.logOut();
        ConnectionProvider.setMode(ConnectionProvider.Mode.TEST);
        final Connection connection = ConnectionProvider.getConnection();
        partyKey = "something that can't possibly occur in the database";
        if (connection.getState() != DBState.SignedIn) {
            final CountDownLatch waiter = new CountDownLatch(1);
            connection.signIn(new CompletionListener<DBResult>() {
                @Override
                public void onCompleted(DBResult result) {

                    if (result != DBResult.Success)
                        fail();

                    waiter.countDown();
                }
            });
            try {
                waiter.await(TIMEOUT, TimeUnit.MILLISECONDS);
            } catch (InterruptedException exception) {
                throw new AssertionError("we must not be interrupted");
            }
        }
    }

    @Test
    public void clickOnHost() {
        mActivityRule.launchActivity(intent);
        pleaseWait();
        onView(withId(R.id.HostButton)).perform(click());
    }

    @Test
    public void clickOnGuest() {
        mActivityRule.launchActivity(intent);
        pleaseWait();
        onView(withId(R.id.GuestButton)).perform(click());
    }

    @Test
    public void memoryClick() {
        mActivityRule.launchActivity(intent);
        pleaseWait();
        onView(withId(R.id.MemoriesButton)).perform(click());
    }

    @Test
    public void pressingBackshouldGoBackToWelcomeScreen() {
        mActivityRule.launchActivity(intent);
        pleaseWait();
        onView(withId(R.id.HostButton)).perform(click());
        pleaseWait();
        UiDevice mDevice;
        mDevice = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation());
        mDevice.pressBack();
        pleaseWait();
        onView(withId(R.id.HostButton)).check(matches(isDisplayed()));

    }
    @AfterClass
    public static void resetDB(){
        MockedConnection connection = (MockedConnection) ConnectionProvider.getConnection();
        connection.resetMockingState();


        ConnectionProvider.setMode(ConnectionProvider.Mode.RUNTIME);
    }


    private void pleaseWait() {
        try {
            Thread.sleep(3000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
}
