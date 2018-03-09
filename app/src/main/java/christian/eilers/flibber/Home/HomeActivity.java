package christian.eilers.flibber.Home;

import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.design.widget.BottomNavigationView;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.view.ViewPager;
import android.support.v7.app.AppCompatActivity;
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
import christian.eilers.flibber.Models.User;
import christian.eilers.flibber.R;
import christian.eilers.flibber.Utils.LocalStorage;

public class HomeActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);
        groupID = LocalStorage.getGroupID(this);

        mView = findViewById(R.id.container);
        bottomNavigationView = findViewById(R.id.bnve);
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

        /*
        Lokales Laden der User-Liste aus dem Cache
        Unnötig, da sowieso jedes mal die get() Methode aufgerufen wird, bevor die Fragmente initialisiert werden
        if(savedInstanceState != null) {
            users = (HashMap<String, User>) savedInstanceState.getSerializable("users");
        }
        */

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

    /*
    Unnötig, siehe oben....
    @Override
    protected void onSaveInstanceState(Bundle outState) {
        if(users != null)
            outState.putSerializable("users", users);
        super.onSaveInstanceState(outState);
    }
    */

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
    private FragmentPagerAdapter adapterViewPager;
    private ViewPager mView;
    private ProgressBar progressBar;
    private Query usersQuery;
    private String groupID;
    private HashMap<String, User> users;

    private final String GROUPS = "groups";
    private final String USERS = "users";
}
