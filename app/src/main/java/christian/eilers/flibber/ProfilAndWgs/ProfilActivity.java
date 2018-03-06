package christian.eilers.flibber.ProfilAndWgs;

import android.os.Bundle;
import android.support.design.widget.TabLayout;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.view.ViewPager;
import android.support.v7.app.AppCompatActivity;

import christian.eilers.flibber.Adapter.WgProfilAdapter;
import christian.eilers.flibber.R;

public class ProfilActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_wgs_and_profil);
        initializeViews();
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
