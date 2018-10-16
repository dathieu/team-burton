package ch.epfl.sweng.partyup.SpotDB;

import android.util.Log;

import com.android.volley.AuthFailureError;
import com.android.volley.Request;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.Map;

import ch.epfl.sweng.partyup.dbstore.schemas.Playlist;
import ch.epfl.sweng.partyup.dbstore.schemas.SongSchema;


public class SpotDBSearchHandler {
    private static final String TRACK_REQUEST = "https://api.spotify.com/v1/tracks/";
    private static final String SEARCH_ENDPOINT = "https://api.spotify.com/v1/search";
    private static final String CURRENT_PROFILE = "https://api.spotify.com/v1/me";
    private static final String USER_ENDPOINT = "https://api.spotify.com/v1/users/";
    private static final String ADD_TRACK_ENDPOINT = "https://api.spotify.com/v1/me/tracks?ids=";
    //the activity from which the search is performed
    private SpotDBResponseHandler caller;
    //the access token used to authorize the search
    private String accessToken;

    /**
     * Constructor of the searchhandler class
     *
     * @param caller      the caller of this method
     * @param accessToken the spotify acces token
     */
    public SpotDBSearchHandler(SpotDBResponseHandler caller, String accessToken) {
        if (accessToken == null || accessToken.isEmpty()) {
            throw new IllegalArgumentException("Refusing to set invalid access token (null or empty)");
        } else if (caller == null) {
            throw new IllegalArgumentException("Refusing to set null caller");
        }
        this.caller = caller;
        this.accessToken = accessToken;
    }

    /**
     * Set the spotify access token
     *
     * @param accessToken the new token
     */
    public void setAccessToken(String accessToken) {
        if (accessToken == null || accessToken.isEmpty()) {
            throw new IllegalArgumentException("Refusing to set invalid access token (null or empty)");
        }
        this.accessToken = accessToken;
    }

    /**
     * @return the spotify access token
     */
    public String getAccessToken() {
        return accessToken;
    }

    /**
     * Make a generic spotify request
     *
     * @param url          the url of the request
     * @param responseType the expected response type
     * @return a request
     */
    private JsonObjectRequest makeGenericGetRequest(String url, final SpotDBResponseType responseType) {
        Log.e("TOKEN", accessToken);
        if (url == null || url.isEmpty()) {
            throw new IllegalArgumentException("Request with invalid url (null or empty)");
        } else if (caller == null) {
            throw new IllegalArgumentException("Request with null caller");
        } else if (accessToken == null || accessToken.isEmpty()) {
            throw new IllegalArgumentException("Request with invalid access token (null or Empty)");
        } else {
            JsonObjectRequest request = new JsonObjectRequest
                    (Request.Method.GET, url, null, new Response.Listener<JSONObject>() {

                        @Override
                        public void onResponse(JSONObject response) {
                            caller.onSpotDBResponse(response, responseType);
                        }
                    }, new Response.ErrorListener() {

                        @Override
                        public void onErrorResponse(VolleyError error) {
                            error.printStackTrace();
                        }
                    }) {
                @Override
                public Map<String, String> getHeaders() throws AuthFailureError {
                    Map<String, String> headers = new HashMap<>();
                    headers.put("Authorization", "Bearer " + accessToken);
                    return headers;
                }
            };

            return request;
        }
    }

    /**
     * A request to save a song in the user library
     *
     * @param responseType the reponse type
     * @param id           the id of the user
     * @return the request
     */
    public JsonObjectRequest saveToLibraryRequest(final SpotDBResponseType responseType, final String id) {
        Log.e("TOKEN", accessToken);
        String url = ADD_TRACK_ENDPOINT + id;


        if (url == null || url.isEmpty()) {
            throw new IllegalArgumentException("Request with invalid url (null or empty)");
        } else if (caller == null) {
            throw new IllegalArgumentException("Request with null caller");
        } else if (accessToken == null || accessToken.isEmpty()) {
            throw new IllegalArgumentException("Request with invalid access token (null or Empty)");
        } else {
            JsonObjectRequest request = new JsonObjectRequest
                    (Request.Method.PUT, url, null, new Response.Listener<JSONObject>() {

                        @Override
                        public void onResponse(JSONObject response) {
                            caller.onSpotDBResponse(response, responseType);
                        }
                    }, new Response.ErrorListener() {

                        @Override
                        public void onErrorResponse(VolleyError error) {
                            error.printStackTrace();
                        }
                    }) {
                @Override
                public Map<String, String> getHeaders() throws AuthFailureError {
                    Map<String, String> headers = new HashMap<>();
                    headers.put("Accept", "application/json");
                    headers.put("Authorization", "Bearer " + accessToken);

                    return headers;
                }

            };

            return request;
        }
    }

    /**
     * A request to get a simplified specific track
     *
     * @param query the track
     * @param type  the request type
     * @return the request
     */
    public JsonObjectRequest trackRequest(String query, SpotDBTypes type) {
        String url = makeSearchUrl(query, type);
        return makeGenericGetRequest(url, SpotDBResponseType.TRACKS);
    }

    /**
     * A full track request
     *
     * @param id the track id
     * @return the request
     */
    public JsonObjectRequest fullTrackRequest(String id) {
        if (id == null || id.isEmpty()) {
            throw new IllegalArgumentException("Request with invalid query (null or empty)");
        } else {
            String url = TRACK_REQUEST + id;
            return makeGenericGetRequest(url, SpotDBResponseType.FULL_TRACK);
        }
    }

