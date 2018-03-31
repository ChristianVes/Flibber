package christian.eilers.flibber.Home;

import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QuerySnapshot;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.HashMap;

import christian.eilers.flibber.Adapter.TaskBeteiligteAdapter;
import christian.eilers.flibber.MainActivity;
import christian.eilers.flibber.Models.TaskModel;
import christian.eilers.flibber.Models.User;
import christian.eilers.flibber.R;
import christian.eilers.flibber.Utils.LocalStorage;

public class TaskActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_task);
        initializeVariables();
        initializeViews();
        loadTask();
    }

    private void initializeVariables() {
        userID = LocalStorage.getUserID(this);
        groupID = LocalStorage.getGroupID(this);
        db = FirebaseFirestore.getInstance();
        storage = FirebaseStorage.getInstance().getReference();
        taskID = getIntent().getExtras().getString(TASKID);
        if(taskID == null) {
            Intent main = new Intent(this, MainActivity.class);
            startActivity(main);
            overridePendingTransition(R.anim.fade_in, R.anim.fade_out);
            finish();
        }
    }

    private void initializeViews() {
        toolbar = findViewById(R.id.toolbar);
        tv_frequenz = findViewById(R.id.frequenz);
        tv_points = findViewById(R.id.points);
        rec_involved = findViewById(R.id.recView_beteiligte);
        rec_verlauf = findViewById(R.id.recVerlauf);
        progressBar = findViewById(R.id.progressBar);

        rec_involved.setHasFixedSize(true);
        rec_involved.setLayoutManager(new LinearLayoutManager(this));
    }

    // Load the task information from the database
    private void loadTask() {
        progressBar.setVisibility(View.VISIBLE);
        db.collection(GROUPS).document(groupID).collection(TASKS).document(taskID).get().addOnSuccessListener(new OnSuccessListener<DocumentSnapshot>() {
            @Override
            public void onSuccess(DocumentSnapshot documentSnapshot) {
                // get the current Task
                thisTask = documentSnapshot.toObject(TaskModel.class);
                // Task-Frequenz
                tv_frequenz.setText(thisTask.getFrequenz() +"");
                // Task Points (Aufwand)
                tv_points.setText(thisTask.getPoints() +"");
                setSupportActionBar(toolbar); // Toolbar als Actionbar setzen
                getSupportActionBar().setTitle(thisTask.getTitle()); // Titel des Tasks setzen

                // Lade Beteiligte User-Liste (in Reihenfolge)
                db.collection(GROUPS).document(groupID).collection(USERS).get().addOnSuccessListener(new OnSuccessListener<QuerySnapshot>() {
                    @Override
                    public void onSuccess(QuerySnapshot documentSnapshots) {
                        retrieveUsers(documentSnapshots);
                        ArrayList<User> userList = new ArrayList<>(users.values());
                        adapter_beteiligte = new TaskBeteiligteAdapter(userList);
                        rec_involved.setAdapter(adapter_beteiligte);
                        progressBar.setVisibility(View.GONE);
                    }
                });
            }
        });
    }

    // Erzeugt eine Userliste aller Beteiligter mithilfe eines Snapshots aus der Datenbank
    private void retrieveUsers(@NotNull QuerySnapshot documentSnapshots) {
        HashMap<String, User> userHashMap = new HashMap<>();
        for(DocumentSnapshot doc : documentSnapshots) {
            User user = doc.toObject(User.class);
            if (thisTask.getInvolvedIDs().contains(user.getUserID()))
                userHashMap.put(user.getUserID(), user);
        }
        users = (HashMap<String, User>) userHashMap.clone();
    }

    private Toolbar toolbar;
    private TextView tv_frequenz, tv_points;
    private RecyclerView rec_involved, rec_verlauf;
    private ProgressBar progressBar;

    private String userID, groupID, taskID;
    private FirebaseFirestore db;
    private StorageReference storage;
    private TaskModel thisTask;
    private HashMap<String, User> users;
    private TaskBeteiligteAdapter adapter_beteiligte;

    private final String TASKID = "taskID";
    private final String TASKS = "tasks";
    private final String USERS = "users";
    private final String GROUPS = "groups";
}
