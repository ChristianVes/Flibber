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
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;

import christian.eilers.flibber.Home.HomeActivity;
import christian.eilers.flibber.Models.User;
import christian.eilers.flibber.Models.Wg;
import christian.eilers.flibber.R;
import christian.eilers.flibber.Utils.Utils;

public class EinladungenFragment extends Fragment {

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        mainView = inflater.inflate(R.layout.fragment_einladungen, container, false);
        initializeViews();
        db = FirebaseFirestore.getInstance();
        loadData();
        return mainView;
    }

    // Initialize views from layout file
    private void initializeViews() {
        recView = mainView.findViewById(R.id.recView);
        btn_join = mainView.findViewById(R.id.btn_join);
        placeholder = mainView.findViewById(R.id.placeholder);
        progressBar = mainView.findViewById(R.id.progressBar);
        progressBar.setVisibility(View.VISIBLE);
        btn_join.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                joinWgDialog();
            }
        });
    }

    // Lade Liste an WG's des eingeloggten Users aus der Database
    private void loadData() {
        // Referenz: WG's des aktuellen Users
        // nach Einzugsdatum soriert
        Query query = FirebaseFirestore.getInstance()
                .collection("users")
                .document(Utils.getUSERID())
                .collection("invitations")
                .orderBy("timestamp");

        FirestoreRecyclerOptions<Wg> options = new FirestoreRecyclerOptions.Builder<Wg>()
                .setQuery(query, Wg.class)
                .build();

        adapter = new FirestoreRecyclerAdapter<Wg, EinladungenFragment.WgHolder>(options) {

            // Set Placeholder-Visibility and ProgressBar-Visibility
            @Override
            public void onDataChanged() {
                super.onDataChanged();
                if (getItemCount() == 0) placeholder.setVisibility(View.VISIBLE);
                else placeholder.setVisibility(View.GONE);
                progressBar.setVisibility(View.GONE);
            }

            @Override
            public void onBindViewHolder(EinladungenFragment.WgHolder holder, int position, Wg model) {
                holder.v_name.setText(model.getName());
                holder.wg = model;
            }

            // Einmalige Zuweisung zum ViewHolder
            @Override
            public EinladungenFragment.WgHolder onCreateViewHolder(ViewGroup group, int i) {
                View view = LayoutInflater.from(group.getContext()).inflate(R.layout.item_wg, group, false);
                return new EinladungenFragment.WgHolder(view);
            }
        };

        recView.setLayoutManager(new LinearLayoutManager(getContext()));
        recView.setAdapter(adapter);
    }

    // Custom ViewHolder for interacting with single items of the RecyclerView
    public class WgHolder extends RecyclerView.ViewHolder implements View.OnClickListener {
        public TextView v_name;
        public Wg wg;

        public WgHolder(View itemView) {
            super(itemView);
            v_name = itemView.findViewById(R.id.wg_name);
            itemView.setOnClickListener(this);
        }

        @Override
        public void onClick(View view) {
            joinWG(wg.getKey());
        }
    }

    // Open Dialog to join an existing WG
    private void joinWgDialog() {
        final View v = getLayoutInflater().inflate(R.layout.dialog_wg_selector, null);
        final EditText eT_key = v.findViewById(R.id.editText_key);
        final Button btn = v.findViewById(R.id.button_ok);
        final AlertDialog dialog = makeAlertDialog(v);

        btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String wgkey = eT_key.getText().toString().trim();
                joinWG(wgkey);
                dialog.dismiss();
            }
        });
        eT_key.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView textView, int i, KeyEvent keyEvent) {
                if (i == EditorInfo.IME_ACTION_GO) {
                    String wgkey = eT_key.getText().toString().trim();
                    joinWG(wgkey);
                    dialog.dismiss();
                    return true;
                }
                return false;
            }
        });
    }

    // Join WG with this specific key (if it exists)
    private void joinWG(final String wgKey) {
        if (TextUtils.isEmpty(wgKey)) {
            Toast.makeText(getContext(), "Keinen Key eingegeben", Toast.LENGTH_SHORT).show();
            return;
        }
        progressBar.setVisibility(View.VISIBLE);

        db.collection("users").document(Utils.getUSERID()).collection("wgs").document(wgKey).get().addOnCompleteListener(new OnCompleteListener<DocumentSnapshot>() {
            @Override
            public void onComplete(@NonNull Task<DocumentSnapshot> task) {
                // Überprüfe ob User bereits Mitglied in der WG ist
                if (task.getResult().exists()) {
                    progressBar.setVisibility(View.GONE);
                    Toast.makeText(getContext(), "Already member of this WG", Toast.LENGTH_SHORT).show();
                    return;
                }

                db.collection("wgs").document(wgKey).get().addOnCompleteListener(new OnCompleteListener<DocumentSnapshot>() {
                    @Override
                    public void onComplete(@NonNull Task<DocumentSnapshot> task) {
                        DocumentSnapshot document = task.getResult();
                        // Überprüfe ob WG mit eingegebenem Key existiert
                        if (!document.exists()) {
                            progressBar.setVisibility(View.GONE);
                            Toast.makeText(getContext(), "WG does not exist", Toast.LENGTH_SHORT).show();
                            return;
                        }
                        // WG zu dem User hinzufügen
                        Wg wg = document.toObject(Wg.class);
                        db.collection("users").document(Utils.getUSERID()).collection("wgs").document(wg.getKey()).set(wg);
                        // User zur WG hinzufügen
                        addUserToWg(wgKey);
                    }
                });
            }
        });
    }

    // Add current User to the WG
    private void addUserToWg(final String wgKey) {
        db.collection("users").document(Utils.getUSERID()).get().addOnCompleteListener(new OnCompleteListener<DocumentSnapshot>() {
            @Override
            public void onComplete(@NonNull Task<DocumentSnapshot> task) {
                String username = task.getResult().getString("name");
                String email = task.getResult().getString("email");
                String picPath = task.getResult().getString("picPath");
                User user = new User(username, email, Utils.getUSERID(), picPath, 0.0);
                db.collection("wgs").document(wgKey).collection("users").document(Utils.getUSERID()).set(user);
                deleteInvitation(wgKey);
            }
        });
    }

    // Delete Invitation Documents at Users-Collection and WG-Collection
    private void deleteInvitation(final String wgkey) {
        db.collection("users").document(Utils.getUSERID()).collection("invitations").document(wgkey).delete();
        db.collection("wgs").document(wgkey).collection("invitations").document(Utils.getUSERID()).delete();
        Toast.makeText(getContext(), "Erfolgreich beigetreten", Toast.LENGTH_SHORT).show();
        progressBar.setVisibility(View.GONE);
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
    public void onStop() {
        super.onStop();
        adapter.stopListening();
    }

    @Override
    public void onStart() {
        super.onStart();
        adapter.startListening();
    }

    private View mainView;
    private RecyclerView recView;
    private TextView placeholder;
    private Button btn_join;
    private ProgressBar progressBar;
    private FirestoreRecyclerAdapter adapter;
    private FirebaseFirestore db;
}
