package ch.epfl.sweng.partyup.dbstore.schemas;


public class Playlist {
    private String name;
    private String spotify_id;
    private String user_id;
    private String track_href;
    private String cover_url;
    public Playlist(String name, String spotify_id, String track_href,String cover_url,String user_id){
        this.name = name;
        this.spotify_id = spotify_id;
        this.track_href = track_href;
        this.cover_url=cover_url;
        this.user_id=user_id;
    }

    public String getName() {
        return name;
    }

    public String getTrack_href() {
        return track_href;
    }

    public String getCover_url() {
        return cover_url;
    }


    public String getSpotify_id() {
        return spotify_id;
    }

    public String getUser_id() {
        return user_id;
    }
}
