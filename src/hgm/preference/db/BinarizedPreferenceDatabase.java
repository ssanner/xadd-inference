package hgm.preference.db;

import com.sun.javafx.collections.transformation.SortedList;
import hgm.asve.BidirectionalMap;
import hgm.preference.Preference;
import util.Pair; //Since I need hash based equality...

import java.util.*;

/**
 * Created by Hadi Afshar.
 * Date: 29/01/14
 * Time: 4:06 AM
 */
public class BinarizedPreferenceDatabase implements DiscretePreferenceDatabase {
    private static final double ZERO = 0d;
    private static final double ONE = 1d;
    private final static Collection<Double> binaryChoices = Arrays.asList(ZERO, ONE);
    DiscretePreferenceDatabase innerDb;

    int numNewAttribs;

    List<Double[]> newItems;

    public BinarizedPreferenceDatabase(DiscretePreferenceDatabase innerDb) {
        this.innerDb = innerDb;

        numNewAttribs = 0;
        for (int inAttId = 0; inAttId < innerDb.getNumberOfAttributes(); inAttId++) {
            Integer choices = innerDb.getAttribChoices(inAttId).size();
            numNewAttribs += (choices == 2) ? 1 : choices; //for binary vars just one var and for others as much as choices exist...
        }

        // initialize new Items:
        newItems = new ArrayList<Double[]>();
        for (int itemId = 0; itemId < innerDb.getNumberOfItems(); itemId++) {
            Double[] newItem = new Double[this.numNewAttribs];
            Arrays.fill(newItem, ZERO);// in case I decide to change the negative binary indicator...

            newItems.add(newItem);
        }


        //fill attribs of the new items
        int c = 0;
        for (int innerAttribId = 0; innerAttribId < innerDb.getNumberOfAttributes(); innerAttribId++) {
            List<Double> sortedAttribChoices = new ArrayList<Double>(innerDb.getAttribChoices(innerAttribId));
            Collections.sort(sortedAttribChoices);


            int skip = sortedAttribChoices.size() == 2 ? 1 : sortedAttribChoices.size();

            for (int itemId = 0; itemId < innerDb.getNumberOfItems(); itemId++) {

                Double[] innerItem = innerDb.getItemAttributeValues(itemId);
                Double innerAttrib = innerItem[innerAttribId];

                Integer d = sortedAttribChoices.indexOf(innerAttrib);

                if (sortedAttribChoices.size() == 2) { //binary
                    newItems.get(itemId)[c] = (double)d;//innerAttrib;
                } else {
                    newItems.get(itemId)[c + d] = ONE;
                }
            }

            c+=skip;
       }

    }

    @Override
    public Collection<Double> getAttribChoices(Integer attribId) {
        return binaryChoices; //everything is binary
    }

    @Override
    public int getNumberOfAttributes() {
        return numNewAttribs;
    }

    @Override
    public int getNumberOfItems() {
        return innerDb.getNumberOfItems();
    }

    @Override
    public List<Preference> getPreferenceResponses() {
        return innerDb.getPreferenceResponses();
    }

    @Override
    public Double[] getItemAttributeValues(int itemId) {
        return newItems.get(itemId);
    }

    @Override
    public double[] getAuxiliaryWeightVector() {
        return null;
    }
}

/*
public class BinarizedPreferenceDatabase implements DiscretePreferenceDatabase {
    private static final double ZERO = 0d;
    private static final double ONE = 1d;
    private final static Collection<Double> binaryChoices = Arrays.asList(ZERO, ONE);
    DiscretePreferenceDatabase innerDb;

    BidirectionalMap<Integer, Integer> innerToOuterBinBinBijection;

int numNewAttribs = 0;

List<Double[]> newItems;

//this could be implemented much much easier with no need to maps...
public BinarizedPreferenceDatabase(DiscretePreferenceDatabase innerDb) {
        this.innerDb = innerDb;

innerToOuterBinBinBijection = new BidirectionalMap<Integer, Integer>();

BidirectionalMap<Pair<Integer , Double >, Integer > innerToOuterMultChoiceBijection;
innerToOuterMultChoiceBijection = new BidirectionalMap<Pair<Integer, Double>, Integer>();

int newId = 0;
for (int innerAttribId = 0; innerAttribId < innerDb.getNumberOfAttributes(); innerAttribId++) {
        Collection<Double> innerAttribChoices = innerDb.getAttribChoices(innerAttribId);

if (innerAttribChoices.size() == 2) {
        innerToOuterBinBinBijection.put(innerAttribId, newId++); //binary values are directly mapped...
} else { //each non-binary attrib with n choices is mapped to n new binary vars although in fact n-1 vars are needed. todo should I use n-1 vars?
        for (Double innerAttribChoice : innerAttribChoices) {
        innerToOuterMultChoiceBijection.put(new Pair<Integer, Double>(innerAttribId, innerAttribChoice), newId++);
}
        }
        }

        numNewAttribs = newId;

// make items:
newItems = new ArrayList<Double[]>();
for (int innerItemId = 0; innerItemId < innerDb.getNumberOfItems(); innerItemId++) {
        Double[] innerItem = innerDb.getItemAttributeValues(innerItemId);

Double[] newItem = new Double[numNewAttribs];
Arrays.fill(newItem, ZERO);// in case I decide to change the negative binary indicator...

int c = 0;
for (int innerAttribId = 0; innerAttribId < innerItem.length; innerAttribId++) {
        Collection<Double> attribChoices = innerDb.getAttribChoices(innerAttribId);
if (attribChoices.size() == 2) {
        newItem[c++] = innerItem[innerAttribId];
} else {
        Double inAttribValue = innerItem[innerAttribId];
Integer outerId = innerToOuterMultChoiceBijection.getValue(new Pair<Integer, Double>(innerAttribId, inAttribValue));
newItem[outerId] = ONE;
}
        }

        newItems.add(newItem);
}

        }

@Override
public Collection<Double> getAttribChoices(Integer attribId) {
        Integer innerId = innerToOuterBinBinBijection.getKey(attribId);
if (innerId != null) return innerDb.getAttribChoices(innerId);

//should be in the other list, no need to check....
return binaryChoices;
}

@Override
public int getNumberOfAttributes() {
        return numNewAttribs;
}

@Override
public int getNumberOfItems() {
        return innerDb.getNumberOfItems();
}

@Override
public List<Preference> getPreferenceResponses() {
        return innerDb.getPreferenceResponses();
}

@Override
public Double[] getItemAttributeValues(int itemId) {
        return newItems.get(itemId);
}

@Override
public double[] getAuxiliaryWeightVector() {
        return null;
}
        }

 */
