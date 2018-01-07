package christian.eilers.flibber.ProfilAndWgs;

import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.TextUtils;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.widget.Button;
import android.widget.EditText;
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
import christian.eilers.flibber.R;
import christian.eilers.flibber.WgSelectorActivity;

public class WgFragment extends Fragment {

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        mainView = inflater.inflate(R.layout.fragment_wg, container, false);
        recView = mainView.findViewById(R.id.recView);
        btn_new = mainView.findViewById(R.id.btn_new);
        placeholder = mainView.findViewById(R.id.placeholder);
        progressBar = mainView.findViewById(R.id.progressBar);
        auth = FirebaseAuth.getInstance();
        userID = auth.getCurrentUser().getUid();
        db = FirebaseFirestore.getInstance();
        progressBar.setVisibility(View.VISIBLE);
        loadData();
        btn_new.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                createWgDialog();
            }
        });
        return mainView;
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
                String name = eT_name.getText().toString().trim();
                createWg(name);
                dialog.dismiss();
            }
        });
        eT_name.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView textView, int i, KeyEvent keyEvent) {
                if (i == EditorInfo.IME_ACTION_GO) {
                    String name = eT_name.getText().toString().trim();
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
        if(TextUtils.isEmpty(wgName)){
            Toast.makeText(getContext(), "Keinen Namen eingegeben", Toast.LENGTH_SHORT).show();
            return;
        }
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
        AlertDialog.Builder mBuilder = new AlertDialog.Builder(getContext());
        mBuilder.setView(v);
        AlertDialog dialog = mBuilder.create();
        dialog.show();
        return dialog;
    }

    @Override
    public void onStart() {
        super.onStart();
        adapter.startListening();
    }

    @Override
    public void onStop() {
        super.onStop();
        adapter.stopListening();
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

        adapter = new FirestoreRecyclerAdapter<Wg, WgFragment.WgHolder>(options) {

            // Set Placeholder-Visibility and ProgressBar-Visibility
            @Override
            public void onDataChanged() {
                super.onDataChanged();
                if (getItemCount() == 0) placeholder.setVisibility(View.VISIBLE);
                else placeholder.setVisibility(View.GONE);
                progressBar.setVisibility(View.GONE);
            }

            @Override
            public void onBindViewHolder(WgFragment.WgHolder holder, int position, Wg model) {
                holder.v_name.setText(model.getName());
            }

            // Einmalige Zuweisung zum ViewHolder
            @Override
            public WgFragment.WgHolder onCreateViewHolder(ViewGroup group, int i) {
                View view = LayoutInflater.from(group.getContext()).inflate(R.layout.item_wg, group, false);
                return new WgFragment.WgHolder(view);
            }
        };

        recView.setLayoutManager(new LinearLayoutManager(getContext()));
        recView.setAdapter(adapter);
    }

    // Custom ViewHolder for interacting with single items of the RecyclerView
    public class WgHolder extends RecyclerView.ViewHolder implements View.OnClickListener{
        public TextView v_name;

        public WgHolder(View itemView) {
            super(itemView);
            v_name = itemView.findViewById(R.id.wg_name);
            itemView.setOnClickListener(this);
        }

        @Override
        public void onClick(View view) {
            // TODO
        }
    }

    private View mainView;
    private RecyclerView recView;
    private TextView placeholder;
    private Button btn_new;
    private ProgressBar progressBar;
    private FirestoreRecyclerAdapter adapter;
    private FirebaseAuth auth;
    private FirebaseFirestore db;

    private String userID;
}
