package ch.epfl.sweng.partyup;

import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.Toast;

import java.io.File;

import ch.epfl.sweng.partyup.dbstore.Party;
import ch.epfl.sweng.partyup.dbstore.listeners.CompletionListener;
import ch.epfl.sweng.partyup.dbstore.statemachine.DBResult;


public class CameraFragment extends android.support.v4.app.Fragment {

    private static File photoFile;
    private static Party party;
    private static Bitmap bitmap;

    public CameraFragment() {
        // Required empty public constructor
    }

    public static CameraFragment newInstance(Party party, Bitmap bitmap, File photoFile) {
        CameraFragment cameraFragment = new CameraFragment();
        CameraFragment.party = party;
        CameraFragment.bitmap = bitmap;
        CameraFragment.photoFile = photoFile;
        return cameraFragment;
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        ((UserActivity)getActivity()).hideStatusBar();
        ((UserActivity)getActivity()).setNavigationVisibility(View.GONE);
        ((UserActivity)getActivity()).setHeaderVisibility(View.GONE);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_camera, container, false);
        ImageView imageView = (ImageView) view.findViewById(R.id.camera_view);
        imageView.setImageBitmap(bitmap);
        ImageView approvePictureButton = (ImageButton) view.findViewById(R.id.approvePictureButton);
        ImageButton retakePictureButton = (ImageButton) view.findViewById(R.id.retakePictureButton);
        approvePictureButton.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View v)
            {
                party.uploadPhoto(Uri.fromFile(photoFile), new CompletionListener<DBResult>() {
                    @Override
                    public void onCompleted(DBResult result) {
                        if (result==DBResult.Failure)
                            Toast.makeText(getActivity(), R.string.errorUploadPhoto, Toast.LENGTH_SHORT).show();
                    }
                });
                photoFile.deleteOnExit();
                getActivity().getSupportFragmentManager().popBackStack();
            }
        });
        retakePictureButton.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View v)
            {
                photoFile.deleteOnExit();
                bitmap = null;
                ((UserActivity)getActivity()).dispatchTakePictureIntent();
            }
        });
        return view;
    }

    @Override
    public void onDetach(){
        super.onDetach();
        ((UserActivity)getActivity()).showStatusBar();
        ((UserActivity)getActivity()).setNavigationVisibility(View.VISIBLE);
        ((UserActivity)getActivity()).setHeaderVisibility(View.VISIBLE);
    }
}
