package ch.epfl.sweng.partyup;


import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;

import com.github.jksiezni.permissive.testing.PermissiveTestRule;

import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;

import ch.epfl.sweng.partyup.testhelpers.SpotifyAuthAndDBCoHelper;

import static ch.epfl.sweng.partyup.UserActivity.isExternalStorageWritable;
import static ch.epfl.sweng.partyup.UserActivity.rotateImage;
import static junit.framework.Assert.fail;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertTrue;

public class CameraTest {
    private static Intent intent = new Intent();
    @ClassRule
    public static final PermissiveTestRule<HostActivity> mActivityRule =
            new PermissiveTestRule<>(HostActivity.class).grantedAll();

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
    public void rotateImageWorksFor360Angle(){
        Bitmap bitmap = BitmapFactory.decodeResource(mActivityRule.getActivity().getResources(),R.mipmap.test_photo);
        Bitmap rotatedBitmap = rotateImage(bitmap, 360);
        assertEquals(bitmap, rotatedBitmap);
    }

    @Test
    public void rotateImageDoesntWorkFor180Angle(){
        Bitmap bitmap = BitmapFactory.decodeResource(mActivityRule.getActivity().getResources(),R.mipmap.test_photo);
        Bitmap rotatedBitmap = rotateImage(bitmap, 180);
        assertNotEquals(bitmap, rotatedBitmap);
    }

    @Test
    public void externalStorageIsWritable(){
        assertTrue(isExternalStorageWritable());
    }
}
