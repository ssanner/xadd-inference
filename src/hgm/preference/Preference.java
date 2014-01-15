package hgm.preference;

/**
 * Created by Hadi Afshar.
 * Date: 19/12/13
 * Time: 11:29 PM
 */
public class Preference {
    public enum Choice {FIRST, SECOND, EQUAL}
    private int itemId1;
    private int itemId2;
    private Choice preferenceChoice;

    public Preference(int itemId1, int itemId2, Choice preferenceChoice) {
        this.itemId1 = itemId1;
        this.itemId2 = itemId2;
        this.preferenceChoice = preferenceChoice;
    }

    public int getItemId1() {
        return itemId1;
    }

    public int getItemId2() {
        return itemId2;
    }

    public Choice getPreferenceChoice() {
        return preferenceChoice;
    }

    @Override
    public String toString() {
        return "#" + itemId1 + (preferenceChoice == Choice.FIRST ? ">" : (preferenceChoice == Choice.EQUAL ? "=" : "<")) +
                "#" + itemId2;
    }
}
