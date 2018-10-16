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
import java.util.List;

import ch.epfl.sweng.partyup.dbstore.schemas.PartySchema;
import ch.epfl.sweng.partyup.eventlisteners.EventListener;

public class SongListFragment extends Fragment{

    public RecyclerView dynamicListView;
    public SongListAdapter dynamicListAdapter;


    private List<EventListener> uiEventListeners;

    private PartySchema partySchema;
    private Activity activity;

    public List<Integer> savedSongs;
    public SongListFragment() {
        uiEventListeners = new ArrayList<>();
    }
    public void addUIEventListener(EventListener uiEventListener){
        uiEventListeners.add(uiEventListener);
    }
    public void removeUIEventListener(EventListener uiEventListener){
        if(uiEventListeners.contains(uiEventListener)){
            uiEventListeners.remove(uiEventListener);
        }
    }
    public static SongListFragment newInstance(PartySchema partySchema, Activity memoryActivity) {
        SongListFragment fragment = new SongListFragment();
        fragment.activity= memoryActivity;
        fragment.partySchema =partySchema;
        fragment.savedSongs= new ArrayList<>();
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
        View rootView =  inflater.inflate(R.layout.fragment_song_list, container, false);

        dynamicListView = (RecyclerView) rootView.findViewById(R.id.dynamic_list_recycler_view);

        // use a linear layout manager
        RecyclerView.LayoutManager dynamicListLayoutManager = new LinearLayoutManager(getActivity());
        dynamicListView.setLayoutManager(dynamicListLayoutManager);

        // specify the adapter
        dynamicListAdapter = new SongListAdapter(partySchema,activity, this);

        dynamicListView.setAdapter(dynamicListAdapter);

        for(EventListener uiEventListener : uiEventListeners)
            uiEventListener.onCreated();

        return rootView;
    }

    @Override
    public void onAttach(Context context) {

        super.onAttach(context);
    }
}

