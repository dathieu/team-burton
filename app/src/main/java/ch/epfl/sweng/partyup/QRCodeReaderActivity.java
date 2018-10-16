package ch.epfl.sweng.partyup;


import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.util.SparseArray;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.widget.TextView;

import com.google.android.gms.vision.CameraSource;
import com.google.android.gms.vision.Detector;
import com.google.android.gms.vision.barcode.Barcode;
import com.google.android.gms.vision.barcode.BarcodeDetector;

import java.io.IOException;

import ch.epfl.sweng.partyup.containers.Tuple;
import ch.epfl.sweng.partyup.dbstore.Connection;
import ch.epfl.sweng.partyup.dbstore.ConnectionProvider;
import ch.epfl.sweng.partyup.dbstore.Party;
import ch.epfl.sweng.partyup.dbstore.listeners.CompletionListener;
import ch.epfl.sweng.partyup.dbstore.statemachine.DBResult;

public class QRCodeReaderActivity extends AppCompatActivity {

    private CameraSource cam;
    private SurfaceView cam_view;
    private TextView text_dec;
    public BarcodeDetector qr_detect;
    public Intent intent;

    private String partyID;

    public static Connection dBConnection;

    /**
     * setup the activity.
     * create a barcode detector and reader, set it up to detected QRcode and connect to the party
     * when decoded.
     *
     * @param savedInstanceState bundle of parameter saved by system
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_qrcode_reader);

        intent = new Intent(this, GuestActivity.class);

        cam_view = (SurfaceView) findViewById(R.id.image_cam);
        text_dec = (TextView) findViewById(R.id.decod_text);

        qr_detect = new BarcodeDetector.Builder(this)
                .setBarcodeFormats(Barcode.QR_CODE)
                .build();

        cam = new CameraSource.Builder(this, qr_detect)
                .setAutoFocusEnabled(true).build();

        dBConnection = ConnectionProvider.getConnection();

        qr_detect.setProcessor(new Detector.Processor<Barcode>() {
            @Override
            public void release() {
            }

            @Override
            public void receiveDetections(Detector.Detections<Barcode> detections) {
                final SparseArray<Barcode> qr = detections.getDetectedItems();
                if (qr.size() != 0) {
                    String newPartyID = qr.valueAt(0).displayValue;
                    if (!newPartyID.equals(partyID)) {
                        partyID = newPartyID;

                        text_dec.post(new Runnable() {
                            public void run() {
                                text_dec.setText(partyID);
                            }
                        });

                        dBConnection.connectToParty(partyID, new CompletionListener<Tuple<DBResult, Party>>() {
                            @Override
                            public void onCompleted(Tuple<DBResult, Party> result) {
                                switch (result.object1) {
                                    case Success:
                                        SharedAndSavedData.connectedParty = result.object2;
                                        SharedAndSavedData.connectedPartyID = partyID;
                                        SharedAndSavedData.setSavedConnectedInfo(QRCodeReaderActivity.this,partyID,false);

                                        startActivity(intent);
                                        intent.setFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
                                        startActivityIfNeeded(intent,0);
                                        finish();
                                        break;
                                    case NotFound:
                                        break;
                                    case InvalidKey:
                                        break;
                                    default:
                                        break;
                                }
                            }
                        });

                    }
                }
            }
        });

        if (checkSelfPermission(android.Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            text_dec.setText(R.string.qr_text_no_perm);
        } else {
            cam_view_setup();
        }
    }

    /**
     * setup the camera used to see the QRcode
     */
    void cam_view_setup() {
        cam_view.getHolder().addCallback(new SurfaceHolder.Callback() {
            @Override
            public void surfaceCreated(SurfaceHolder holder) {
                try {
                    if (checkSelfPermission(android.Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
                        cam.start(cam_view.getHolder());
                    }
                } catch (IOException ie) {
                    Log.e("CAMERA SOURCE", ie.getMessage());
                }
            }

            @Override
            public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
            }

            @Override
            public void surfaceDestroyed(SurfaceHolder holder) {
                cam.stop();
            }
        });
        cam_view.setVisibility(View.VISIBLE);
    }

    /**
     * define the behaviour of the back button
     */
    @Override
    public void onBackPressed() {
        Intent intent = new Intent(this, WelcomeScreen.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
        startActivityIfNeeded(intent,0);
        finish();
    }

}
