package ch.epfl.sweng.partyup;

import android.content.Context;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;

import com.android.volley.toolbox.JsonObjectRequest;

import org.json.JSONException;
import org.json.JSONObject;

import ch.epfl.sweng.partyup.SpotDB.SpotDBRequestQueue;
import ch.epfl.sweng.partyup.SpotDB.SpotDBResponseHandler;
import ch.epfl.sweng.partyup.SpotDB.SpotDBResponseType;
import ch.epfl.sweng.partyup.SpotDB.SpotDBSearchHandler;
import ch.epfl.sweng.partyup.SpotDB.SpotDBTypes;
import ch.epfl.sweng.partyup.dbstore.Party;
import ch.epfl.sweng.partyup.dbstore.schemas.SongSchema;

public class SearchFragment extends Fragment implements SpotDBResponseHandler {
    private RecyclerView searchRecyclerView;
    private RecyclerView.Adapter searchAdapter;
    private static Party party;
    private SongSchema[] trackResults = new SongSchema[]{};
    private View rootView;

    private EditText searchField;

    /**
     * Create a new instance of the fragment
     *
     * @param party the party host the fragment
     * @return a new instance of the fragment
     */
    public static SearchFragment newInstance(Party party) {
        SearchFragment fragment = new SearchFragment();
        SearchFragment.party = party;
        return fragment;
    }

    /**
     * Create the fragment
     *
     * @param savedInstanceState the saved instance
     */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ((UserActivity) getActivity()).setNavigationVisibility(View.GONE);
        ((UserActivity) getActivity()).setHeaderVisibility(View.GONE);
    }

    @Override
    public void onDetach() {
        super.onDetach();

        //hide the keyboard
        InputMethodManager imm = (InputMethodManager) getActivity().getSystemService(Context.INPUT_METHOD_SERVICE);
        imm.hideSoftInputFromWindow(searchField.getWindowToken(), 0);

        ((UserActivity) getActivity()).setNavigationVisibility(View.VISIBLE);
        ((UserActivity) getActivity()).setHeaderVisibility(View.VISIBLE);
    }

    /**
     * Create the view of the fragment
     *
     * @param inflater           the inflater to use
     * @param container          the conainer to use
     * @param savedInstanceState the saved instacne
     * @return the view created
     */
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        rootView = inflater.inflate(R.layout.fragment_search, container, false);

        searchField = (EditText) rootView.findViewById(R.id.search_field);

        Button button = (Button) rootView.findViewById(R.id.playlistButton);

        if (!(this.getActivity() instanceof HostActivity)) {
            button.setVisibility(View.GONE);

        }
        searchRecyclerView = (RecyclerView) rootView.findViewById(R.id.search_recycler_view);

        searchRecyclerView.setHasFixedSize(true);

        RecyclerView.LayoutManager searchLayoutManager = new LinearLayoutManager(this.getContext());
        searchRecyclerView.setLayoutManager(searchLayoutManager);

        //Set adapter
        searchAdapter = new SearchAdapter(trackResults, party);
        searchRecyclerView.setAdapter(searchAdapter);

        searchField.setOnKeyListener(new View.OnKeyListener() {

            public boolean onKey(View view, int keyCode, KeyEvent event) {
                if (event.getAction() == KeyEvent.ACTION_DOWN) {
                    switch (keyCode) {
                        case KeyEvent.KEYCODE_DPAD_CENTER:
                        case KeyEvent.KEYCODE_ENTER:
                            requestSearch(searchField);
                            return true;
                        default:
                            break;
                    }
                }
                return false;
            }
        });
        rootView.findViewById(R.id.no_result_text).setVisibility(View.INVISIBLE);
        return rootView;
    }

    @Override
    public void onResume() {
        super.onResume();

        //display the keyboard
        searchField.requestFocus();
        InputMethodManager imgr = (InputMethodManager) getActivity().getSystemService(Context.INPUT_METHOD_SERVICE);
        imgr.showSoftInput(searchField, InputMethodManager.SHOW_IMPLICIT);
    }

    public void requestSearch(View view){
        EditText searchField = (EditText) rootView.findViewById(R.id.search_field);
        String query = searchField.getText().toString();
        if (!query.isEmpty()) {
            JsonObjectRequest trackRequest = new SpotDBSearchHandler(this, SpotAuth.getSpotToken()).trackRequest(query, SpotDBTypes.TRACK);
            SpotDBRequestQueue.getInstance(this.getContext()).addToRequestQueue(trackRequest);
        }


        if (view != null) {
            InputMethodManager imm = (InputMethodManager) view.getContext().getSystemService(Context.INPUT_METHOD_SERVICE);
            imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
        }
    }

    /**
     * Hendle the reponse of spotify
     *
     * @param responseWrapped the spotify simplified response
     * @param type            the type of the reponse
     */
    public void onSpotDBResponse(JSONObject responseWrapped, SpotDBResponseType type) {
        try {
            //populate trackResults
            JSONObject response = (JSONObject) responseWrapped.get("tracks");

            try {
                trackResults = SpotDBSearchHandler.makeSongArray(response, false);

                rootView.findViewById(R.id.no_result_text).setVisibility(View.INVISIBLE);

            } catch (IllegalArgumentException ex) {
                trackResults = new SongSchema[]{};
                rootView.findViewById(R.id.no_result_text).setVisibility(View.VISIBLE);
            }

            searchAdapter = new SearchAdapter(trackResults, party);
            searchRecyclerView.setAdapter(searchAdapter);

        } catch (JSONException jsException) {
            jsException.printStackTrace();
        }
    }
}
