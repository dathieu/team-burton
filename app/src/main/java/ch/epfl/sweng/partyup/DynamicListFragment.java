package ch.epfl.sweng.partyup;

import android.app.Activity;
import android.content.Context;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import java.util.ArrayList;

import ch.epfl.sweng.partyup.dbstore.Party;
import ch.epfl.sweng.partyup.dbstore.schemas.ProposalSchema;
import ch.epfl.sweng.partyup.eventlisteners.EventListener;
import ch.epfl.sweng.partyup.eventlisteners.ProposalAddListener;

public class DynamicListFragment extends Fragment {

    public RecyclerView dynamicListView;
    public static DynamicListAdapter dynamicListAdapter;
    private RecyclerView.LayoutManager dynamicListLayoutManager;

    private ArrayList<EventListener> uiEventListeners;
    private ProposalAddListener proposalAddListener;

    /**
     * Adds a listener
     * @param uiEventListener the listener to add
     */
    public void addUIEventListener(EventListener uiEventListener) {
        uiEventListeners.add(uiEventListener);
    }

    /**
     * REmoves a listener
     * @param uiEventListener the listener to remove
     */
    public void removeUIEventListener(EventListener uiEventListener) {
        if (uiEventListeners.contains(uiEventListener)) {
            uiEventListeners.remove(uiEventListener);
        }
    }

    private static Party party;

    /**
     * Constructor of the class
     */
    public DynamicListFragment() {

        uiEventListeners = new ArrayList<>();
    }

    /**
     * Creates a new instance of the dynamic list fragment
     *
     * @param party      the party
     * @return the new instance
     */

    public static DynamicListFragment newInstance(Party party) {
        DynamicListFragment fragment = new DynamicListFragment();
        DynamicListFragment.party =party;
        return fragment;
    }


    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View rootView = inflater.inflate(R.layout.fragment_dynamic_list, container, false);

        dynamicListView = (RecyclerView) rootView.findViewById(R.id.dynamic_list_recycler_view);

        // use a linear layout manager
        dynamicListLayoutManager = new LinearLayoutManager(getActivity());
        dynamicListView.setLayoutManager(dynamicListLayoutManager);

        // specify the adapter

        dynamicListAdapter = new DynamicListAdapter(party,getActivity());
        dynamicListAdapter.isOnHost= (this.getActivity() instanceof HostActivity);

        dynamicListView.setAdapter(dynamicListAdapter);
        if (proposalAddListener != null)
            dynamicListAdapter.addProposalAddListener(proposalAddListener);

        for (EventListener uiEventListener : uiEventListeners)
            uiEventListener.onCreated();

        return rootView;
    }

    @Override
    public void onAttach(Context context) {

        super.onAttach(context);

        if (context instanceof ProposalAddListener)
            proposalAddListener = (ProposalAddListener) context;
    }

    /**
     * Removes the song with the most upvotes
     */
    public static void removeTopSong() {
        dynamicListAdapter.removeTopSong();
    }

    public static ProposalSchema getTopVoted() {

        if (dynamicListAdapter != null) {
            return dynamicListAdapter.getTopVoted();
        } else return null;
    }
}
