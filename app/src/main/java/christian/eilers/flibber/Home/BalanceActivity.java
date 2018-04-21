package christian.eilers.flibber.Home;

import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.view.View;
import android.widget.ProgressBar;

import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import org.fabiomsr.moneytextview.MoneyTextView;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;

import christian.eilers.flibber.Adapter.BalanceUserAdapter;
import christian.eilers.flibber.MainActivity;
import christian.eilers.flibber.Models.Balancing;
import christian.eilers.flibber.Models.User;
import christian.eilers.flibber.R;
import christian.eilers.flibber.Utils.LocalStorage;
import static christian.eilers.flibber.Utils.Strings.*;

public class BalanceActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_balance);
        initializeViews();
        initializeVariables();
    }

    private void initializeViews() {
        toolbar = findViewById(R.id.toolbar);
        recView = findViewById(R.id.recView);
        progressBar = findViewById(R.id.progressBar);
    }

    private void initializeVariables() {
        userID = LocalStorage.getUserID(this);
        groupID = LocalStorage.getGroupID(this);
        db = FirebaseFirestore.getInstance();
        balancingID = getIntent().getStringExtra(BALANCING);
        users = (HashMap<String, User>) getIntent().getSerializableExtra(USERS);
        if(balancingID == null || users == null || groupID == null) {
            Intent main = new Intent(this, MainActivity.class);
            startActivity(main);
            overridePendingTransition(R.anim.fade_in, R.anim.fade_out);
            finish();
        } else
            loadBalance();
    }

    private void loadBalance() {
        progressBar.setVisibility(View.VISIBLE);
        db.collection(GROUPS).document(groupID).collection(BALANCING).document(balancingID).get().addOnSuccessListener(new OnSuccessListener<DocumentSnapshot>() {
            @Override
            public void onSuccess(DocumentSnapshot documentSnapshot) {
                thisBalancing = documentSnapshot.toObject(Balancing.class);
                final ArrayList<User> balanceUsers = new ArrayList<>();
                for (String key : thisBalancing.getValues().keySet()) {
                    User user = users.get(key);
                    user.setMoney(thisBalancing.getValues().get(key));
                    balanceUsers.add(user);
                }
                // Sorting by money
                Collections.sort(balanceUsers, new Comparator<User>() {
                    @Override
                    public int compare(User user2, User user1)
                    {
                        if (user1.getMoney() < user2.getMoney()) return -1;
                        return 1;
                    }
                });
                recView.setHasFixedSize(true);
                recView.setLayoutManager(new LinearLayoutManager(BalanceActivity.this));
                BalanceUserAdapter adapter = new BalanceUserAdapter(balanceUsers);
                recView.setAdapter(adapter);
                progressBar.setVisibility(View.GONE);
            }
        });
    }

    private Toolbar toolbar;
    private RecyclerView recView;
    private ProgressBar progressBar;

    private String userID, groupID, balancingID;
    private FirebaseFirestore db;
    private Balancing thisBalancing;
    private HashMap<String, User> users;
}
