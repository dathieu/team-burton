package ch.epfl.sweng.partyup.SpotDB;

import org.json.JSONObject;

public interface SpotDBResponseHandler {
    void onSpotDBResponse(JSONObject response, SpotDBResponseType type);
}
