package ch.epfl.sweng.partyup;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;


public class PlayerFragment extends Fragment {
    /**
     * Create the view for our fragment
     *
     * @param inflater           the inflater used for the fragment
     * @param container          the container used for the fragment
     * @param savedInstanceState the saved instance
     * @return return the viex of the fragment
     */
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_player, container, false);
    }
}
