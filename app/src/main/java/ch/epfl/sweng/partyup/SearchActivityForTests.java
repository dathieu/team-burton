package ch.epfl.sweng.partyup;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;

import com.spotify.sdk.android.authentication.AuthenticationClient;
import com.spotify.sdk.android.authentication.AuthenticationResponse;

import org.json.JSONObject;

import ch.epfl.sweng.partyup.SpotDB.SpotDBResponseHandler;
import ch.epfl.sweng.partyup.SpotDB.SpotDBResponseType;

/**
 * THIS IS A CLASS ONLY USED FOR TESTS
 */
public class SearchActivityForTests extends AppCompatActivity implements SpotDBResponseHandler {
    public Boolean responseReceived = false;
    public SpotDBResponseHandler responseHandler;

    @Override
    public void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        setContentView(R.layout.fragment_search);

        String[] scopes = new String[]{""};
        SpotAuth.spotRequest(this, scopes, AuthenticationResponse.Type.TOKEN);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == SpotAuth.AUTH_TOKEN_REQUEST_CODE) {

            final AuthenticationResponse response = AuthenticationClient.getResponse(resultCode, data);
            if (response.getType() == AuthenticationResponse.Type.TOKEN) {

                SpotAuth.setSpotToken(response.getAccessToken());
                SpotAuth.setAuthenticated(true);
            }
        }
    }

    public void onSpotDBResponse(JSONObject response, SpotDBResponseType type) {
        responseHandler.onSpotDBResponse(response, type);
        responseReceived = true;
    }
    public void onPlaylistClick(View view) {
    }

    public void onClickBackButton(View view) {
    }
}
