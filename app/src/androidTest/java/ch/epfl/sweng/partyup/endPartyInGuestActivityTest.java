package ch.epfl.sweng.partyup;

import android.content.Intent;
import android.graphics.Bitmap;
import android.support.test.rule.ActivityTestRule;
import android.support.test.runner.AndroidJUnit4;
import android.util.Log;

import com.google.android.gms.vision.Frame;
import com.google.zxing.WriterException;

import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import ch.epfl.sweng.partyup.containers.Tuple;
import ch.epfl.sweng.partyup.dbstore.Connection;
import ch.epfl.sweng.partyup.dbstore.ConnectionProvider;
import ch.epfl.sweng.partyup.dbstore.Party;
import ch.epfl.sweng.partyup.dbstore.listeners.CompletionListener;
import ch.epfl.sweng.partyup.dbstore.statemachine.DBResult;
import ch.epfl.sweng.partyup.dbstore.statemachine.DBState;

import static android.support.test.espresso.Espresso.onView;
import static android.support.test.espresso.assertion.ViewAssertions.matches;
import static android.support.test.espresso.matcher.ViewMatchers.isDisplayed;
import static android.support.test.espresso.matcher.ViewMatchers.withId;
import static junit.framework.Assert.fail;


@RunWith(AndroidJUnit4.class)
public class endPartyInGuestActivityTest {

    private static final int timeout = 30;

    @ClassRule
    public static final ActivityTestRule<QRCodeReaderActivity> mQrReaderRule
            = new ActivityTestRule<>(QRCodeReaderActivity.class);

    @Rule
    public final ActivityTestRule<GuestActivity> mActivityRule
            = new ActivityTestRule<>(GuestActivity.class, false, false);

    @BeforeClass
    public static void dbSetup() {
        final CountDownLatch Waiter = new CountDownLatch(1);

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
                waiter.await(timeout, TimeUnit.SECONDS);
            } catch (InterruptedException exception) {
                throw new AssertionError("we must not be interrupted");
            }
        }
        connection.createParty(new CompletionListener<Tuple<DBResult, Party>>() {
            @Override
            public void onCompleted(Tuple<DBResult, Party> result) {
                if (result.object1 == DBResult.Success) {
                    SharedAndSavedData.connectedParty = result.object2;
                    SharedAndSavedData.connectedPartyID=SharedAndSavedData.connectedParty.getKey();
                    try {
                        Bitmap bitmap = null;
                        bitmap = GuestActivity.generateQRCode(result.object2.getKey());
                        mQrReaderRule.getActivity().qr_detect.receiveFrame(new Frame.Builder().setBitmap(bitmap).build());
                    } catch (WriterException exception) {
                        exception.printStackTrace();
                    }
                    Waiter.countDown();
                }
            }
        });

        try {
            Waiter.await(timeout, TimeUnit.SECONDS);
        } catch (InterruptedException exception) {
            throw new AssertionError("we must not be interrupted");
        }
    }

    //patry created we can join: -KxRy6e_2gI1kyh_4SpM

    @Test
    public void canEndPartyAsGuest()
    {
        mActivityRule.launchActivity(new Intent());

        onView(withId(R.id.bottom_navigation)).check(matches(isDisplayed()));
        final CountDownLatch Waiter = new CountDownLatch(1);
        SharedAndSavedData.connectedParty.endParty(new CompletionListener<DBResult>() {
            @Override
            public void onCompleted(DBResult result) {
                if (result == DBResult.Success) {
                    Waiter.countDown();
                }
            }
        });
        Log.d("test", "canEndPartyAsGuest: waiter");
        try {
            Waiter.await(timeout, TimeUnit.SECONDS);//waiting the party to have finished
        } catch (InterruptedException exception) {
            throw new AssertionError("we must not be interrupted");
        }

        Log.d("test", "canEndPartyAsGuest: sleep");
        try {
            Thread.sleep(5000); //waiting for the activity to change
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        Log.d("test", "canEndPartyAsGuest: match");
        onView(withId(R.id.GuestButton)).check(matches(isDisplayed()));
    }

}
