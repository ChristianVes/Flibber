package christian.eilers.flibber.Home;

import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.text.TextUtils;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.Toast;

import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QuerySnapshot;
import com.google.firebase.storage.FirebaseStorage;

import java.util.ArrayList;
import java.util.HashMap;

import christian.eilers.flibber.Adapter.BeteiligteAdapter;
import christian.eilers.flibber.Adapter.BezahlerAdapter;
import christian.eilers.flibber.Models.Transaction;
import christian.eilers.flibber.Models.User;
import christian.eilers.flibber.R;
import christian.eilers.flibber.Utils.LocalStorage;

public class TransactionActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_finance_entry);
        initializeViews();
        initializeVariables();
    }

    private void initializeViews() {
        toolbar = findViewById(R.id.toolbar);
        et_article = findViewById(R.id.input_article);
        et_description = findViewById(R.id.input_description);
        et_price = findViewById(R.id.input_price);
        rec_beteiligte = findViewById(R.id.listBeteiligte);
        rec_bezahler = findViewById(R.id.listBezahler);
        rec_beteiligte = findViewById(R.id.listBeteiligte);

        rec_bezahler.setHasFixedSize(true);
        rec_bezahler.setLayoutManager(new LinearLayoutManager(this));
        rec_beteiligte.setHasFixedSize(true);
        rec_beteiligte.setLayoutManager(new LinearLayoutManager(this));

        setSupportActionBar(toolbar); // Toolbar als Actionbar setzen
        getSupportActionBar().setDisplayShowTitleEnabled(false); // Titel der Actionbar ausblenden
    }

    // Initialize variables
    private void initializeVariables() {
        userID = LocalStorage.getUserID(this);
        groupID = LocalStorage.getGroupID(this);
        db = FirebaseFirestore.getInstance();

        db.collection(GROUPS).document(groupID).collection(USERS).get().addOnSuccessListener(new OnSuccessListener<QuerySnapshot>() {
            @Override
            public void onSuccess(QuerySnapshot documentSnapshots) {
                retrieveUsers(documentSnapshots);
                ArrayList<User> userList = new ArrayList<>(users.values());
                adapter_bezahler = new BezahlerAdapter(userList, userID);
                adapter_beteiligte = new BeteiligteAdapter(userList);
                rec_bezahler.setAdapter(adapter_bezahler);
                rec_beteiligte.setAdapter(adapter_beteiligte);
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

    // Save the Transaction in the database
    private void saveTransaction() {
        String s_price = et_price.getText().toString().trim();
        String title = et_article.getText().toString().trim();
        String description = et_description.getText().toString().trim();
        if (TextUtils.isEmpty(s_price)) return;
        if (TextUtils.isEmpty(title)) return;

        DocumentReference doc = db.collection(GROUPS).document(groupID).collection(FINANCES).document();

        Transaction transaction = new Transaction(
                doc.getId(),
                title,
                description,
                adapter_bezahler.getBezahlerID(),
                userID,
                adapter_beteiligte.getInvolvedIDs(),
                Double.parseDouble(s_price)
        );

        doc.set(transaction);

        finish();
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_finanzen_eintrag, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_save:
                saveTransaction();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    private String userID, groupID;
    private FirebaseFirestore db;
    private HashMap<String, User> users;
    private BezahlerAdapter adapter_bezahler;
    private BeteiligteAdapter adapter_beteiligte;

    private Toolbar toolbar;
    private EditText et_price, et_article, et_description;
    private RecyclerView rec_bezahler, rec_beteiligte;

    private final String GROUPS = "groups";
    private final String USERS = "users";
    private final String FINANCES = "finances";
}
