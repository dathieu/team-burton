package ch.epfl.sweng.partyup;

import android.content.Context;
import android.content.SharedPreferences;

import ch.epfl.sweng.partyup.dbstore.Party;


public class SharedAndSavedData {

    public static Party connectedParty=null;
    public static String connectedPartyID=null;

    /**
     * read the system to find if we were connected to a party
     *
     * @param context a context to access resources.
     * @return the partyId or null if we were not connected
     */
    public static String getSavedConnectedPartyId(Context context){
        SharedPreferences sharedPref = context.getSharedPreferences(
                context.getString(R.string.data_save_filename), Context.MODE_PRIVATE);

        return sharedPref.getString(context.getString(R.string.key_pref_connected_party),null);
    }

    /**
     * read the system to find if we were host of a previously connected party
     *
     * @param context a context to access resources.
     * @return true if we were host of the previously connected party
     */
    public static boolean getSavedConnectedIsHost(Context context){
        SharedPreferences sharedPref = context.getSharedPreferences(
                context.getString(R.string.data_save_filename), Context.MODE_PRIVATE);

        return sharedPref.getBoolean(context.getString(R.string.key_pref_is_host),false);
    }

    /**
     * write the system with info about the party we are connected
     *
     * @param context a context to access resources.
     * @param partyId the ID if the party
     * @param isHost are we hosting the party?
     * @return true if the write succeded
     */
    public static boolean setSavedConnectedInfo(Context context, final String partyId, final boolean isHost){
        SharedPreferences.Editor sharedPrefEdit = context.getSharedPreferences(
                context.getString(R.string.data_save_filename), Context.MODE_PRIVATE).edit();

        sharedPrefEdit.putString(context.getString(R.string.key_pref_connected_party),partyId);
        sharedPrefEdit.putBoolean(context.getString(R.string.key_pref_is_host),isHost);

        return sharedPrefEdit.commit();
    }


}
