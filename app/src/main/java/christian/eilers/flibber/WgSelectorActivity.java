package christian.eilers.flibber;

import android.content.Intent;
import android.support.annotation.NonNull;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.firebase.ui.firestore.FirestoreRecyclerAdapter;
import com.firebase.ui.firestore.FirestoreRecyclerOptions;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;

import christian.eilers.flibber.Models.User;
import christian.eilers.flibber.Models.Wg;

public class WgSelectorActivity extends AppCompatActivity implements View.OnClickListener {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_wg_selector);
        initializeViews();
        auth = FirebaseAuth.getInstance();
        userID = auth.getCurrentUser().getUid();
        db = FirebaseFirestore.getInstance();
        progressBar.setVisibility(View.VISIBLE);
        loadData();
    }

    // Lade Liste an WG's des eingeloggten Users aus der Database
    private void loadData() {
        // Referenz: WG's des aktuellen Users
        // nach Einzugsdatum soriert
        Query query = FirebaseFirestore.getInstance()
                .collection("users")
                .document(userID)
                .collection("wgs")
                .orderBy("timestamp");

        FirestoreRecyclerOptions<Wg> options = new FirestoreRecyclerOptions.Builder<Wg>()
                .setQuery(query, Wg.class)
                .build();

        adapter = new FirestoreRecyclerAdapter<Wg, WgHolder>(options) {

            // Set Placeholder-Visibility and ProgressBar-Visibility
            @Override
            public void onDataChanged() {
                super.onDataChanged();
                if (getItemCount() == 0) placeholder.setVisibility(View.VISIBLE);
                else placeholder.setVisibility(View.GONE);
                progressBar.setVisibility(View.GONE);
            }

            @Override
            public void onBindViewHolder(WgHolder holder, int position, Wg model) {
                holder.v_name.setText(model.getName());
            }

            // Einmalige Zuweisung zum ViewHolder
            @Override
            public WgHolder onCreateViewHolder(ViewGroup group, int i) {
                View view = LayoutInflater.from(group.getContext()).inflate(R.layout.item_wg, group, false);
                return new WgHolder(view);
            }
        };

        recView.setLayoutManager(new LinearLayoutManager(WgSelectorActivity.this));
        recView.setAdapter(adapter);
    }

    // Custom ViewHolder for populating the WG's
    public static class WgHolder extends RecyclerView.ViewHolder {
        public TextView v_name;

        public WgHolder(View itemView) {
            super(itemView);
            v_name = itemView.findViewById(R.id.wg_name);
        }
    }

    // Open Dialog to join an existing WG
    private void joinWgDialog() {
        View v = getLayoutInflater().inflate(R.layout.dialog_wg_selector, null);
        final EditText eT_key = v.findViewById(R.id.editText_key);
        Button btn = v.findViewById(R.id.button_ok);
        final AlertDialog dialog = makeAlertDialog(v);
        btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String key = eT_key.getText().toString();
                joinWG(key);
                dialog.dismiss();
            }
        });
        eT_key.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView textView, int i, KeyEvent keyEvent) {
                if (i == EditorInfo.IME_ACTION_GO) {
                    String key = eT_key.getText().toString();
                    createWg(key);
                    dialog.dismiss();
                    return true;
                }
                return false;
            }
        });
    }

    // Join WG with this specific key (if it exists)
    private void joinWG(final String wgKey) {

        db.collection("users").document(userID).collection("wgs").document(wgKey).get().addOnCompleteListener(new OnCompleteListener<DocumentSnapshot>() {
            @Override
            public void onComplete(@NonNull Task<DocumentSnapshot> task) {
                // Überprüfe ob User bereits Mitglied in der WG ist
                if (!task.isSuccessful()) return;
                if (!task.getResult().exists()) {
                    Toast.makeText(WgSelectorActivity.this, "Already member of this WG", Toast.LENGTH_SHORT).show();
                    return;
                }

                db.collection("wgs").document(wgKey).get().addOnCompleteListener(new OnCompleteListener<DocumentSnapshot>() {
                    @Override
                    public void onComplete(@NonNull Task<DocumentSnapshot> task) {
                        if (!task.isSuccessful()) return;
                        DocumentSnapshot document = task.getResult();
                        // Überprüfe ob WG mit eingegebenem Key existiert
                        if (!document.exists()) {
                            Toast.makeText(WgSelectorActivity.this, "WG does not exist", Toast.LENGTH_SHORT).show();
                            return;
                        }
                        // WG zu dem User hinzufügen
                        Wg wg = document.toObject(Wg.class);
                        db.collection("users").document(userID).collection("wgs").document(wg.getKey()).set(wg);
                        // User zur WG hinzufügen
                        addUserToWg(wgKey);
                    }
                });
            }
        });
    }

    // Open Dialog to create a new WG
    private void createWgDialog() {
        final View v = getLayoutInflater().inflate(R.layout.dialog_wg_selector, null);
        final TextView v_title = v.findViewById(R.id.title);
        final EditText eT_name = v.findViewById(R.id.editText_key);
        final Button btn = v.findViewById(R.id.button_ok);
        v_title.setText("Neue WG gründen");
        eT_name.setHint("Name der WG");
        final AlertDialog dialog = makeAlertDialog(v);
        btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String name = eT_name.getText().toString();
                createWg(name);
                dialog.dismiss();
            }
        });
        eT_name.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView textView, int i, KeyEvent keyEvent) {
                if (i == EditorInfo.IME_ACTION_GO) {
                    String name = eT_name.getText().toString();
                    createWg(name);
                    dialog.dismiss();
                    return true;
                }
                return false;
            }
        });
    }

    // Fügt neue WG zur Database hinzu
    private void createWg(final String wgName) {
        // Create new WG-Document
        final DocumentReference ref_wg = db.collection("wgs").document();
        final Wg wg = new Wg(wgName, ref_wg.getId(), null);
        ref_wg.set(wg);

        // Create new WG-Document for this user
        db.collection("users").document(userID).collection("wgs").document(wg.getKey()).set(wg);
        // Add user to WG
        addUserToWg(wg.getKey());
    }

    // Add current User to the WG
    private void addUserToWg(final String wgKey) {
        db.collection("users").document(userID).get().addOnCompleteListener(new OnCompleteListener<DocumentSnapshot>() {
            @Override
            public void onComplete(@NonNull Task<DocumentSnapshot> task) {
                if (!task.isSuccessful()) return;
                String username = task.getResult().getString("name");
                User user = new User(username, userID, null, 0.0);
                db.collection("wgs").document(wgKey).collection("users").document(userID).set(user);
            }
        });
    }

    // Make an AlertDialog from a View
    public AlertDialog makeAlertDialog(View v) {
        AlertDialog.Builder mBuilder = new AlertDialog.Builder(WgSelectorActivity.this);
        mBuilder.setView(v);
        AlertDialog dialog = mBuilder.create();
        dialog.show();
        return dialog;
    }

    // Initialize views from layout file
    private void initializeViews() {
        btn_back = findViewById(R.id.btn_exit);
        btn_new = findViewById(R.id.btn_new);
        btn_join = findViewById(R.id.btn_join);
        recView = findViewById(R.id.recView);
        placeholder = findViewById(R.id.placeholder);
        progressBar = findViewById(R.id.progressBar);

        btn_back.setOnClickListener(this);
        btn_new.setOnClickListener(this);
        btn_join.setOnClickListener(this);
    }

    // Sign out and open LoginActivity
    @Override
    public void onBackPressed() {
        auth.signOut();
        Intent i = new Intent(WgSelectorActivity.this, LoginActivity.class);
        startActivity(i);
        overridePendingTransition(R.anim.fade_in, R.anim.fade_out);
        finish();
    }

    // Check which Button has been clicked
    @Override
    public void onClick(View v) {
        int id = v.getId();
        if (id == R.id.btn_exit) {
            onBackPressed();
        } else if (id == R.id.btn_join) {
            joinWgDialog();
        } else if (id == R.id.btn_new) {
            createWgDialog();
        }
    }

    // Adapter starts listening
    @Override
    protected void onStart() {
        super.onStart();
        adapter.startListening();
    }

    // Adapter stops listening
    @Override
    protected void onStop() {
        super.onStop();
        adapter.stopListening();
    }

    private ImageButton btn_back;
    private RecyclerView recView;
    private TextView placeholder;
    private Button btn_new, btn_join;
    private ProgressBar progressBar;
    private FirebaseAuth auth;
    private FirebaseFirestore db;
    private String userID;
    private FirestoreRecyclerAdapter adapter;

}
