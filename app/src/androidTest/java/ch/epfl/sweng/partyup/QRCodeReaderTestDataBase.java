package ch.epfl.sweng.partyup;

import android.content.res.Resources;
import android.graphics.Bitmap;
import android.support.test.rule.ActivityTestRule;

import com.google.android.gms.vision.Frame;
import com.google.zxing.WriterException;

import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.MockitoAnnotations;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import ch.epfl.sweng.partyup.containers.Tuple;
import ch.epfl.sweng.partyup.dbstore.Connection;
import ch.epfl.sweng.partyup.dbstore.ConnectionProvider;
import ch.epfl.sweng.partyup.dbstore.Party;
import ch.epfl.sweng.partyup.dbstore.listeners.CompletionListener;
import ch.epfl.sweng.partyup.dbstore.statemachine.DBResult;
import ch.epfl.sweng.partyup.dbstore.statemachine.DBState;
import ch.epfl.sweng.partyup.testhelpers.SpotifyAuthAndDBCoHelper;

import static android.support.test.espresso.Espresso.onView;
import static android.support.test.espresso.action.ViewActions.click;
import static android.support.test.espresso.matcher.ViewMatchers.withId;
import static junit.framework.Assert.fail;
import static org.junit.Assert.assertEquals;

public class QRCodeReaderTestDataBase {
    @Rule
    public final ActivityTestRule<QRCodeReaderActivity> mActivityRule = new ActivityTestRule<>(QRCodeReaderActivity.class);

    private QRCodeReaderActivity activity;
    private Resources resource;
    private Connection dbConnection;

    private static final int TIMEOUT = 20000;

    @BeforeClass
    public static void setupDB() {
        SpotifyAuthAndDBCoHelper.DBCo();
    }

    @Before
    public void setup() {
        MockitoAnnotations.initMocks(this);
        activity = mActivityRule.getActivity();
        resource = activity.getResources();
        dbConnection= ConnectionProvider.getConnection();
    }

    @Test
    public void dBStateShouldBeCreatedWhenQrCodeIdDoesntMatchAnyParty() {
        Bitmap bitmap = null;
        try {
            bitmap = HostActivity.generateQRCode("-45_s");
        } catch (WriterException exception) {
            exception.printStackTrace();
        }
        activity.qr_detect.receiveFrame(new Frame.Builder().setBitmap(bitmap).build());
        try {
            Thread.sleep(5000);
        } catch (InterruptedException exception) {
            exception.printStackTrace();
        }
        assertEquals(QRCodeReaderActivity.dBConnection.getState(), DBState.SignedIn);
    }

    @Test
    public void dBStateShouldBeCreatedWhenQrCodeIdIsntAValidID() {
        Bitmap bitmap = null;
        try {
            bitmap = HostActivity.generateQRCode(".:!ù#");
        } catch (WriterException exception) {
            exception.printStackTrace();
        }
        activity.qr_detect.receiveFrame(new Frame.Builder().setBitmap(bitmap).build());
        try {
            Thread.sleep(5000);
        } catch (InterruptedException exception) {
            exception.printStackTrace();
        }
        assertEquals(QRCodeReaderActivity.dBConnection.getState(), DBState.SignedIn);
    }

    @Test
    public void guestActivityShould_startWhenQrCodeIdIsValid() {
        final CountDownLatch waiter = new CountDownLatch(1);
        dbConnection.createParty(new CompletionListener<Tuple<DBResult, Party>>() {
            @Override
            public void onCompleted(Tuple<DBResult, Party> result) {
                waiter.countDown();
                if (result.object1 == DBResult.Success) {
                    try {
                        activity.qr_detect.receiveFrame(new Frame.Builder().setBitmap(HostActivity.generateQRCode(result.object2.getKey())).build());
                    } catch (WriterException exception) {
                        exception.printStackTrace();
                    }
                }
                else {
                    fail();
                }
            }
        });
        try {
            waiter.await(TIMEOUT, TimeUnit.MILLISECONDS);
        } catch (InterruptedException exception) {
            throw new AssertionError("we must not be interrupted");
        }

        try {
            Thread.sleep(5000); //UI and activities reaction
        } catch (InterruptedException exception) {
            exception.printStackTrace();
        }
        onView(withId(R.id.navigation_qrcode)).perform(click());
    }
}
