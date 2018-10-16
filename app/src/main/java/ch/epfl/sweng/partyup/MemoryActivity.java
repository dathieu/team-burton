package ch.epfl.sweng.partyup;

import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.design.widget.BottomNavigationView;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.MenuItem;

import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import java.util.ArrayList;
import java.util.List;

import ch.epfl.sweng.partyup.dbstore.Party;
import ch.epfl.sweng.partyup.dbstore.listeners.CompletionListener;
import ch.epfl.sweng.partyup.dbstore.schemas.PartySchema;
import ch.epfl.sweng.partyup.gallery.GalleryFragment;

public class MemoryActivity extends AppCompatActivity {

    public static Party party;
    public static PartySchema partySchema;

    private FirebaseStorage storage;

    BottomNavigationView bottomNavigationView;

    /**
     * Create the memory activity
     *
     * @param savedInstanceState the saved instance
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_memory);

        if (party == null)
            throw new AssertionError("this activity must not be started without a party");

        if (partySchema == null)
            throw new AssertionError("this activity must not be started without a partySchema");

        final SongListFragment fragment = SongListFragment.newInstance(partySchema, this);
        switchToFragment(fragment);

        storage = FirebaseStorage.getInstance();
        //enables the user to switch between tabs
        bottomNavigationView = (BottomNavigationView) findViewById(R.id.navigation_memory);
        bottomNavigationView.setOnNavigationItemSelectedListener(new BottomNavigationView.OnNavigationItemSelectedListener() {
            @Override
            public boolean onNavigationItemSelected(@NonNull MenuItem item) {
                switch (item.getItemId()) {
                    case R.id.memory_navigation_photos:
                        party.getPhotoUrl(new CompletionListener<List<String>>() {
                            @Override
                            public void onCompleted(List<String> result) {
                                List<StorageReference> imagesRef = new ArrayList<>();
                                for (String p : result) {
                                    Log.e("TAG photo url", p);
                                    imagesRef.add(storage.getReferenceFromUrl(p));
                                }
                                GalleryFragment galleryFragment = GalleryFragment.newInstance(imagesRef);
                                switchToFragment(galleryFragment);
                            }
                        });
                        break;
                    case R.id.memory_navigation_songslist:
                        switchToFragment(fragment);
                        break;
                }

                return true;
            }
        });
    }

    /**
     * Helper to switch fragment
     *
     * @param fragment the fragment to switch to
     */
    private void switchToFragment(Fragment fragment) {
        FragmentManager fragmentManager = getSupportFragmentManager();
        FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
        fragmentTransaction.replace(R.id.content, fragment).commit();
    }


}
