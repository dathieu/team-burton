package ch.epfl.sweng.partyup.SpotDBTests;

import android.content.Intent;
import android.support.test.rule.ActivityTestRule;
import android.support.test.runner.AndroidJUnit4;

import com.android.volley.AuthFailureError;
import com.android.volley.toolbox.JsonObjectRequest;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.runner.RunWith;

import ch.epfl.sweng.partyup.SearchActivityForTests;
import ch.epfl.sweng.partyup.SpotAuth;
import ch.epfl.sweng.partyup.SpotDB.SpotDBRequestQueue;
import ch.epfl.sweng.partyup.SpotDB.SpotDBResponseHandler;
import ch.epfl.sweng.partyup.SpotDB.SpotDBResponseType;
import ch.epfl.sweng.partyup.SpotDB.SpotDBSearchHandler;
import ch.epfl.sweng.partyup.SpotDB.SpotDBTypes;
import ch.epfl.sweng.partyup.testhelpers.SpotifyAuthAndDBCoHelper;

import static junit.framework.Assert.fail;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

@RunWith(AndroidJUnit4.class)

public class SpotDBSearchHandlerTest {

    private String QUERY = "Californication";
    private static Intent intent = new Intent();
    private static SearchActivityForTests act;


    @ClassRule
    public static final ActivityTestRule<SearchActivityForTests> mActivityRule =
            new ActivityTestRule<>(SearchActivityForTests.class, true, false);

    @BeforeClass
    public static void authentication() {
        SpotAuth.logOut();
        SpotifyAuthAndDBCoHelper.DBCo();
        mActivityRule.launchActivity(intent);
        try {
            SpotifyAuthAndDBCoHelper.startActivity();
        } catch (Exception e) {
            fail();
        }


    }

