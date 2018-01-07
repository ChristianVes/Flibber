package christian.eilers.flibber.Adapter;

import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;

import christian.eilers.flibber.ProfilAndWgs.EinladungenFragment;
import christian.eilers.flibber.ProfilAndWgs.ProfilFragment;
import christian.eilers.flibber.ProfilAndWgs.WgFragment;

public class WgProfilAdapter extends FragmentPagerAdapter {
    private int NUM_ITEMS = 3;

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
            case 2:
                return new EinladungenFragment();
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
            case 2:
                return "Einladungen";
            default:
                return null;
        }
    }
}
