package christian.eilers.flibber.Adapter;

import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;

import christian.eilers.flibber.Home.PutzplanFragment;
import christian.eilers.flibber.Profil.GroupFragment;
import christian.eilers.flibber.Profil.ProfilFragment;

public class ProfilPagerAdapter extends FragmentPagerAdapter {
    private int NUM_ITEMS = 3;

    public ProfilPagerAdapter(FragmentManager fm) {
        super(fm);
    }

    @Override
    public Fragment getItem(int position) {
        switch (position) {
            case 0:
                return new PutzplanFragment();
            case 1:
                return new GroupFragment();
            case 2:
                return new ProfilFragment();
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
                return "???";
            case 1:
                return "Gruppen";
            case 2:
                return "Profil";
            default:
                return null;
        }
    }
}
