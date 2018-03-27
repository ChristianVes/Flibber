package christian.eilers.flibber.Adapter;

import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;

import christian.eilers.flibber.Home.FinanceFragment;
import christian.eilers.flibber.Home.HomeFragment;
import christian.eilers.flibber.Home.TaskFragment;
import christian.eilers.flibber.Home.SettingsFragment;
import christian.eilers.flibber.Home.ShoppingFragment;

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
                return new TaskFragment();
            case 1:
                return new ShoppingFragment();
            case 2:
                return new HomeFragment();
            case 3:
                return new FinanceFragment();
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
