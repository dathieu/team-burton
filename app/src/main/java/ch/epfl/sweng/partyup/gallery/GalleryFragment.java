package ch.epfl.sweng.partyup.gallery;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentTransaction;
import android.support.v7.widget.DefaultItemAnimator;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.google.firebase.storage.StorageReference;

import java.util.List;

import ch.epfl.sweng.partyup.R;

public class GalleryFragment extends Fragment {

    private GalleryAdapter mAdapter;
    private static List<StorageReference> imagesRef;

    /**
     * Create a new instance of the gallery
     *
     * @param imRef the reference
     * @return a new gallery fragment
     */
    public static GalleryFragment newInstance(List<StorageReference> imRef) {
        GalleryFragment galleryFragment = new GalleryFragment();
        imagesRef = imRef;
        return galleryFragment;
    }

    /**
     * Create the fragment
     *
     * @param savedInstanceState the saved instance
     */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mAdapter = new GalleryAdapter(getActivity().getApplicationContext(), imagesRef);
    }

    /**
     * Create the view for the fragment
     *
     * @param inflater           the inflater to use
     * @param container          the container to use
     * @param savedInstanceState the qsaved instance
     * @return the view created
     */
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.activity_gallery, container, false);
        TextView textViewNoPhoto = (TextView) view.findViewById(R.id.textViewNoPhoto);
        if (imagesRef.size()==0)
            textViewNoPhoto.setVisibility(View.VISIBLE);

        RecyclerView recyclerView = (RecyclerView) view.findViewById(R.id.recycler_view_gallery);

        RecyclerView.LayoutManager mLayoutManager = new GridLayoutManager(getActivity().getApplicationContext(), 2);
        recyclerView.setLayoutManager(mLayoutManager);
        recyclerView.setItemAnimator(new DefaultItemAnimator());
        recyclerView.setAdapter(mAdapter);

        recyclerView.addOnItemTouchListener(new GalleryAdapter.RecyclerTouchListener(getActivity().getApplicationContext(), recyclerView, new GalleryAdapter.ClickListener() {
            @Override
            public void onClick(View view, int position) {
                Bundle bundle = new Bundle();
                bundle.putInt("position", position);

                FragmentTransaction ft = getActivity().getSupportFragmentManager().beginTransaction();
                SlideshowDialogFragment newFragment = SlideshowDialogFragment.newInstance(imagesRef);
                newFragment.setArguments(bundle);
                newFragment.show(ft, "slideshow");
            }

            @Override
            public void onLongClick(View view, int position) {

            }
        }));

        return view;
    }
}
