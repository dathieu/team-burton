package ch.epfl.sweng.partyup;

import android.app.Activity;
import android.content.Intent;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Date;

import ch.epfl.sweng.partyup.containers.Tuple;
import ch.epfl.sweng.partyup.dbstore.Party;
import ch.epfl.sweng.partyup.dbstore.schemas.PartySchema;


public class MemoriesAdapter extends RecyclerView.Adapter<MemoriesAdapter.ViewHolder> {
    private ArrayList<Tuple<Party, PartySchema>> mDataSet;
    private Activity mParentActivity;

    /**
     * Constructor of the memories
     *
     * @param parentActivity the parent activity
     * @param dataSet        the dataSet of parties
     */
    public MemoriesAdapter(Activity parentActivity, ArrayList<Tuple<Party, PartySchema>> dataSet) {
        if (dataSet == null || parentActivity == null) {
            throw new IllegalArgumentException("Cannot instantiate MemoriesAdapter with null parent activity or data set");
        }
        mDataSet = dataSet;
        mParentActivity = parentActivity;
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        public TextView nameTextView;
        public TextView dateTextView;
        public View holderView;

        public ViewHolder(View partyDisplay) {
            super(partyDisplay);
            nameTextView = (TextView) partyDisplay.findViewById(R.id.name_text_view);
            dateTextView = (TextView) partyDisplay.findViewById(R.id.date_text_view);
            holderView = partyDisplay.findViewById(R.id.party_holder_layout);
        }
    }

    @Override
    public MemoriesAdapter.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View partyView = LayoutInflater.from(parent.getContext()).inflate(R.layout.party_display, parent, false);
        return new ViewHolder(partyView);
    }

    @Override
    public void onBindViewHolder(final MemoriesAdapter.ViewHolder holder, final int position) {
        PartySchema schema = mDataSet.get(position).object2;
        holder.nameTextView.setText(schema.getName());
        holder.dateTextView.setText(intToDateString(schema.getTimeStamp()));

        holder.holderView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Tuple<Party, PartySchema> target = mDataSet.get(holder.getAdapterPosition());
                Intent intent = new Intent(mParentActivity, MemoryActivity.class);
                MemoryActivity.party = target.object1;
                MemoryActivity.partySchema = target.object2;
                mParentActivity.startActivity(intent);
            }
        });
    }

    /**
     * @return the number of items in this adapter
     */
    @Override
    public int getItemCount() {
        return mDataSet.size();
    }

    /**
     * @param intDate the date
     * @return a string representation of the date
     */
    private String intToDateString(long intDate) {
        return DateFormat.getInstance().format(new Date(intDate));
    }
}
