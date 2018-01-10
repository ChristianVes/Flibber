package christian.eilers.flibber.Home;

import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.design.widget.BottomNavigationView;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.view.ViewPager;
import android.support.v7.app.AppCompatActivity;
import android.view.MenuItem;

import com.ittianyu.bottomnavigationviewex.BottomNavigationViewEx;

import christian.eilers.flibber.Adapter.HomePagerAdapter;
import christian.eilers.flibber.R;
import christian.eilers.flibber.Utils.Utils;

public class HomeActivity extends AppCompatActivity {

    private BottomNavigationViewEx bottomNavigationView;
    private FragmentPagerAdapter adapterViewPager;
    private ViewPager mView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);
        Utils.getLocalData(this);

        ////////////////////////////////////////////////////////////////////////
        mView = findViewById(R.id.container);
        bottomNavigationView = findViewById(R.id.bnve);
        ////////////////////////////////////////////////////////////////////////

        mView.setOffscreenPageLimit(5);

        // Bottom Navigation View initialisieren und auf Home Screen setzen
        setBottomNavigationBar(bottomNavigationView);

        // Checked-Item in Bottom Navigation View anpassen, je nachdem, welches Fragment gerade aktiv ist
        mView.addOnPageChangeListener(new ViewPager.OnPageChangeListener() {
            @Override
            public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {
            }

            @Override
            public void onPageSelected(int position) {
                bottomNavigationView.setCurrentItem(position);
            }

            @Override
            public void onPageScrollStateChanged(int state) {
            }
        });

        // Fragment wechseln, je nachdem welches Item der Bottom Navigation View angegklickt wurde
        bottomNavigationView.setOnNavigationItemSelectedListener(new BottomNavigationView.OnNavigationItemSelectedListener() {
            @Override
            public boolean onNavigationItemSelected(@NonNull MenuItem item) {
                item.setChecked(true);
                switch (item.getItemId()) {
                    case R.id.ic_putzplan:
                        mView.setCurrentItem(0);
                        break;
                    case R.id.ic_einkaufsliste:
                        mView.setCurrentItem(1);
                        break;
                    case R.id.ic_home:
                        mView.setCurrentItem(2);
                        break;
                    case R.id.ic_finanzen:
                        mView.setCurrentItem(3);
                        break;
                    case R.id.ic_settings:
                        mView.setCurrentItem(4);
                        break;
                }

                return false;
            }
        });

        adapterViewPager = new HomePagerAdapter(getSupportFragmentManager());
        mView.setAdapter(adapterViewPager);
        mView.setCurrentItem(2);

        bottomNavigationView.setupWithViewPager(mView);
    }


    // Allgemeine Einstellungen für Bottom Navigation View
    private void setBottomNavigationBar(BottomNavigationViewEx bottomView) {
        bottomView.enableAnimation(false);
        bottomView.enableShiftingMode(false);
        bottomView.enableItemShiftingMode(false);
        bottomView.setIconSize(32, 32);
        bottomView.setCurrentItem(2);
    }
}