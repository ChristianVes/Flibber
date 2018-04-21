package christian.eilers.flibber.Home;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.design.widget.BottomNavigationView;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.view.ViewPager;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.MenuItem;
import android.view.View;
import android.widget.ProgressBar;

import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.EventListener;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QuerySnapshot;
import com.ittianyu.bottomnavigationviewex.BottomNavigationViewEx;

import java.util.HashMap;

import christian.eilers.flibber.Adapter.HomePagerAdapter;
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
        if(groupID == null || groupName == null) {
            Intent main = new Intent(this, MainActivity.class);
            main.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
            startActivity(main);
            finish();
            return;
        }

        mView = findViewById(R.id.container);
        bottomNavigationView = findViewById(R.id.bnve);
        toolbar = findViewById(R.id.toolbar);
        progressBar = findViewById(R.id.progressBar);
        progressBar.setVisibility(View.VISIBLE);

        // Das "gecheckte" Item in der Bottom Navigation View anpassen, je nachdem, welches Fragment gerade aktiv ist
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
        mView.setOffscreenPageLimit(5);

        /*
        Bottom Navigation View initialisieren und auf Home Screen setzen
        Fragment wechseln, je nachdem welches Item in der Bottom Navigation View angegklickt wurde
        */
        setBottomNavigationBar(bottomNavigationView);

        setSupportActionBar(toolbar);

        /*
        Lade Liste aller User dieser Gruppe einmalig und initalisiere anschließend die Fragmente
        Anschließend wird die Userliste über einen Listener up-to-date gehalten
        Der Listener ist an den Lifecycle der Activity gebunden
         */
        usersQuery = FirebaseFirestore.getInstance().collection(GROUPS).document(groupID).collection(USERS);
        usersQuery.get().addOnSuccessListener(new OnSuccessListener<QuerySnapshot>() {
            @Override
            public void onSuccess(QuerySnapshot documentSnapshots) {
                retrieveUsers(documentSnapshots);
                adapterViewPager = new HomePagerAdapter(getSupportFragmentManager());
                bottomNavigationView.setOnNavigationItemSelectedListener(new BottomNavigationView.OnNavigationItemSelectedListener() {
                    @Override
                    public boolean onNavigationItemSelected(@NonNull MenuItem item) {
                        item.setChecked(true);
                        switch (item.getItemId()) {
                            case R.id.ic_putzplan:
                                mView.setCurrentItem(0);
                                getSupportActionBar().setTitle(adapterViewPager.getPageTitle(0));
                                break;
                            case R.id.ic_einkaufsliste:
                                mView.setCurrentItem(1);
                                getSupportActionBar().setTitle(adapterViewPager.getPageTitle(1));
                                break;
                            case R.id.ic_home:
                                mView.setCurrentItem(2);
                                getSupportActionBar().setTitle(adapterViewPager.getPageTitle(2));
                                break;
                            case R.id.ic_finanzen:
                                mView.setCurrentItem(3);
                                getSupportActionBar().setTitle(adapterViewPager.getPageTitle(3));
                                break;
                            case R.id.ic_settings:
                                mView.setCurrentItem(4);
                                getSupportActionBar().setTitle(groupName);
                                break;
                        }

                        return false;
                    }
                });
                mView.setAdapter(adapterViewPager);
                mView.setCurrentItem(2);
                progressBar.setVisibility(View.GONE);
            }
        });
        usersQuery.addSnapshotListener(HomeActivity.this, new EventListener<QuerySnapshot>() {
            @Override
            public void onEvent(QuerySnapshot documentSnapshots, FirebaseFirestoreException e) {
                retrieveUsers(documentSnapshots);
            }
        });

        bottomNavigationView.setupWithViewPager(mView);
    }

    // Delete the recent Articles from the SharedPreferences for Shopping-List-Notifications
    @Override
    protected void onStart() {
        super.onStart();
        SharedPreferences.Editor editor = getSharedPreferences(NOTIFICATIONS, Context.MODE_PRIVATE).edit();
        editor.putStringSet("TITLES", null);
        editor.putStringSet("DESCRIPTIONS", null);
        editor.putStringSet(SHOPPING, null);
        editor.putStringSet(FINANCES, null);
        editor.putStringSet(TASKS, null);
        editor.apply();
    }

    // Erzeugt eine Userliste mithilfe eines Snapshots aus der Datenbank
    private void retrieveUsers(QuerySnapshot documentSnapshots) {
        HashMap<String, User> userHashMap = new HashMap<>();
        for(DocumentSnapshot doc : documentSnapshots) {
            User user = doc.toObject(User.class);
            userHashMap.put(user.getUserID(), user);
        }
        users = (HashMap<String, User>) userHashMap.clone();
    }

    // Allgemeine Einstellungen für Bottom Navigation View
    private void setBottomNavigationBar(BottomNavigationViewEx bottomView) {
        bottomView.enableAnimation(false);
        bottomView.enableShiftingMode(false);
        bottomView.enableItemShiftingMode(false);
        bottomView.setIconSize(32, 32);
        bottomView.setCurrentItem(2);
    }

    // Liefert Zugriff auf die Userliste innherlab von Fragmenten
    public HashMap<String, User> getUsers() {
        return users;
    }


    private BottomNavigationViewEx bottomNavigationView;
    private Toolbar toolbar;
    private FragmentPagerAdapter adapterViewPager;
    private ViewPager mView;
    private ProgressBar progressBar;
    private Query usersQuery;
    private String groupID, groupName;
    private HashMap<String, User> users;
}
