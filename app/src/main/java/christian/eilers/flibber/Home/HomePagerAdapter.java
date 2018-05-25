package christian.eilers.flibber.Home;

import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;

import christian.eilers.flibber.Home.Finance.FinanceFragment;
import christian.eilers.flibber.Home.HomeFragment;
import christian.eilers.flibber.Home.TaskFragment;
import christian.eilers.flibber.Home.SettingsFragment;
import christian.eilers.flibber.Home.ShoppingFragment;

public class HomePagerAdapter extends FragmentPagerAdapter {
    private static int NUM_ITEMS = 4;

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
                return new HomeFragment2();
            case 1:
                return new FinanceFragment();
            case 2:
                return new ShoppingFragment();
            case 3:
                return new TaskFragment();
            default:
                return null;
        }
    }

    // Returns the page title for the top indicator
    @Override
    public CharSequence getPageTitle(int position) {
        switch (position) {
            case 0:
                return "Pinnwand";
            case 1:
                return "Finanzen";
            case 2:
                return "Einkaufen";
            case 3:
                return "Agenda";
            default:
                return null;
        }
    }
}
