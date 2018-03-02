package christian.eilers.flibber.ProfilAndWgs;

import android.support.design.widget.TabLayout;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.view.ViewPager;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;

import christian.eilers.flibber.Adapter.WgProfilAdapter;
import christian.eilers.flibber.R;
import christian.eilers.flibber.Utils.Utils;

public class WgsAndProfilActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_wgs_and_profil);
        initializeViews();
        Utils.getLocalData(this);
    }

    private void initializeViews() {
        viewPager = findViewById(R.id.viewpager);
        tabLayout = findViewById(R.id.tabs);

        adapter = new WgProfilAdapter(getSupportFragmentManager());
        viewPager.setOffscreenPageLimit(1);
        viewPager.setCurrentItem(2);
        viewPager.setAdapter(adapter);
        tabLayout.setupWithViewPager(viewPager);
    }

    private ViewPager viewPager;
    private FragmentPagerAdapter adapter;
    private TabLayout tabLayout;
}
