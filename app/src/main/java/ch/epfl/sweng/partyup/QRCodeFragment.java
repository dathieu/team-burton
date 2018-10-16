package ch.epfl.sweng.partyup;

import android.graphics.Bitmap;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;


public class QRCodeFragment extends Fragment {

    private static Bitmap bitmap;

    /**
     * Create a new instance of the fragment
     *
     * @param qrbitmap the bitmap used to create the qrcode
     * @return a new instance of the fragment
     */
    public static QRCodeFragment newInstance(Bitmap qrbitmap) {
        QRCodeFragment fragment = new QRCodeFragment();
        bitmap = qrbitmap;
        return fragment;
    }

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
        View view = inflater.inflate(R.layout.fragment_qrcode, container, false);
        ImageView imageView = (ImageView) view.findViewById(R.id.qr_view);
        imageView.setImageBitmap(bitmap);

        return view;
    }
}
