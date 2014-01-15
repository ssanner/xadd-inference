package sve.gibbs;

import java.io.File;
import java.util.ArrayList;

public class PreferenceDataset {

    private ArrayList<Double[]> _items;
    private ArrayList<Double[]> _prefs;
    private ArrayList<Double[]> _users;
    private String _pref_path;
    private String _pref_name;
    private int _prefLimit = -1;

    public PreferenceDataset(String itemsPath, String usersPath,
                             String prefsPath) {
        _items = CSVHandler.readcsvDouble(itemsPath);
        _users = CSVHandler.readcsvDouble(usersPath);
        _prefs = CSVHandler.readcsvDouble(prefsPath);
        _pref_path = prefsPath;
        _pref_name = (new File(prefsPath)).getName();
    }

    public PreferenceDataset(String prefsPath) {
        _prefs = CSVHandler.readcsvDouble(prefsPath);
    }

    public PreferenceDataset() {
        _items = new ArrayList<Double[]>();
        _prefs = new ArrayList<Double[]>();
        _users = new ArrayList<Double[]>();
    }

    public Double[] getItem(int i) {
        return _items.get(i);
    }

    public int[] getPreference(int idx) {
        int[] p = new int[_prefs.get(idx).length];
        for (int i = 0; i < _prefs.get(idx).length; i++)
            p[i] = _prefs.get(idx)[i].intValue() - 1; // it is assumed the
        // preferences are
        // indexed from 1

        return p;
    }

    public Double[] getUser(int i) {
        return _users.get(i);
    }

    public int getPreferencesCount() {
        if (_prefLimit > -1 && _prefLimit < _prefs.size()) {
            return _prefLimit;
        }
        return _prefs.size();
    }

    public int getUsersCount() {
        return _users.size();
    }

    public int getItemsCount() {
        return _items.size();
    }

    public int itemsDimension() {
        if (_items.size() < 1) {
            System.err.println("The items dataset is empty!");
        }
        return _items.get(0).length;
    }

    public int usersDimension() {
        if (_users.size() < 1) {
            System.err.println("The users dataset is empty!");
        }
        return _users.get(0).length;
    }

    public String getPreferenceFilename() {
        return _pref_name;
    }

    public String getPreferenceFilepath() {
        return _pref_path;
    }

    public void setPreferenceLimit(int limit) {
        _prefLimit = limit;
    }

    public void selectItemSubset(int m, int... columnIndex) {
        selectItemSubset(m, 1., columnIndex);
    }

    public void selectItemSubset(int m, double pref_percent, int... columnIndex) {
        ArrayList<Integer> _ar = new ArrayList<Integer>();
        if (_items != null) {
            if (m >= _items.size()) {
                Common.println("More than number of items.");
                return;
            }

            int l = _items.size() - 1;
            for (int i = l; i >= m; i--) {
                _items.remove(i);
            }
        }
        for (int i = 0; i < _prefs.size(); i++) {
            for (int j = 0; j < columnIndex.length; j++) {
                if (_prefs.get(i)[columnIndex[j]] > m) {
                    _ar.add(i);
                    break;
                }
            }
        }

        for (int i = _ar.size() - 1; i >= 0; i--) {
            _prefs.remove((int) _ar.get(i));
        }

        int l = (int) ((double) _prefs.size() * pref_percent);
        int i = _prefs.size() - 1;
        while (_prefs.size() > l) {
            _prefs.remove(i--);
        }
    }
}
