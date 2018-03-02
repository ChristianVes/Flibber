package christian.eilers.flibber.Adapter;

import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;

import christian.eilers.flibber.ProfilAndWgs.ProfilFragment;
import christian.eilers.flibber.ProfilAndWgs.WgFragment;

public class WgProfilAdapter extends FragmentPagerAdapter {
    private int NUM_ITEMS = 2;

    public WgProfilAdapter(FragmentManager fm) {
        super(fm);
    }

    @Override
    public Fragment getItem(int position) {
        switch (position) {
            case 0:
                return new ProfilFragment();
            case 1:
                return new WgFragment();
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
                return "WG's";
            default:
                return null;
        }
    }
}
