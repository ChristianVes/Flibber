package christian.eilers.flibber.Home;

import android.app.Activity;
import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.SwitchCompat;
import android.support.v7.widget.Toolbar;
import android.text.TextUtils;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.ProgressBar;

import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QuerySnapshot;

import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.concurrent.TimeUnit;

import christian.eilers.flibber.Adapter.BeteiligteAdapter;
import christian.eilers.flibber.MainActivity;
import christian.eilers.flibber.Models.TaskModel;
import christian.eilers.flibber.Models.User;
import christian.eilers.flibber.R;
import christian.eilers.flibber.Utils.LocalStorage;
import static christian.eilers.flibber.Utils.Strings.*;

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
        getSupportActionBar().setDisplayShowTitleEnabled(false); // Titel der Actionbar ausblenden
    }

    // Initialize variables
    private void initializeVariables() {
        userID = LocalStorage.getUserID(this);
        groupID = LocalStorage.getGroupID(this);
        db = FirebaseFirestore.getInstance();
        users = (HashMap<String, User>) getIntent().getSerializableExtra(USERS);
        if(users == null) {
            Intent main = new Intent(this, MainActivity.class);
            startActivity(main);
            overridePendingTransition(R.anim.fade_in, R.anim.fade_out);
            finish();
        }
        ArrayList<User> userList = new ArrayList<>(users.values());
        adapter_beteiligte = new BeteiligteAdapter(userList);
        recView_beteiligte.setAdapter(adapter_beteiligte);

        /*progressBar.setVisibility(View.VISIBLE);

        db.collection(GROUPS).document(groupID).collection(USERS).get().addOnSuccessListener(new OnSuccessListener<QuerySnapshot>() {
            @Override
            public void onSuccess(QuerySnapshot documentSnapshots) {
                retrieveUsers(documentSnapshots);
                ArrayList<User> userList = new ArrayList<>(users.values());
                adapter_beteiligte = new BeteiligteAdapter(userList);
                recView_beteiligte.setAdapter(adapter_beteiligte);
                progressBar.setVisibility(View.GONE);
            }
        });*/
    }

    private void saveTask() {
        String title = et_name.getText().toString().trim();
        String s_frequenz = et_frequenz.getText().toString().trim();
        String s_points = et_points.getText().toString().trim();

        if (TextUtils.isEmpty(title) || TextUtils.isEmpty(s_frequenz)) return;
        if (adapter_beteiligte.getInvolvedIDs().isEmpty()) return;

        progressBar.setVisibility(View.VISIBLE);
        long frequenz = Long.valueOf(s_frequenz);
        long points = 0;
        if (!TextUtils.isEmpty(s_points)) points = Long.valueOf(s_points);
        boolean hasOrder = switch_order.isChecked();

        DocumentReference doc = db.collection(GROUPS).document(groupID).collection(TASKS).document();

        TaskModel task = new TaskModel(title, frequenz, points, adapter_beteiligte.getInvolvedIDs(),
                hasOrder,
                new Date(System.currentTimeMillis() + TimeUnit.DAYS.toMillis(frequenz)));

        doc.set(task).addOnSuccessListener(new OnSuccessListener<Void>() {
            @Override
            public void onSuccess(Void aVoid) {
                progressBar.setVisibility(View.GONE);
                finish();
            }
        });
    }

    // Erzeugt eine Userliste mithilfe eines Snapshots aus der Datenbank
    private void retrieveUsers(@NotNull QuerySnapshot documentSnapshots) {
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

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_new_task, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_save:
                saveTask();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    private Toolbar toolbar;
    private EditText et_name, et_frequenz, et_points;
    private SwitchCompat switch_order;
    private RecyclerView recView_beteiligte;
    private BeteiligteAdapter adapter_beteiligte;
    private ProgressBar progressBar;

    private String userID, groupID;
    private FirebaseFirestore db;
    private HashMap<String, User> users;

}
