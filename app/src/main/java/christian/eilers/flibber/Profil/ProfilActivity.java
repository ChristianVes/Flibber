package christian.eilers.flibber.Profil;

import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.design.widget.BottomNavigationView;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.view.ViewPager;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.ProgressBar;

import com.google.firebase.auth.FirebaseAuth;
import com.ittianyu.bottomnavigationviewex.BottomNavigationViewEx;

import christian.eilers.flibber.Adapter.ProfilPagerAdapter;
import christian.eilers.flibber.LoginActivity;
import christian.eilers.flibber.R;
import christian.eilers.flibber.Utils.LocalStorage;

public class ProfilActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profil);
        auth = FirebaseAuth.getInstance();
        initializeViews();
    }

    private void initializeViews() {
        viewPager = findViewById(R.id.container);
        progressBar = findViewById(R.id.progressBar);
        bottomNavigationView = findViewById(R.id.bnve);
        toolbar = findViewById(R.id.profil_toolbar);

        setSupportActionBar(toolbar); // Toolbar als Actionbar setzen

        // Das "gecheckte" Item in der Bottom Navigation View anpassen, je nachdem, welches Fragment gerade aktiv ist
        viewPager.addOnPageChangeListener(new ViewPager.OnPageChangeListener() {
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
        viewPager.setOffscreenPageLimit(2);

        setBottomNavigationBar(bottomNavigationView);
        bottomNavigationView.setOnNavigationItemSelectedListener(new BottomNavigationView.OnNavigationItemSelectedListener() {
            @Override
            public boolean onNavigationItemSelected(@NonNull MenuItem item) {
                item.setChecked(true);
                switch (item.getItemId()) {
                    case R.id.ic_locked:
                        viewPager.setCurrentItem(0);
                        getSupportActionBar().setTitle("???");
                        break;
                    case R.id.ic_home:
                        viewPager.setCurrentItem(1);
                        getSupportActionBar().setTitle("Gruppen");
                        break;
                    case R.id.ic_profil:
                        viewPager.setCurrentItem(2);
                        getSupportActionBar().setTitle("Profil");
                        break;
                }

                return false;
            }
        });

        bottomNavigationView.setupWithViewPager(viewPager);

        adapter = new ProfilPagerAdapter(getSupportFragmentManager());
        viewPager.setAdapter(adapter);
        viewPager.setCurrentItem(1);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_profil, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_logout:
                auth.signOut();
                LocalStorage.setData(this, null, null, null, null);
                Intent login = new Intent(ProfilActivity.this, LoginActivity.class);
                startActivity(login);
                overridePendingTransition(R.anim.fade_in, R.anim.fade_out);
                finish();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    // Allgemeine Einstellungen für Bottom Navigation View
    private void setBottomNavigationBar(BottomNavigationViewEx bottomView) {
        bottomView.enableAnimation(false);
        bottomView.enableShiftingMode(false);
        bottomView.enableItemShiftingMode(false);
        bottomView.setIconSize(32, 32);
        bottomView.setCurrentItem(1);
    }

    private ViewPager viewPager;
    private Toolbar toolbar;
    private ProgressBar progressBar;
    private BottomNavigationViewEx bottomNavigationView;
    private FragmentPagerAdapter adapter;

    private FirebaseAuth auth;
}