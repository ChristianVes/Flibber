package christian.eilers.flibber.Home;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.design.widget.AppBarLayout;
import android.support.design.widget.BottomNavigationView;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.view.ViewPager;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageView;
import android.widget.ProgressBar;

import com.afollestad.materialdialogs.MaterialDialog;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.EventListener;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QuerySnapshot;
import com.ittianyu.bottomnavigationviewex.BottomNavigationViewEx;

import java.util.HashMap;

import christian.eilers.flibber.MainActivity;
import christian.eilers.flibber.Models.User;
import christian.eilers.flibber.R;
import christian.eilers.flibber.Utils.LocalStorage;
import static christian.eilers.flibber.Utils.Strings.*;

public class HomeActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);
        groupID = LocalStorage.getGroupID(this);
        groupName = LocalStorage.getGroupName(this);
        isInitialized = false;
        if(groupID == null || groupName == null) {
            Intent main = new Intent(this, MainActivity.class);
            main.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
            startActivity(main);
            finish();
            return;
        }

        mView = findViewById(R.id.container);
        bottomNavigationView = findViewById(R.id.bnve);
        appbar = findViewById(R.id.appbar);
        toolbar = findViewById(R.id.toolbar);
        progressBar = findViewById(R.id.progressBar);
        logo = findViewById(R.id.logo);

        // get all users from the current group
        // initialize fragments at the first time
        usersQuery = FirebaseFirestore.getInstance().collection(GROUPS).document(groupID).collection(USERS);
        usersQuery.addSnapshotListener(HomeActivity.this, new EventListener<QuerySnapshot>() {
            @Override
            public void onEvent(QuerySnapshot documentSnapshots, FirebaseFirestoreException e) {
                if (e != null) {
                    Intent main = new Intent(HomeActivity.this, MainActivity.class);
                    main.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                    startActivity(main);
                    finish();
                    return;
                }
                retrieveUsers(documentSnapshots);
                if (!isInitialized) initializeViews();
                isInitialized = true;
            }
        });
    }

    // initialize the fragments & setup the listener for the @bottomNavigationView
    private void initializeViews() {
        // change visibilities
        progressBar.setVisibility(View.GONE);
        logo.setVisibility(View.GONE);
        appbar.setVisibility(View.VISIBLE);
        bottomNavigationView.setVisibility(View.VISIBLE);

        setSupportActionBar(toolbar);

        // adjust checked item in @bottomNavigationView when view page changes
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
        mView.setOffscreenPageLimit(4);
        // initialize bottomNavigationView
        setBottomNavigationBar(bottomNavigationView);
        bottomNavigationView.setupWithViewPager(mView);

        adapterViewPager = new HomePagerAdapter(getSupportFragmentManager());
        bottomNavigationView.setOnNavigationItemSelectedListener(new BottomNavigationView.OnNavigationItemSelectedListener() {
            @Override
            public boolean onNavigationItemSelected(@NonNull MenuItem item) {
                item.setChecked(true);
                switch (item.getItemId()) {
                    case R.id.ic_home:
                        mView.setCurrentItem(0);
                        getSupportActionBar().setTitle(adapterViewPager.getPageTitle(0));
                        break;
                    case R.id.ic_finanzen:
                        mView.setCurrentItem(1);
                        getSupportActionBar().setTitle(adapterViewPager.getPageTitle(1));
                        break;
                    case R.id.ic_einkaufsliste:
                        mView.setCurrentItem(2);
                        getSupportActionBar().setTitle(adapterViewPager.getPageTitle(2));
                        break;
                    case R.id.ic_putzplan:
                        mView.setCurrentItem(3);
                        getSupportActionBar().setTitle(adapterViewPager.getPageTitle(3));
                        break;
                }
                return false;
            }
        });
        mView.setAdapter(adapterViewPager);
        mView.setCurrentItem(0);
        if (users.size() == 1) {
            welcomeDialog();
            // TODO: switch to Settings Activity, Welcome Dialog anpassen / verschieben
        }
    }

    // Show welcome dialog
    private void welcomeDialog() {
        new MaterialDialog.Builder(HomeActivity.this)
                .title("Willkommen")
                .content("Lade als erstes deine Mitbewohner per E-Mail Addresse über das Feld \"Einladen\" " +
                        "in die Gruppe ein. Beachte, du kannst nur Personen einladen, die bereits registriert sind.")
                .positiveText("Okay")
                .show();
    }

    // Delete the recent Notifications from the SharedPreferences
    @Override
    protected void onStart() {
        super.onStart();
        SharedPreferences.Editor editor = getSharedPreferences(NOTIFICATIONS, Context.MODE_PRIVATE).edit();
        editor.putStringSet("TITLES", null);
        editor.putStringSet("DESCRIPTIONS", null);
        editor.putStringSet(SHOPPING, null);
        editor.putStringSet(FINANCES, null);
        editor.putStringSet(FINANCE_DELETED, null);
        editor.putStringSet(TASKS, null);
        editor.apply();
    }

    // produces a map of users for given snapshots
    private void retrieveUsers(QuerySnapshot documentSnapshots) {
        HashMap<String, User> refreshedUsers = new HashMap<>();
        for(DocumentSnapshot doc : documentSnapshots) {
            User user = doc.toObject(User.class);
            refreshedUsers.put(user.getUserID(), user);
        }
        users = (HashMap<String, User>) refreshedUsers.clone();
    }

    // General settings for the @bottomNavigationView
    private void setBottomNavigationBar(BottomNavigationViewEx bottomView) {
        bottomView.enableAnimation(false);
        bottomView.enableShiftingMode(false);
        bottomView.enableItemShiftingMode(false);
        //bottomView.setIconSize(26, 26);
        bottomView.setCurrentItem(0);
    }

    public HashMap<String, User> getUsers() {
        return users;
    }

    private BottomNavigationViewEx bottomNavigationView;
    private Toolbar toolbar;
    private AppBarLayout appbar;
    private FragmentPagerAdapter adapterViewPager;
    private ViewPager mView;
    private ProgressBar progressBar;
    private ImageView logo;

    private Query usersQuery;
    private String groupID, groupName;
    private HashMap<String, User> users;
    private boolean isInitialized = false;
}
