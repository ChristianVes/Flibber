package christian.eilers.flibber;

import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;

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
                return PutzplanFragment.newInstance();
            case 1:
                return PutzplanFragment.newInstance();
            case 2:
                return PutzplanFragment.newInstance();
            case 3:
                return PutzplanFragment.newInstance();
            case 4:
                return PutzplanFragment.newInstance();
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