    @Test
    public void responseHasRightFields() {
        act = mActivityRule.launchActivity(intent);
        while (!SpotAuth.isAuthenticated()) {
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        act.responseHandler = new SpotDBResponseHandler() {
            @Override
            public void onSpotDBResponse(JSONObject response, SpotDBResponseType type) {
                JSONObject tracks = new JSONObject();
                try {
                    Object optTracks = response.get("tracks");
                    if (optTracks != JSONObject.NULL && optTracks.getClass() != JSONObject.class) {
                        throw new RuntimeException("field 'total' doesn't have expected type (JSONObject)");
                    } else if (optTracks != JSONObject.NULL && optTracks.getClass() == JSONObject.class) {
                        tracks = (JSONObject) optTracks;
                    }
                } catch (JSONException JSEx) {
                    JSEx.printStackTrace();
                    throw new RuntimeException("No 'tracks' field");
                }

                try {
                    Object total = tracks.get("total");
                    if (total != JSONObject.NULL && total.getClass() != Integer.class) {
                        throw new RuntimeException("field 'total' doesn't have expected type (Integer)");
                    }
                } catch (JSONException JSEx) {
                    JSEx.printStackTrace();
                    throw new RuntimeException("No 'total' field");
                }

                try {
                    Object limit = tracks.get("limit");
                    if (limit != JSONObject.NULL && limit.getClass() != Integer.class) {
                        throw new RuntimeException("field 'limit' doesn't have expected type (Integer)");
                    }
                } catch (JSONException JSEx) {
                    JSEx.printStackTrace();
                    throw new RuntimeException("No 'limit' field");
                }

                try {
                    Object items = tracks.get("items");
                    if (items != JSONObject.NULL && items.getClass() != JSONArray.class) {
                        throw new RuntimeException("field 'items' doesn't have expected type (JSONArray)");
                    }
                } catch (JSONException JSEx) {
                    JSEx.printStackTrace();
                    throw new RuntimeException("No 'items' field");
                }

                try {
                    Object href = tracks.get("href");
                    if (href != JSONObject.NULL && href.getClass() != String.class) {
                        throw new RuntimeException("field 'href' doesn't have expected type (String)");
                    }
                } catch (JSONException JSEx) {
                    JSEx.printStackTrace();
                    throw new RuntimeException("No 'href' field");
                }

                try {
                    Object next = tracks.get("next");
                    if (next != JSONObject.NULL && next.getClass() != String.class) {
                        throw new RuntimeException("field 'next' doesn't have expected type (String)");
                    }
                } catch (JSONException JSEx) {
                    JSEx.printStackTrace();
                    throw new RuntimeException("No 'next' field");
                }

                try {
                    Object previous = tracks.get("previous");
                    if (previous != JSONObject.NULL && previous.getClass() != String.class) {
                        throw new RuntimeException("field 'previous' doesn't have expected type (String)");
                    }
                } catch (JSONException JSEx) {
                    JSEx.printStackTrace();
                    throw new RuntimeException("No 'previous' field");
                }

                try {
                    Object offset = tracks.get("offset");
                    if (offset != JSONObject.NULL && offset.getClass() != Integer.class) {
                        throw new RuntimeException("field 'offset' doesn't have expected type (Integer)");
                    }
                } catch (JSONException JSEx) {
                    JSEx.printStackTrace();
                    throw new RuntimeException("No 'offset' field");
                }
            }
        };

        JsonObjectRequest trackRequest = new SpotDBSearchHandler(act, SpotAuth.getSpotToken()).trackRequest(QUERY, SpotDBTypes.TRACK);
        SpotDBRequestQueue.getInstance(act).addToRequestQueue(trackRequest);

        while (!act.responseReceived) {
            try {
                Thread.sleep(100);
            } catch (InterruptedException inter) {
                inter.printStackTrace();
                throw new RuntimeException("Thread.sleep interrupted");
            }
        }
    }

    @Test
    public void requestHasRightHeaderAndUrl() {
        act = mActivityRule.launchActivity(intent);
        while (!SpotAuth.isAuthenticated()) {
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }


        JsonObjectRequest trackRequest = new SpotDBSearchHandler(act, SpotAuth.getSpotToken()).trackRequest(QUERY, SpotDBTypes.TRACK);
        try {
            String auth = trackRequest.getHeaders().get("Authorization");
            String url = trackRequest.getUrl();
            assertThat(auth, is("Bearer " + SpotAuth.getSpotToken()));
            assertThat(url, is("https://api.spotify.com/v1/search" + "?q=" + QUERY + "&type=track"));
        } catch (AuthFailureError authfail) {
            authfail.printStackTrace();
            throw new RuntimeException("Could not access headers");
        }
    }

    @Test(expected = RuntimeException.class)
    public void cannotMakeRequestWithInvalidFields() {
        mActivityRule.launchActivity(intent);

        while (!SpotAuth.isAuthenticated()) {
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        try {
            JsonObjectRequest trackRequestNullToken = new SpotDBSearchHandler(act, null).trackRequest(QUERY, SpotDBTypes.TRACK);
        } catch (IllegalArgumentException ilArg) {
            try {
                JsonObjectRequest trackRequestEmptyToken = new SpotDBSearchHandler(act, "").trackRequest(QUERY, SpotDBTypes.TRACK);
            } catch (IllegalArgumentException ilArg2) {
                try {
                    JsonObjectRequest trackRequestEmptyQuery = new SpotDBSearchHandler(act, SpotAuth.getSpotToken()).trackRequest("", SpotDBTypes.TRACK);
                } catch (IllegalArgumentException ilArg3) {
                    try {
                        JsonObjectRequest trackRequestNullQuery = new SpotDBSearchHandler(act, SpotAuth.getSpotToken()).trackRequest(null, SpotDBTypes.TRACK);
                    } catch (IllegalArgumentException ilArg4) {
                        JsonObjectRequest trackRequestNullCaller = new SpotDBSearchHandler(null, SpotAuth.getSpotToken()).trackRequest(null, SpotDBTypes.TRACK);
                    }
                    throw new RuntimeException("No exception when making a request with null query");
                }
                throw new RuntimeException("No exception when making a request with empty query");
            }
            throw new RuntimeException("No exception when making a request with empty token");
        }
        throw new RuntimeException("No exception when making a request with null token");
    }




}
