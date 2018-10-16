package ch.epfl.sweng.partyup;

import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.DialogFragment;
import android.support.v7.app.AlertDialog;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

public class ConfirmEndPartyFragment extends DialogFragment {

        public ConfirmEndPartyFragment() {
            // Required empty public constructor
        }

        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
        }

        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container,
                                 Bundle savedInstanceState) {
            // Inflate the layout for this fragment
            return inflater.inflate(R.layout.dialog_confirm_end_party, container, false);
        }

        /* The activity that creates an instance of this dialog fragment must
         * implement this interface in order to receive event callbacks.
         * Each method passes the DialogFragment in case the host needs to query it. */
        public interface ConfirmEndPartyListener {
            void onDialogConfirmEndPartyClick(DialogFragment dialog, boolean confirmed);
        }

        // Use this instance of the interface to deliver action events
        ConfirmEndPartyListener mListener;

        // Override the Fragment.onAttach() method to instantiate the NoticeDialogListener
        @Override
        public void onAttach(Context context) {
            super.onAttach(context);
            // Verify that the host activity implements the callback interface
            try {
                // Instantiate the NoticeDialogListener so we can ic_approve_picture events to the host
                mListener = (ConfirmEndPartyListener) context;
            } catch (ClassCastException e) {
                // The activity doesn't implement the interface, throw exception
                throw new ClassCastException(context.toString() + " must implement NoticeDialogListener");
            }
        }

        @NonNull
        @Override
        public Dialog onCreateDialog(Bundle savedInstanceState) {
            AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
            // Get the layout inflater
            LayoutInflater inflater = getActivity().getLayoutInflater();

            // Inflate and set the layout for the dialog
            // Pass null as the parent view because its going in the dialog layout
            final View confirmEndPartyView = inflater.inflate(R.layout.dialog_confirm_end_party, null);
            builder.setView(confirmEndPartyView)
                    // Add action buttons
                    .setPositiveButton(R.string.end_party, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int id) {
                            mListener.onDialogConfirmEndPartyClick(ConfirmEndPartyFragment.this, true);
                        }
                    })
                    .setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int id) {
                            ConfirmEndPartyFragment.this.getDialog().cancel();
                        }
                    });
            return builder.create();
        }
}
