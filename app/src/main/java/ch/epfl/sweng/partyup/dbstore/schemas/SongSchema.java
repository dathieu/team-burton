package ch.epfl.sweng.partyup.dbstore.schemas;

public class SongSchema {
    private String name="";
    private String artist_name="";
    private String album_name="";
    private String spotify_id="";
    private String album_url="";
    private long timestamp=0;

    public SongSchema(){}

    public SongSchema(String name, String artist_name, String album_name, String spotify_id, String album_url, long timestamp){
        if(name == null)
            throw new IllegalArgumentException("name must not be null");
        if(artist_name == null)
            artist_name="";
        if(album_name == null)
            album_name="";
        if(album_url == null)
            album_url="";

        this.name = name;
        this.artist_name = artist_name;
        this.album_name = album_name;
        this.spotify_id = spotify_id;
        this.album_url = album_url;
        this.timestamp=timestamp;
    }

    public static SongSchema getSample(){
        SongSchema sample = new SongSchema();
        sample.name="song name";
        sample.artist_name="artist name";
        sample.spotify_id="unique spotify identifier";
        sample.timestamp=1111111111;
        sample.album_url="album url";

        return sample;
    }

    @Override
    public boolean equals(Object obj) {
        if(this == obj)
            return true;
        if(! (obj instanceof SongSchema))
            return false;
        SongSchema other = (SongSchema) obj;

        return this.getAlbum_name().equals(other.getAlbum_name()) &&
                this.getAlbum_url().equals(other.getAlbum_url()) &&
                this.getArtist_name().equals(other.getArtist_name()) &&
                this.getName().equals(other.getName()) &&
                this.getSpotify_id().equals(other.getSpotify_id()) &&
                this.getTimestamp() == other.getTimestamp();
    }

    // getters and setters

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getArtist_name() {
        return artist_name;
    }

    public void setArtist_name(String artist_name) {
        this.artist_name = artist_name;
    }

    public String getAlbum_name() {
        return album_name;
    }

    public void setAlbum_name(String album_name) {
        this.album_name = album_name;
    }

    public String getSpotify_id() {
        return spotify_id;
    }

    public void setSpotify_id(String spotify_id) {
        this.spotify_id = spotify_id;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }

    public String getAlbum_url() {
        return album_url;
    }

    public void setAlbum_url(String album_url) {
        this.album_url = album_url;
    }
}
