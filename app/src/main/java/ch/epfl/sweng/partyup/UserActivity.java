package ch.epfl.sweng.partyup;

import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.Matrix;
import android.media.ExifInterface;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.support.annotation.NonNull;
import android.support.design.widget.BottomNavigationView;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.content.FileProvider;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.PopupMenu;
import android.view.MenuItem;
import android.view.View;
import android.view.WindowManager;
import android.widget.LinearLayout;
import android.widget.Toast;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.MultiFormatWriter;
import com.google.zxing.WriterException;
import com.google.zxing.common.BitMatrix;
import com.spotify.sdk.android.authentication.AuthenticationResponse;
import com.spotify.sdk.android.player.Spotify;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

import ch.epfl.sweng.partyup.SpotDB.SpotDBResponseHandler;
import ch.epfl.sweng.partyup.SpotDB.SpotDBResponseType;
import ch.epfl.sweng.partyup.dbstore.ConnectionProvider;
import ch.epfl.sweng.partyup.dbstore.listeners.CompletionListener;
import ch.epfl.sweng.partyup.dbstore.statemachine.DBResult;

public abstract class UserActivity extends AppCompatActivity implements PopupMenu.OnMenuItemClickListener, SpotDBResponseHandler {

    private final static int QRcodeWidth = 500;
    private final static int QRcodeHeight = 500;
    protected static Bitmap bitmap;

    static final int REQUEST_TAKE_PHOTO = 45;

    protected QRCodeFragment qrCodeFragment;
    protected DynamicListFragment proposalFragment;
    protected SpotifyPlayerControl spotifyPlayer;
    protected PlaylistFragment playlistFragment;

    public BottomNavigationView bottomNavigationView;
    public LinearLayout header;

    private File photoFile;
    private boolean firstTimePictureTaken;


