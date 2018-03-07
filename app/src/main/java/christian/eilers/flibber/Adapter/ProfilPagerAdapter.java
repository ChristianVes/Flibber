package christian.eilers.flibber.Adapter;

import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;

import christian.eilers.flibber.ProfilAndWgs.ProfilFragment;
import christian.eilers.flibber.ProfilAndWgs.GroupFragment;

public class ProfilPagerAdapter extends FragmentPagerAdapter {
    private int NUM_ITEMS = 2;

    public ProfilPagerAdapter(FragmentManager fm) {
        super(fm);
    }

    @Override
    public Fragment getItem(int position) {
        switch (position) {
            case 0:
                return new ProfilFragment();
            case 1:
                return new GroupFragment();
            default:
                return null;
        }
    }

    @Override
    public int getCount() {
        return NUM_ITEMS;
    }

    // Returns the page title for the top indicator
    @Override
    public CharSequence getPageTitle(int position) {
        switch (position) {
            case 0:
                return "Profil";
            case 1:
                return "Gruppen";
            default:
                return null;
        }
    }
}
