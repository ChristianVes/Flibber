package christian.eilers.flibber.Adapter;

import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;

import christian.eilers.flibber.Home.FinanzenFragment;
import christian.eilers.flibber.Home.PutzplanFragment;
import christian.eilers.flibber.Home.SettingsFragment;

public class HomePagerAdapter extends FragmentPagerAdapter {
    private static int NUM_ITEMS = 5;

    public HomePagerAdapter(FragmentManager fragmentManager) {
        super(fragmentManager);
    }

    // Returns total number of pages
    @Override
    public int getCount() {
        return NUM_ITEMS;
    }

    // Returns the fragment to display for that page
    @Override
    public Fragment getItem(int position) {
        switch (position) {
            case 0:
                return new PutzplanFragment();
            case 1:
                return new PutzplanFragment();
            case 2:
                return new FinanzenFragment();
            case 3:
                return new PutzplanFragment();
            case 4:
                return new SettingsFragment();
            default:
                return null;
        }
    }

    // Returns the page title for the top indicator
    @Override
    public CharSequence getPageTitle(int position) {
        switch (position) {
            case 0:
                return "Putzplan";
            case 1:
                return "Einkaufsliste";
            case 2:
                return "Home";
            case 3:
                return "Finanzen";
            case 4:
                return "Settings";
            default:
                return null;
        }
    }
}
