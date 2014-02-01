package hgm.preference.db;

import hgm.preference.Choice;
import hgm.preference.Preference;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Random;

/**
 * Created by Hadi Afshar.
 * Date: 28/01/14
 * Time: 2:24 PM
 * <p>
 *     Randomly flips some preferences form e.g. A < B to B > A
 */
public class ShuffledPreferenceDatabase implements DiscretePreferenceDatabase {
    private DiscretePreferenceDatabase wrappedDb;
    private List<Preference> flippedResponses;

    public ShuffledPreferenceDatabase(DiscretePreferenceDatabase wrappedDb) {

        this.wrappedDb = wrappedDb;

        Random random = new Random();
        List<Preference> wrappedResponses = wrappedDb.getPreferenceResponses();
        flippedResponses = new ArrayList<Preference>(wrappedResponses.size());

        for (int i = 0; i < wrappedResponses.size(); i++) {
            Preference preference = wrappedResponses.get(i);
            if (random.nextBoolean()) {
                flippedResponses.add(preference); //no change
            } else {
                flippedResponses.add(flip(preference)); //flip
            }
        }
    }

    private Preference flip(Preference preference) {
        Choice flippedChoice;
        switch (preference.getPreferenceChoice()){
            case FIRST:
                flippedChoice = Choice.SECOND;
                break;
            case SECOND:
                flippedChoice = Choice.FIRST;
                break;
            case EQUAL:
                flippedChoice = Choice.EQUAL;
                break;
            default:
                throw new RuntimeException("unexpected choice");
        }
        return new Preference(preference.getItemId2(), preference.getItemId1(), flippedChoice);
    }

    @Override
    public Collection<Double> getAttribChoices(Integer attribId) {
        return wrappedDb.getAttribChoices(attribId);
    }

    @Override
    public int getNumberOfAttributes() {
        return wrappedDb.getNumberOfAttributes();
    }

    @Override
    public int getNumberOfItems() {
        return wrappedDb.getNumberOfItems();
    }

    @Override
    public List<Preference> getPreferenceResponses() {
        return flippedResponses;
    }

    @Override
    public Double[] getItemAttributeValues(int itemId) {
        return wrappedDb.getItemAttributeValues(itemId);
    }

    @Override
    public double[] getAuxiliaryWeightVector() {
        return wrappedDb.getAuxiliaryWeightVector();
    }
}
