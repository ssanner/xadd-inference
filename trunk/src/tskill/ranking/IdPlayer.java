package tskill.ranking;

import tskill.jskills.IPlayer;
import tskill.jskills.Rating;

/**
 * Created by Hadi Afshar.
 * Date: 30/01/14
 * Time: 11:05 PM
 */
public class IdPlayer implements IPlayer {

    //NOTE: since more than one IdPlayer can have -1 ids, hash map should not be based on id (and rating)
    private int id;
    private Rating rating;

    public IdPlayer(int id, Rating rating) {
        this.id = id;
        this.rating = rating;
    }

    public Rating getRating() {
        return rating;
    }

    public void updateRating(Rating newRating) {
        this.rating = newRating;
    }

    public int getId() {
        return id;
    }

    @Override
    public String toString() {
        return "IdPlayer{" +
                "id=" + id +
                ", rating=" + rating +
                '}';
    }


}