    /**
     * A request for the current user profile
     *
     * @return the request
     */
    public JsonObjectRequest currentProfileRequest() {
        String url = CURRENT_PROFILE;
        return makeGenericGetRequest(url, SpotDBResponseType.PROFILE);
    }

    /**
     * A request to get the tracks from a specific playlist
     *
     * @param playlistId the id of the playlist
     * @param userId     the user id
     * @return the request
     */
    public JsonObjectRequest playlistTracksRequest(String playlistId, String userId) {
        String url = makePlaylistsUrl(userId) + "/" + playlistId + "/tracks";
        Log.e("ANSWER", url);
        return makeGenericGetRequest(url, SpotDBResponseType.TRACKLIST);
    }

    /**
     * A request for the playlists of a user
     *
     * @param userId the user id
     * @return the request
     */
    public JsonObjectRequest userPlaylistsRequest(String userId) {
        if (userId == null || userId.isEmpty()) {
            throw new IllegalArgumentException("Empty or null user id");
        }
        String url = makePlaylistsUrl(userId);
        JsonObjectRequest request = makeGenericGetRequest(url, SpotDBResponseType.PLAYLISTS);
        return request;
    }

    /**
     * Make a valid url to request playlist to spotify
     *
     * @param userId the user id
     * @return the url
     */
    private String makePlaylistsUrl(String userId) {
        return USER_ENDPOINT + userId + "/playlists";
    }

    /**
     * Make a  url for requesting a search to spotify
     *
     * @param query the qery
     * @param type  the request type
     * @return the url
     */
    private String makeSearchUrl(String query, SpotDBTypes type) {
        String queryFormatted = query.replace(" ", "%20");
        String typeString = "";
        switch (type) {
            case ALBUM:
                typeString = "album";
                break;
            case ARTIST:
                typeString = "artist";
                break;
            case PLAYLIST:
                typeString = "playlist";
                break;
            case TRACK:
                typeString = "track";
        }
        return SEARCH_ENDPOINT + "?q=" + queryFormatted + "&type=" + typeString;
    }

    /**
     * Mae an array of songs from a json object
     *
     * @param response     the json object
     * @param fromPlaylist true if it is from a playlist request
     * @return the array of songs
     * @throws JSONException if the json object is not valid
     */
    public static SongSchema[] makeSongArray(JSONObject response, boolean fromPlaylist) throws JSONException {
        int total = (Integer) response.get("total");
        int limit = (Integer) response.get("limit");
        if ((Integer) response.get("total") > 0) {
            int nSongs = (total > limit ? limit : total);
            JSONArray results = (JSONArray) response.get("items");
            SongSchema[] newResults = new SongSchema[nSongs];
            for (int index = 0; index < nSongs; index++) {
                JSONObject song = (fromPlaylist ? results.getJSONObject(index).getJSONObject("track") : results.getJSONObject(index));
                String songName = (String) song.get("name");

                JSONArray artists = (JSONArray) song.get("artists");

                String artistList = "";
                String artistIds = "";
                Log.e("ARTISTS", artists.toString());
                for (int j = 0; j < artists.length(); j++) {
                    if (j == 0) {
                        artistList = artists.getJSONObject(j).get("name").toString();
                        artistIds = artists.getJSONObject(j).get("id").toString();
                    } else {
                        artistList = artistList + ", " + artists.getJSONObject(j).get("name");
                        artistIds = artistIds + ", " + artists.getJSONObject(j).get("id");
                    }
                }


                String albumName = ""; //No album name in simplified track object
                String albumId = ""; //No album id in simplified track object

                String spotifyId = (String) song.get("uri"); //We use the uri and not the id to play the song
                newResults[index] = new SongSchema(songName, artistList, albumName, spotifyId, null, System.currentTimeMillis());
            }
            return newResults;
        } else {
            throw new IllegalArgumentException("No results found ('total' field equal to zero in response)");
        }
    }

    /**
     * Make a plylist array from a json object
     *
     * @param response the json response
     * @return an array of playlist
     * @throws JSONException if the json object is not valid
     */
    public static Playlist[] makePlaylistArray(JSONObject response) throws JSONException {
        int total = (Integer) response.get("total");
        int limit = (Integer) response.get("limit");
        if ((Integer) response.get("total") > 0) {
            int nPlaylist = (total > limit ? limit : total);
            JSONArray results = (JSONArray) response.get("items");
            Playlist[] newResults = new Playlist[nPlaylist];
            for (int i = 0; i < nPlaylist; i++) {
                JSONObject playlist = results.getJSONObject(i);
                String user_id = (String) playlist.getJSONObject("owner").get("id");
                String playlistName = (String) playlist.get("name");
                String imageURL = null;
                JSONArray images = (JSONArray) playlist.get("images");
                if (images.length() > 0) {
                    imageURL = images.getJSONObject(0).get("url").toString();
                }
                JSONObject tracks = playlist.getJSONObject("tracks");
                String href = tracks.get("href").toString();
                String spotifyId = (String) playlist.get("id"); //We use the uri and not the id to play the song
                newResults[i] = new Playlist(playlistName, spotifyId, href, imageURL, user_id);
            }
            return newResults;
        } else {
            return new Playlist[]{};
        }
    }
}
