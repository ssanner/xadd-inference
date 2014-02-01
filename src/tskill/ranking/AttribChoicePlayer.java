package tskill.ranking;

import tskill.jskills.IPlayer;

/**
 * Created by Hadi Afshar.
 * Date: 25/01/14
 * Time: 11:26 PM
 */
@Deprecated
public class AttribChoicePlayer<A, C> implements IPlayer{
    A attribute;
    C choice;
    public AttribChoicePlayer(A attribute, C choice) {
        this.attribute = attribute;
        this.choice = choice;
    }

    @Override
    public String toString() {
        return attribute + ":" + choice;
    }
}
