package ch.epfl.sweng.partyup.testhelpers;


import android.content.res.Resources;
import android.support.test.InstrumentationRegistry;
import android.support.test.espresso.Espresso;
import android.support.test.espresso.ViewAction;
import android.support.test.espresso.action.CoordinatesProvider;
import android.support.test.espresso.action.GeneralClickAction;
import android.support.test.espresso.action.Press;
import android.support.test.espresso.action.Tap;
import android.support.test.uiautomator.UiDevice;
import android.support.test.uiautomator.UiObject;
import android.support.test.uiautomator.UiSelector;
import android.util.Log;
import android.view.View;

import org.hamcrest.Description;
import org.hamcrest.Matcher;
import org.hamcrest.TypeSafeMatcher;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import ch.epfl.sweng.partyup.SharedAndSavedData;
import ch.epfl.sweng.partyup.containers.Tuple;
import ch.epfl.sweng.partyup.dbstore.Connection;
import ch.epfl.sweng.partyup.dbstore.ConnectionProvider;
import ch.epfl.sweng.partyup.dbstore.Party;
import ch.epfl.sweng.partyup.dbstore.listeners.CompletionListener;
import ch.epfl.sweng.partyup.dbstore.statemachine.DBResult;
import ch.epfl.sweng.partyup.dbstore.statemachine.DBState;

import static android.support.test.espresso.Espresso.onView;
import static android.support.test.espresso.matcher.ViewMatchers.withResourceName;
import static junit.framework.Assert.fail;

public class SpotifyAuthAndDBCoHelper {
    private static final int POPUP_WAIT = 10000;
    private static final int TIMEOUT = 5000;


    public static ViewAction clickXY(final int coordonneX, final int coordonneY) {
        return new GeneralClickAction(
                Tap.SINGLE,
                new CoordinatesProvider() {
                    @Override
                    public float[] calculateCoordinates(View view) {

                        final int[] screenPos = new int[2];
                        view.getLocationOnScreen(screenPos);

                        final float screenX = screenPos[0] + coordonneX;
                        final float screenY = screenPos[1] + coordonneY;
                        float[] coordinates = {screenX, screenY};
                        Log.e("coordinates", Float.toString(screenX) + ":" + Float.toString(screenY));
                        return coordinates;
                    }
                },
                Press.FINGER);
    }


    private static int getScreenWidth() {
        return Resources.getSystem().getDisplayMetrics().widthPixels;
    }


    public static void startActivity() throws Exception {

        float xOnFullHD = 540;
        float yOnFullHD = 630;

        float webViewXOnFullHD = 48;
        float webViewYOnFullHD = 114;

        int xOn320 = 160;
        int yOn480 = 230;

        int webViewX320 = 11;
        int webViewY480 = 33;


        int relativeXLogin = (int) (xOnFullHD - webViewXOnFullHD);
        int relativeYLogin = (int) (yOnFullHD - webViewYOnFullHD);


        if (getScreenWidth() == 320) {
            relativeXLogin = xOn320 - webViewX320;
            relativeYLogin = yOn480 - webViewY480;

        }
        UiDevice mDevice;
        mDevice = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation());
        try {
            Thread.sleep(POPUP_WAIT);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }


        onView(withResourceName("com_spotify_sdk_login_webview")).perform(clickXY(relativeXLogin, relativeYLogin));

        UiObject usernameField = mDevice.findObject(new UiSelector()
                .text("Username or email address").className("android.widget.EditText"));
        UiObject passwordField = mDevice.findObject(new UiSelector()
                .className("android.widget.EditText").instance(1));

        usernameField.waitForExists(10000);
        passwordField.waitForExists(10000);

        UiObject loginButton = mDevice.findObject(new UiSelector().className("android.widget.Button").instance(1));
        usernameField.setText("zwillenguru");
        //"orne_8-1.0.1"
        if (passwordField.exists()) {
            passwordField.setText("sweng2017");
            //c4nB3hardC0d3d
        }
        Espresso.closeSoftKeyboard();
        if (loginButton.exists()) {
            loginButton.click();
        }

    }

    public static void DBCo(){

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
                waiter.await(TIMEOUT, TimeUnit.MILLISECONDS);
            } catch (InterruptedException exception) {
                throw new AssertionError("we must not be interrupted");
            }
        }
    }

    public static void GuestDBCO(){
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
                waiter.await(TIMEOUT, TimeUnit.MILLISECONDS);
            } catch (InterruptedException exception) {
                throw new AssertionError("we must not be interrupted");
            }
        }
        connection.createParty(new CompletionListener<Tuple<DBResult, Party>>() {
            @Override
            public void onCompleted(Tuple<DBResult, Party> result) {
                if (result.object1 == DBResult.Success) {
                    SharedAndSavedData.connectedParty = result.object2;
                    SharedAndSavedData.connectedPartyID=result.object2.getKey();
                    Waiter.countDown();
                }
            }
        });

        try {
            Waiter.await(TIMEOUT, TimeUnit.MILLISECONDS);
        } catch (InterruptedException exception) {
            throw new AssertionError("we must not be interrupted");
        }

    }
    public static Matcher<View> withIndex(final Matcher<View> matcher, final int index) {
        return new TypeSafeMatcher<View>() {
            int currentIndex = 0;

            @Override
            public void describeTo(Description description) {
                description.appendText("with index: ");
                description.appendValue(index);
                matcher.describeTo(description);
            }

            @Override
            public boolean matchesSafely(View view) {
                return matcher.matches(view) && currentIndex++ == index;
            }
        };
    }
}
