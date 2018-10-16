package ch.epfl.sweng.partyup;

import android.app.Activity;
import android.webkit.CookieManager;

import com.spotify.sdk.android.authentication.AuthenticationClient;
import com.spotify.sdk.android.authentication.AuthenticationRequest;
import com.spotify.sdk.android.authentication.AuthenticationResponse;

public class SpotAuth {

    public static final int AUTH_TOKEN_REQUEST_CODE = 0x10;
    private static final String REDIRECT_URI = "partyup://callback";
    private static boolean authenticated = false;
    private static String spotToken = null;


    public static boolean isAuthenticated() {
        return authenticated;
    }

    /**
     * Set the authentication to authenticated
     *
     * @param authenticated will ebe the next state of the authentication
     */
    public static void setAuthenticated(boolean authenticated) {
        SpotAuth.authenticated = authenticated;
    }

    /**
     * Set the spotify authentication token for the whole app
     *
     * @param token the new token
     */
    public static void setSpotToken(String token) {
        spotToken = token;
    }

    /**
     * @return the spotify token
     */
    public static String getSpotToken() {
        return spotToken;
    }

    /**
     * Trigger a spotify authentication request
     *
     * @param caller the caller of the authnatication
     * @param scopes the spotify scopes
     * @param type   the response type you need
     */
    static public void spotRequest(Activity caller, String[] scopes, AuthenticationResponse.Type type) {
        if (caller == null || scopes.length == 0) {
            throw new IllegalArgumentException("Token requested with invalid scopes or null activity");
        }
        AuthenticationRequest.Builder builder = new AuthenticationRequest.Builder("d98c4f3e380346ee8ac0e19fd16e9749", type, REDIRECT_URI);
        builder.setScopes(scopes);
        AuthenticationRequest request = builder.build();
        AuthenticationClient.openLoginActivity(caller, AUTH_TOKEN_REQUEST_CODE, request);

    }

    /**
     * Logout from spotify
     */
    @SuppressWarnings("deprecations")
    static public void logOut() {
        CookieManager cookieManager = CookieManager.getInstance();
        cookieManager.removeAllCookie();
    }

}

