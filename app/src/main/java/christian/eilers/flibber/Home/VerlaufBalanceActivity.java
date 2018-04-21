package christian.eilers.flibber.Home;

import android.content.Intent;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.text.format.DateUtils;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.firebase.ui.firestore.FirestoreRecyclerAdapter;
import com.firebase.ui.firestore.FirestoreRecyclerOptions;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;

import org.fabiomsr.moneytextview.MoneyTextView;

import java.util.HashMap;

import christian.eilers.flibber.Adapter.VerlaufAdapter;
import christian.eilers.flibber.Adapter.VerlaufAllAdapter;
import christian.eilers.flibber.MainActivity;
import christian.eilers.flibber.Models.Balancing;
import christian.eilers.flibber.Models.Payment;
import christian.eilers.flibber.Models.User;
import christian.eilers.flibber.R;
import christian.eilers.flibber.Utils.LocalStorage;

import static christian.eilers.flibber.Utils.Strings.BALANCING;
import static christian.eilers.flibber.Utils.Strings.BUFFER;
import static christian.eilers.flibber.Utils.Strings.FINANCES;
import static christian.eilers.flibber.Utils.Strings.GROUPS;
import static christian.eilers.flibber.Utils.Strings.TIMESTAMP;
import static christian.eilers.flibber.Utils.Strings.USERS;

//TODO: Platzhalter wenn noch keine vorhanden sind
public class VerlaufBalanceActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_verlauf_balance);
        initializeViews();
        initializeVariables();
    }

    private void initializeViews() {
        toolbar = findViewById(R.id.toolbar);
        recView = findViewById(R.id.recVerlauf);
        progressBar = findViewById(R.id.progressBar);
        setSupportActionBar(toolbar); // Toolbar als Actionbar setzen
    }

    // Initialize variables
    private void initializeVariables() {
        db = FirebaseFirestore.getInstance();
        userID = LocalStorage.getUserID(this);
        groupID = LocalStorage.getGroupID(this);

        users = (HashMap<String, User>) getIntent().getSerializableExtra(USERS);
        if(users == null || groupID == null) {
            Intent main = new Intent(this, MainActivity.class);
            startActivity(main);
            overridePendingTransition(R.anim.fade_in, R.anim.fade_out);
            finish();
        }
        else loadVerlauf();
    }

    // Load all transactions/payments
    private void loadVerlauf() {
        Query query = db.collection(GROUPS).document(groupID)
                .collection(BALANCING).orderBy(TIMESTAMP, Query.Direction.DESCENDING);   // order by Date

        FirestoreRecyclerOptions<Balancing> recyclerOptions = new FirestoreRecyclerOptions.Builder<Balancing>()
                .setQuery(query, Balancing.class)
                .build();

        adapter = new FirestoreRecyclerAdapter<Balancing, BalancingHolder>(recyclerOptions){

            @NonNull
            @Override
            public BalancingHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
                View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_balancing, parent, false);
                return new BalancingHolder(view);
            }

            @Override
            protected void onBindViewHolder(@NonNull final BalancingHolder holder, final int position, @NonNull final Balancing model) {
                // CREATOR_NAME
                holder.tv_creator.setText(users.get(model.getCreatorID()).getName());
                // TIMESTAMP (Buffer um "in 0 Minuten"-Anzeige zu vermeiden)
                if (model.getTimestamp() != null)
                    holder.tv_datum.setText(
                            DateUtils.getRelativeTimeSpanString(model.getTimestamp().getTime(),
                                    System.currentTimeMillis() + BUFFER,
                                    DateUtils.MINUTE_IN_MILLIS,
                                    DateUtils.FORMAT_ABBREV_RELATIVE));
                else holder.tv_datum.setText("");
                // MONEY
                if (model.getValues().containsKey(userID))
                    holder.tv_value.setAmount(model.getValues().get(userID));
                else holder.tv_value.setAmount(0);

                holder.itemView.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        // TODO: Balance Detail Activity
                        Toast.makeText(holder.itemView.getContext(), "noch nicht möglich...", Toast.LENGTH_SHORT).show();
                        /*Intent i = new Intent(holder.itemView.getContext(), BalanceActivity.class);
                        i.putExtra(BALANCING, getSnapshots().getSnapshot(position).getId());
                        startActivity(i);*/
                    }
                });
            }
        };

        recView.setLayoutManager(new LinearLayoutManager(this));
        recView.setAdapter(adapter);
        recView.setNestedScrollingEnabled(false);
        adapter.startListening();
    }

    // Custom ViewHolder for a Transaction
    public class BalancingHolder extends RecyclerView.ViewHolder {
        TextView tv_datum, tv_creator;
        MoneyTextView tv_value;

        public BalancingHolder(View itemView) {
            super(itemView);
            tv_datum = itemView.findViewById(R.id.datum);
            tv_value = itemView.findViewById(R.id.value);
            tv_creator = itemView.findViewById(R.id.name);
        }
    }

    @Override
    protected void onStart() {
        super.onStart();
        if (adapter != null) adapter.startListening();
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (adapter != null) adapter.stopListening();
    }

    private FirebaseFirestore db;
    private String userID, groupID;
    private HashMap<String, User> users;
    private FirestoreRecyclerAdapter adapter;

    private Toolbar toolbar;
    private RecyclerView recView;
    private ProgressBar progressBar;
}
