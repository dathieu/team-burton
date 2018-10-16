package ch.epfl.sweng.partyup;


import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Button;

import java.util.concurrent.CountDownLatch;

import ch.epfl.sweng.partyup.containers.Tuple;
import ch.epfl.sweng.partyup.dbstore.Connection;
import ch.epfl.sweng.partyup.dbstore.ConnectionProvider;
import ch.epfl.sweng.partyup.dbstore.Party;
import ch.epfl.sweng.partyup.dbstore.listeners.CompletionListener;
import ch.epfl.sweng.partyup.dbstore.schemas.PartySchema;
import ch.epfl.sweng.partyup.dbstore.statemachine.DBResult;
import ch.epfl.sweng.partyup.dbstore.statemachine.DBState;

public class WelcomeScreen extends AppCompatActivity {
    static final int PERMISSIONS_REQUEST = 1;
    private Activity thisActivity;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    /**
     * setup the welcome screen activity when shown.
     * It check for the permission,disable the button or the screen when connecting and loading.
     * And check if we were connected to reconnect automatically.
     */
    @Override
    protected void onResume() {
        super.onResume();
        thisActivity = this;
        setContentView(R.layout.activity_welcome_screen);
        UserActivity.unlock(thisActivity);
        UserActivity.lockForLoading(this);


        if (checkSelfPermission(android.Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED || checkSelfPermission(android.Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{android.Manifest.permission.CAMERA, Manifest.permission.WRITE_EXTERNAL_STORAGE}, PERMISSIONS_REQUEST);
        }

        final Button host = (Button) findViewById(R.id.HostButton);
        final Button guest = (Button) findViewById(R.id.GuestButton);
        final Button memories = (Button) findViewById(R.id.MemoriesButton);

        //initial signin to database
        //if it fail then the database is down and we can stop the activity
        Connection connection = ConnectionProvider.getConnection();
        if (connection.getState() != DBState.SignedIn) {
            host.setEnabled(false);
            guest.setEnabled(false);
            memories.setEnabled(false);
            final CountDownLatch waiter = new CountDownLatch(1);
            connection.signIn(new CompletionListener<DBResult>() {
                @Override
                public void onCompleted(DBResult result) {
                    if (result != DBResult.Success) {
                        finish();
                    }
                    host.setEnabled(true);
                    guest.setEnabled(true);
                    memories.setEnabled(true);
                    checkPreviousConnection();
                }
            });
        } else {
            checkPreviousConnection();
        }
    }

    //Start the HostActivity whenc the hostbutton is clicked. Allow to host a party
    public void StartHostActivity(View view) {
        Intent intent = new Intent(this, HostActivity.class);
        startActivity(intent);
    }

    //Start the qrCodeReader when the guestButton is clicked. Allow to join a party
    public void StartQRCodeReaderActivity(View view) {
        Intent intent = new Intent(this, QRCodeReaderActivity.class);
        startActivity(intent);
    }

    //Start the memories Activity.
    public void startMemoriesActivity(View view) {
        Intent intent = new Intent(this, MemoriesActivity.class);
        startActivity(intent);
    }

    /*
    Allow to reconnect to a party if this party isn't ended.
    */
    private void checkPreviousConnection() {
        final String id = SharedAndSavedData.getSavedConnectedPartyId(this);
        if (id != null) {
            ConnectionProvider.getConnection().connectToParty(id, new CompletionListener<Tuple<DBResult, Party>>() {
                @Override
                public void onCompleted(Tuple<DBResult, Party> result) {
                    switch (result.object1) {
                        case Success:
                            SharedAndSavedData.connectedParty = result.object2;
                            SharedAndSavedData.connectedPartyID = id;
                            SharedAndSavedData.connectedParty.getPartySchema(new CompletionListener<Tuple<DBResult, PartySchema>>() {
                                @Override
                                public void onCompleted(Tuple<DBResult, PartySchema> result) {
                                    if (result.object1 == DBResult.Success) {
                                        if (result.object2.getEnded()) {
                                            SharedAndSavedData.connectedParty = null;
                                            SharedAndSavedData.connectedPartyID = null;
                                            SharedAndSavedData.setSavedConnectedInfo(WelcomeScreen.this, null, false);
                                            UserActivity.unlock(thisActivity);
                                        } else {
                                            boolean isHost = SharedAndSavedData.getSavedConnectedIsHost(WelcomeScreen.this);
                                            Intent intent;
                                            if (isHost) {
                                                intent = new Intent(WelcomeScreen.this, HostActivity.class);
                                            } else {
                                                intent = new Intent(WelcomeScreen.this, GuestActivity.class);
                                            }
                                            startActivity(intent);

                                        }
                                    }
                                }
                            });

                            break;
                        default:
                            SharedAndSavedData.connectedParty = null;
                            SharedAndSavedData.connectedPartyID = null;
                            SharedAndSavedData.setSavedConnectedInfo(WelcomeScreen.this, null, false);
                            UserActivity.unlock(thisActivity);
                            break;
                    }
                }
            });
        } else {
            UserActivity.unlock(thisActivity);
        }
    }

}
