package ch.epfl.sweng.partyup.dbstore.schemas;

import com.google.firebase.database.Exclude;

import java.util.HashMap;
import java.util.Map;

import ch.epfl.sweng.partyup.dbstore.Connection;

public class ProposalSchema {
    private String song_id;
    private String song_name;
    private String artist_name;
    private Map<String, Boolean> voters;


    public ProposalSchema(){
        voters = new HashMap<>();
    }

    @Exclude
    public static ProposalSchema getSample(Connection connection){
        return  getSample(connection.getUserName());
    }

    @Exclude
    public static ProposalSchema getSample(String userName){
        ProposalSchema proposalSchema = new ProposalSchema();
        proposalSchema.song_id="completely random song id";
        proposalSchema.song_name = "completely random song name";
        proposalSchema.artist_name="completely random artist name";

        HashMap<String, Boolean> voters = new HashMap<>();
        voters.put(userName, true);
        proposalSchema.setVoters(voters);

        return  proposalSchema;
    }

    // for comparison and hashing
    // the identify of a proposal is given by its songId
    // there can be only one proposal for each song

    @Override
    public boolean equals(Object obj) {
        if(! (obj instanceof ProposalSchema))
            return false;
        ProposalSchema other = (ProposalSchema) obj;
        if(! song_id.equals(other.song_id))
            return false;

        return true;
    }

    @Override
    public int hashCode() {
        return song_id.hashCode();
    }

    //Returns the total score of this proposal (upvotes - downvotes)
    public int getScore() {
        int score = 0;
        if(getVoters() != null) {
            Map<String, Boolean> voters = getVoters();
            for (String voter : voters.keySet())
                if (voters.get(voter))
                    score++;
                else
                    score--;
        }
        return score;
    }

    // getters and setters

    public String getSong_id() {
        return song_id;
    }

    public void setSong_id(String song_id) {
        this.song_id = song_id;
    }

    public Map<String, Boolean> getVoters() {
        return voters;
    }

    public void setVoters(Map<String, Boolean> voters) {
        this.voters = voters;
    }

    public String getSong_name() {
        return song_name;
    }

    public void setSong_name(String song_name) {
        this.song_name = song_name;
    }

    public String getArtist_name() {
        return artist_name;
    }

    public void setArtist_name(String artist_name) {
        this.artist_name = artist_name;
    }
}