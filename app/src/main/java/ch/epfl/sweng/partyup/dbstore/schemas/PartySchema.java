package ch.epfl.sweng.partyup.dbstore.schemas;

import com.google.firebase.database.Exclude;

import java.util.HashMap;
import java.util.Map;

import ch.epfl.sweng.partyup.dbstore.Tools;

public class PartySchema {
    private String name;
    private long timeStamp;
    private boolean ended = false;

    private Map<String, Boolean> users = new HashMap<>();

    private Map<String, ProposalSchema> proposals = new HashMap<>();
    private Map<String, SongSchema> played_songs = new HashMap<>();

    private SongSchema currentSong= null;

    public PartySchema(){}

    @Exclude
    public static PartySchema getSample(){
        PartySchema sample = new PartySchema();
        sample.name="best party";

        String username = Tools.generateKey(10);

        HashMap<String, Boolean> users = new HashMap<>();
        users.put(username, true);
        sample.setUsers(users);

        HashMap<String, ProposalSchema> proposals = new HashMap<>();
        proposals.put(Tools.generateKey(10), ProposalSchema.getSample(username));
        sample.setProposals(proposals);

        HashMap<String, SongSchema> songs = new HashMap<>();
        songs.put(Tools.generateKey(10), SongSchema.getSample());
        sample.setPlayed_songs(songs);

        return sample;
    }

    // getters and setters


    public long getTimeStamp() {
        return timeStamp;
    }

    public void setTimeStamp(long timeStamp) {
        this.timeStamp = timeStamp;
    }

    public Map<String, Boolean> getUsers() {
        return users;
    }

    public void setUsers(Map<String, Boolean> users) {
        this.users = users;
    }

    public Map<String, ProposalSchema> getProposals() {
        return proposals;
    }

    public void setProposals(Map<String, ProposalSchema> proposals) {
        this.proposals = proposals;
    }

    public Map<String, SongSchema> getPlayed_songs() {
        return played_songs;
    }

    public void setPlayed_songs(Map<String, SongSchema> played_songs) {
        this.played_songs = played_songs;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public boolean getEnded() {
        return ended;
    }

    public void setEnded(boolean ended) {
        this.ended = ended;
    }

    public SongSchema getCurrentSong() {
        return currentSong;
    }

    public void setCurrentSong(SongSchema currentSong) {
        this.currentSong = currentSong;
    }
}
