package ch.epfl.sweng.partyup;


import android.support.test.espresso.Espresso;
import android.support.test.runner.AndroidJUnit4;

import com.github.jksiezni.permissive.testing.PermissiveTestRule;

import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import static android.support.test.espresso.assertion.ViewAssertions.matches;
import static android.support.test.espresso.matcher.ViewMatchers.withId;
import static android.support.test.espresso.matcher.ViewMatchers.withText;

@RunWith(AndroidJUnit4.class)
public class QRCodeReaderActivityTestPermissionDenied {

    @Rule
    public final PermissiveTestRule<QRCodeReaderActivity> mActivityRule =
            new PermissiveTestRule<>(QRCodeReaderActivity.class).deniedAll();

    @Test
    public void init_no_permission() {
        Espresso.onView(withId(R.id.decod_text)).check(matches(withText("permission to use the camera not granted")));
    }

}


