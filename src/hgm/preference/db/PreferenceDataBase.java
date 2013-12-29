package hgm.preference.db;

import java.util.List;

/**
 * Created by Hadi Afshar.
 * Date: 19/12/13
 * Time: 8:33 PM
 */
public interface PreferenceDataBase {
//    String[] getAttributes();
    int getNumberOfAttributes();

    int numberOfItems();

    List<Preference> getPreferenceResponses();

    Double[] getItemAttributeValues(int itemId);
}
