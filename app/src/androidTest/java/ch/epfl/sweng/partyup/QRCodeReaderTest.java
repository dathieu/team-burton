package ch.epfl.sweng.partyup;

import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.support.test.rule.ActivityTestRule;

import com.google.android.gms.vision.Frame;
import com.google.zxing.WriterException;

import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.MockitoAnnotations;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import ch.epfl.sweng.partyup.dbstore.Connection;
import ch.epfl.sweng.partyup.dbstore.ConnectionProvider;
import ch.epfl.sweng.partyup.dbstore.listeners.CompletionListener;
import ch.epfl.sweng.partyup.dbstore.statemachine.DBResult;
import ch.epfl.sweng.partyup.dbstore.statemachine.DBState;

import static android.support.test.espresso.Espresso.onView;
import static android.support.test.espresso.assertion.ViewAssertions.matches;
import static android.support.test.espresso.matcher.ViewMatchers.withId;
import static android.support.test.espresso.matcher.ViewMatchers.withText;
import static junit.framework.Assert.fail;

public class QRCodeReaderTest {
    @Rule
    public final ActivityTestRule<QRCodeReaderActivity> mActivityRule = new ActivityTestRule<>(QRCodeReaderActivity.class);

    private QRCodeReaderActivity activity;
   private Resources resource;
    private static String partyKey = null;
    private static final int TIMEOUT = 20000;

    @BeforeClass
    public static void setupDB() {
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

    @AfterClass
    public static void tearDownDB() {
        Connection connection = ConnectionProvider.getConnection();
        connection.signOut();
    }

    @Before
    public void setup() {
        MockitoAnnotations.initMocks(this);
        activity = mActivityRule.getActivity();
        resource = activity.getResources();
    }

    @Test
    public void decodeqr_test_qr_0() {
        activity.qr_detect.receiveFrame(new Frame.Builder().setBitmap(BitmapFactory.decodeResource(resource, R.mipmap.test_qr_0)).build());
        onView(withId(R.id.decod_text)).check(matches(withText("test qr")));
    }

    @Test
    public void decodeqr_test_qr_90() {
        activity.qr_detect.receiveFrame(new Frame.Builder().setBitmap(BitmapFactory.decodeResource(resource, R.mipmap.test_qr_90)).build());
        onView(withId(R.id.decod_text)).check(matches(withText("test qr")));
    }

    @Test
    public void decodeqr_test_qr_180() {
        activity.qr_detect.receiveFrame(new Frame.Builder().setBitmap(BitmapFactory.decodeResource(resource, R.mipmap.test_qr_180)).build());
        onView(withId(R.id.decod_text)).check(matches(withText("test qr")));
    }

    @Test
    public void decodeqr_test_qr_270() {
        activity.qr_detect.receiveFrame(new Frame.Builder().setBitmap(BitmapFactory.decodeResource(resource, R.mipmap.test_qr_270)).build());
        onView(withId(R.id.decod_text)).check(matches(withText("test qr")));
    }

    @Test
    public void decodeGeneratedQrCodeWithHostActivity() {
        Bitmap bitmap = null;
        try {
            bitmap = HostActivity.generateQRCode(partyKey);
        } catch (WriterException exception) {
            exception.printStackTrace();
        }
        activity.qr_detect.receiveFrame(new Frame.Builder().setBitmap(bitmap).build());
        onView(withId(R.id.decod_text)).check(matches(withText(partyKey)));
    }

    @Test
    public void decodeGeneratedQrCodeWithGuestActivity() {
        Bitmap bitmap = null;
        try {
            bitmap = GuestActivity.generateQRCode(partyKey);
        } catch (WriterException exception) {
            exception.printStackTrace();
        }
        activity.qr_detect.receiveFrame(new Frame.Builder().setBitmap(bitmap).build());
        onView(withId(R.id.decod_text)).check(matches(withText(partyKey)));
    }
}
