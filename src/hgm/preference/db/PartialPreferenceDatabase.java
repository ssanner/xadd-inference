package hgm.preference.db;

import hgm.preference.Preference;

import java.util.List;

/**
 * Created by Hadi Afshar.
 * Date: 28/01/14
 * Time: 1:47 AM
 * <p>
 * Wraps another data base and only returns the first 'numberOfPreferences' preferences of the wrapped database. Note that the number of items is as the wrapped database.
 */
public class PartialPreferenceDatabase extends  PreferenceDatabase {
    private PreferenceDatabase wrappedDatabase;
    int numberOfPreferences;

    public PartialPreferenceDatabase(PreferenceDatabase wrappedDatabase, int numberOfPreferences) {
        super(wrappedDatabase.getPrior());
        this.wrappedDatabase = wrappedDatabase;
        this.numberOfPreferences = numberOfPreferences;
    }

    @Override
    public int getNumberOfParameters() {
        return wrappedDatabase.getNumberOfParameters();
    }

    @Override
    public int getNumberOfItems() {
        return wrappedDatabase.getNumberOfItems();
    }

    @Override
    public List<Preference> getObservedDataPoints() {
        return wrappedDatabase.getObservedDataPoints().subList(0, numberOfPreferences);
    }

    @Override
    public Double[] getItemAttributeValues(int itemId) {
        return wrappedDatabase.getItemAttributeValues(itemId);
    }

    @Override
    public double[] getAuxiliaryWeightVector() {
        return wrappedDatabase.getAuxiliaryWeightVector();
    }
}
