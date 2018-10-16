package ch.epfl.sweng.partyup.util;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;

import ch.epfl.sweng.partyup.R;
import ch.epfl.sweng.partyup.dbstore.schemas.ProposalSchema;
import ch.epfl.sweng.partyup.eventlisteners.ProposalAddListener;

public class DynamicListFragmentTest extends AppCompatActivity implements ProposalAddListener {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_dynamic_list_fragment_test);
    }

    @Override
    public void addProposal(ProposalSchema newProposal) {

    }
}
