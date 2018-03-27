package christian.eilers.flibber.Home;

import android.app.Activity;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.Switch;

import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QuerySnapshot;

import java.util.ArrayList;
import java.util.HashMap;

import christian.eilers.flibber.Adapter.BeteiligteAdapter;
import christian.eilers.flibber.Models.User;
import christian.eilers.flibber.R;
import christian.eilers.flibber.Utils.LocalStorage;

public class NewTaskActivity extends AppCompatActivity implements View.OnFocusChangeListener {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_new_task);
        initializeViews();
        initializeVariables();
    }

    private void initializeViews() {
        toolbar = findViewById(R.id.toolbar);
        et_name = findViewById(R.id.input_title);
        et_frequenz = findViewById(R.id.input_frequenz);
        et_points = findViewById(R.id.input_points);
        switch_order = findViewById(R.id.switch_order);
        recView_beteiligte = findViewById(R.id.recView_beteiligte);
        progressBar = findViewById(R.id.progressBar);
        progressBar.setVisibility(View.VISIBLE);

        et_name.setOnFocusChangeListener(this);
        et_points.setOnFocusChangeListener(this);
        et_frequenz.setOnFocusChangeListener(this);

        recView_beteiligte.setHasFixedSize(true);
        recView_beteiligte.setLayoutManager(new LinearLayoutManager(this));

        setSupportActionBar(toolbar); // Toolbar als Actionbar setzen
    }

    // Initialize variables
    private void initializeVariables() {
        userID = LocalStorage.getUserID(this);
        groupID = LocalStorage.getGroupID(this);
        db = FirebaseFirestore.getInstance();

        progressBar.setVisibility(View.VISIBLE);

        db.collection(GROUPS).document(groupID).collection(USERS).get().addOnSuccessListener(new OnSuccessListener<QuerySnapshot>() {
            @Override
            public void onSuccess(QuerySnapshot documentSnapshots) {
                retrieveUsers(documentSnapshots);
                ArrayList<User> userList = new ArrayList<>(users.values());
                adapter_beteiligte = new BeteiligteAdapter(userList);
                recView_beteiligte.setAdapter(adapter_beteiligte);
                progressBar.setVisibility(View.GONE);
            }
        });
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

    @Override
    public void onFocusChange(View view, boolean hasFocus) {
        if (et_frequenz.hasFocus() || et_points.hasFocus() || et_name.hasFocus()) return;
        if (!hasFocus) {
            InputMethodManager inputMethodManager = (InputMethodManager)getSystemService(Activity.INPUT_METHOD_SERVICE);
            inputMethodManager.hideSoftInputFromWindow(view.getWindowToken(), 0);
        }
    }

    private Toolbar toolbar;
    private EditText et_name, et_frequenz, et_points;
    private Switch switch_order;
    private RecyclerView recView_beteiligte;
    private BeteiligteAdapter adapter_beteiligte;
    private ProgressBar progressBar;

    private String userID, groupID;
    private FirebaseFirestore db;
    private HashMap<String, User> users;

    private final String GROUPS = "groups";
    private final String USERS = "users";

}