    private boolean playerShowed;

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_user);
        lockForLoading(this);

        bottomNavigationView = (BottomNavigationView) findViewById(R.id.bottom_navigation);
        bottomNavigationView.setOnNavigationItemSelectedListener(mOnNavigationItemSelectedListener);

        photoFile = null;

        header = (LinearLayout) findViewById(R.id.header_layout);

        playerShowed = true;
    }

    /**
     * Locke the activity. All the fields are disabled
     *
     * @param activity the activity to lock
     */
    public static void lockForLoading(Activity activity) {
        View progressBar = activity.findViewById(R.id.loadingBar);
        progressBar.setVisibility(View.VISIBLE);
        activity.getWindow().setFlags(WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE,
                WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE);
    }

    /**
     * unlock the activity and enables again the fields
     *
     * @param activity activity to unlock
     */
    public static void unlock(Activity activity) {
        View progressBar = activity.findViewById(R.id.loadingBar);
        progressBar.setVisibility(View.GONE);
        activity.getWindow().clearFlags(WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE);
    }

    /**
     * Allow to switch between the QRcode tab and the proposal list tab.
     */
    protected final BottomNavigationView.OnNavigationItemSelectedListener mOnNavigationItemSelectedListener
            = new BottomNavigationView.OnNavigationItemSelectedListener() {

        @Override
        public boolean onNavigationItemSelected(@NonNull MenuItem item) {
            qrCodeFragment = QRCodeFragment.newInstance(bitmap);
            proposalFragment = DynamicListFragment.newInstance(SharedAndSavedData.connectedParty);

            switch (item.getItemId()) {
                case R.id.navigation_playlist:
                    playerShowed = true;
                    changeView(proposalFragment, true, false);
                    return true;
                case R.id.navigation_qrcode:
                    playerShowed = false;
                    changeView(qrCodeFragment, false, false);
                    return true;
            }
            return false;
        }

    };


    /**
     * Helper that allow to hide/show the player if needed, and change the fragment displayed
     *
     * @param fragment         the fragment to display
     * @param showPlayer       true if the player needs to be displayed
     * @param backStackEnabled true if you need to add to the backStack the Fragment fragment
     */
    protected void changeView(Fragment fragment, boolean showPlayer, boolean backStackEnabled) {
        FragmentManager fragmentManager = getSupportFragmentManager();
        FragmentTransaction transaction = fragmentManager.beginTransaction();
        if (backStackEnabled)
            transaction.addToBackStack(null);
        transaction.replace(R.id.content, fragment);
        if (showPlayer)
            transaction.show(fragmentManager.findFragmentById(R.id.playerFragment));
        else
            transaction.hide(fragmentManager.findFragmentById(R.id.playerFragment));
        transaction.commit();
    }

    /**
     * Generate the Qrcode displayed in the Qrcode tab.
     *
     * @param value the value you want to encode
     * @return return the BItmap of the QrCode
     * @throws WriterException if the qrcode could not be written
     */
    public static Bitmap generateQRCode(String value) throws WriterException {
        BitMatrix bitMatrix;

        bitMatrix = new MultiFormatWriter().encode(
                value,
                BarcodeFormat.QR_CODE,
                QRcodeWidth, QRcodeHeight, null
        );

        int[] pixels = new int[QRcodeWidth * QRcodeHeight];

        for (int y = 0; y < QRcodeHeight; y++) {
            int offset = y * QRcodeWidth;
            for (int x = 0; x < QRcodeWidth; x++) {
                pixels[offset + x] = bitMatrix.get(x, y) ? Color.argb(255, 34, 34, 34) : Color.argb(255, 0, 144, 54);
            }
        }
        bitmap = Bitmap.createBitmap(QRcodeWidth, QRcodeHeight, Bitmap.Config.ARGB_4444);

        bitmap.setPixels(pixels, 0, 500, 0, 0, QRcodeWidth, QRcodeHeight);

        return bitmap;
    }

    /**
     * Handle the response from spotify
     *
     * @param response the spotify response
     * @param type     the type of the spotify response
     */
    @Override
    public void onSpotDBResponse(JSONObject response, SpotDBResponseType type) {
        //if the request was a user information request.
        if (type == SpotDBResponseType.PROFILE) {
            try {
                String spotifyId = response.getString("id");
                ConnectionProvider.getConnection().registerSpotifyId(spotifyId, new CompletionListener<DBResult>() {
                    @Override
                    public void onCompleted(DBResult result) {
                        // nothing to do here
                    }
                });
            } catch (JSONException ex) {
                ex.printStackTrace();
            }
        }
    }

    @Override
    protected void onDestroy() {
        //Destroy the player, leave it could lead to resources leak
        if (spotifyPlayer != null)
            Spotify.destroyPlayer(spotifyPlayer);
        super.onDestroy();
    }

    /**
     * Enable to add the current played song to the user spotify playlist if he is connected
     *
     * @param view the view calling this method
     */
    public void saveToLibrary(View view) {
        if (SpotAuth.getSpotToken() == null) {
            String[] scopes = new String[]{"user-library-modify"};
            SpotAuth.spotRequest(this, scopes, AuthenticationResponse.Type.TOKEN);
        } else {
            SpotifyPlayerControl.saveCurrentTrackToLibrary(this);
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        //Handle the response from the camera app
        if (requestCode == REQUEST_TAKE_PHOTO && resultCode == RESULT_OK) {
            Bitmap myBitmap = BitmapFactory.decodeFile(photoFile.getPath());

            // rotate the picture
            ExifInterface exif = null;
            try {
                exif = new ExifInterface(photoFile.getPath());
            } catch (IOException e) {
                e.printStackTrace();
            }
            int orientation = 0;
            if (exif != null) {
                orientation = exif.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_UNDEFINED);
            }
            Bitmap rotatedBitmap;
            switch (orientation) {
                case ExifInterface.ORIENTATION_ROTATE_90:
                    rotatedBitmap = rotateImage(myBitmap, 90);
                    break;
                case ExifInterface.ORIENTATION_ROTATE_180:
                    rotatedBitmap = rotateImage(myBitmap, 180);
                    break;
                case ExifInterface.ORIENTATION_ROTATE_270:
                    rotatedBitmap = rotateImage(myBitmap, 270);
                    break;
                case ExifInterface.ORIENTATION_NORMAL:
                default:
                    rotatedBitmap = myBitmap;
            }
            if (firstTimePictureTaken) {
                firstTimePictureTaken = false;
                changeView(CameraFragment.newInstance(SharedAndSavedData.connectedParty, rotatedBitmap, photoFile), false, true);
            } else
                changeView(CameraFragment.newInstance(SharedAndSavedData.connectedParty, rotatedBitmap, photoFile), false, false);
        }
    }

    public abstract boolean onMenuItemClick(MenuItem item);

    public abstract void onSearchButtonClick(View view);

    /**
     * Open the camera fragment
     *
     * @param view the view calling this method
     */
    public void onCameraButtonClick(View view) {
        firstTimePictureTaken = true;
        dispatchTakePictureIntent();
    }

    /**
     * Create a file to stock ptohos in it
     *
     * @return a FIle created to stock photos
     * @throws IOException if the file wasn't created successfully
     */
    private File createImageFile() throws IOException {
        // Create an image file name
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.FRANCE).format(new Date());
        String imageFileName = "JPEG_" + timeStamp + "_";
        File storageDir = UserActivity.this.getExternalFilesDir(Environment.DIRECTORY_PICTURES);
        return File.createTempFile(imageFileName, ".jpg", storageDir);
    }

    /**
     * Display the picture intent if a photo is taken
     */
    public void dispatchTakePictureIntent() {
        Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        if (UserActivity.this.checkSelfPermission(android.Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            Toast.makeText(UserActivity.this, "The app needs to have access to the external storage.", Toast.LENGTH_SHORT).show();
        } else if (takePictureIntent.resolveActivity(UserActivity.this.getPackageManager()) != null && // check that the app handle takePictureIntent
                isExternalStorageWritable()) {
            // Create the File where the photo should go
            try {
                photoFile = createImageFile();
            } catch (IOException ex) {
                ex.printStackTrace();
            }
            if (photoFile != null) {
                Uri photoURI = FileProvider.getUriForFile(UserActivity.this, "com.example.android.fileprovider", photoFile);
                takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, photoURI);
                startActivityForResult(takePictureIntent, REQUEST_TAKE_PHOTO);
            }
        }
    }

    /**
     * @return the state of the ExternalStorageState
     */
    public static boolean isExternalStorageWritable() {
        String state = Environment.getExternalStorageState();
        return (Environment.MEDIA_MOUNTED.equals(state));
    }

    /**
     * Rotate a picture
     *
     * @param source the source picture
     * @param angle  the angle to rotate
     * @return the new bitmap rotated
     */
    public static Bitmap rotateImage(Bitmap source, float angle) {
        Matrix matrix = new Matrix();
        matrix.postRotate(angle);
        return Bitmap.createBitmap(source, 0, 0, source.getWidth(), source.getHeight(), matrix, true);
    }

    /**
     * Handle the back on the search fragment
     *
     * @param view tha view calling this method
     */
    public void onClickBackButton(View view) {
        onBackPressed();
    }

    /**
     * Hide the upper bar
     */
    public void hideStatusBar() {
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
    }

    /**
     * Show the upper bar
     */
    public void showStatusBar() {
        getWindow().clearFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
    }

    /**
     * Set the visibility of the lower bar
     *
     * @param visibility the value of the visibility
     */
    public void setNavigationVisibility(int visibility) {
        bottomNavigationView.setVisibility(visibility);
    }

    /**
     * Set the visibility of the upper bar
     *
     * @param visibility the value of the visibility
     */
    public void setHeaderVisibility(int visibility) {
        header.setVisibility(visibility);
    }


    @Override
    public void onBackPressed() {
        boolean onQR = (qrCodeFragment != null) && qrCodeFragment.isVisible();
        boolean onProp = (proposalFragment != null) && proposalFragment.isVisible();
        boolean onPlaylist = (playlistFragment != null) && playlistFragment.isVisible();
        if (!onQR && !onProp) {
            FragmentManager fragmentManager = getSupportFragmentManager();
            super.onBackPressed();
            FragmentTransaction transaction = fragmentManager.beginTransaction();
            if (!playerShowed || onPlaylist) {
                transaction.hide(fragmentManager.findFragmentById(R.id.playerFragment));
            } else {
                transaction.show(fragmentManager.findFragmentById(R.id.playerFragment));
            }
            transaction.commit();
        }
    }
}
