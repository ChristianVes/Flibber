package christian.eilers.flibber.ProfilAndWgs;

import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
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

import com.crashlytics.android.Crashlytics;
import com.firebase.ui.firestore.FirestoreRecyclerAdapter;
import com.firebase.ui.firestore.FirestoreRecyclerOptions;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;

import java.util.HashMap;
import java.util.Map;

import christian.eilers.flibber.Home.HomeActivity;
import christian.eilers.flibber.Models.User;
import christian.eilers.flibber.Models.Wg;
import christian.eilers.flibber.R;
import christian.eilers.flibber.Utils.Utils;

public class WgFragment extends Fragment {

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        mainView = inflater.inflate(R.layout.fragment_wg, container, false);
        initializeViews();
        db = FirebaseFirestore.getInstance();
        loadData();
        btn_new.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                createWgDialog();
            }
        });
        btn_einladungen.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                FragmentManager fm = getFragmentManager();
                EinladungenFragment frag = EinladungenFragment.newInstance();
                frag.setTargetFragment(WgFragment.this, 1);
                frag.show(fm, "einladungen");
            }
        });
        return mainView;
    }

    // Initialize views from layout file
    private void initializeViews() {
        recView = mainView.findViewById(R.id.recView);
        btn_new = mainView.findViewById(R.id.btn_new);
        btn_einladungen = mainView.findViewById(R.id.btn_einladungen);
        placeholder = mainView.findViewById(R.id.placeholder);
        progressBar = mainView.findViewById(R.id.progressBar);
        progressBar.setVisibility(View.VISIBLE);
    }

    // Open Dialog to create a new WG
    private void createWgDialog() {
        final View v = getLayoutInflater().inflate(R.layout.dialog_wg_selector, null);
        final TextView v_title = v.findViewById(R.id.title);
        final EditText eT_name = v.findViewById(R.id.editText_key);
        final Button btn = v.findViewById(R.id.button_ok);
        // Change title/hint (because layout file is used for Join-Dialog too)
        v_title.setText("Neue WG gründen");
        eT_name.setHint("Name der WG");
        final AlertDialog dialog = makeAlertDialog(v);
        btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String wgName = eT_name.getText().toString().trim();
                createWg(wgName);
                dialog.dismiss();
            }
        });
        eT_name.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView textView, int i, KeyEvent keyEvent) {
                if (i == EditorInfo.IME_ACTION_GO) {
                    String wgName = eT_name.getText().toString().trim();
                    createWg(wgName);
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
        progressBar.setVisibility(View.VISIBLE);
        // Create new WG-Document
        final DocumentReference ref_wg = db.collection("wgs").document();
        final Wg wg = new Wg(wgName, ref_wg.getId(), null);
        ref_wg.set(wg);

        // Add WG to the current user's WG-Collection
        db.collection("users").document(Utils.getUSERID()).collection("wgs").document(wg.getKey()).set(wg);
        // Add user to the WG
        addUserToWg(wg.getKey(), wgName);
    }

    // Add current User to the WG
    private void addUserToWg(final String wgKey, final String wgName) {
        db.collection("users").document(Utils.getUSERID()).get().addOnCompleteListener(new OnCompleteListener<DocumentSnapshot>() {
            @Override
            public void onComplete(@NonNull Task<DocumentSnapshot> task) {
                if (!task.isSuccessful()) {
                    Crashlytics.logException(task.getException());
                    progressBar.setVisibility(View.GONE);
                    return;
                }
                String username = task.getResult().getString("name");
                String email = task.getResult().getString("email");
                String picPath = task.getResult().getString("picPath");
                User user = new User(username, email, Utils.getUSERID(), picPath, 0.0);
                db.collection("wgs").document(wgKey).collection("users").document(Utils.getUSERID()).set(user);
                progressBar.setVisibility(View.GONE);
                Toast.makeText(getContext(), wgName + " created!", Toast.LENGTH_SHORT).show();
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
                .document(Utils.getUSERID())
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
                holder.wg = model;
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
        public Wg wg;

        public WgHolder(View itemView) {
            super(itemView);
            v_name = itemView.findViewById(R.id.wg_name);
            itemView.setOnClickListener(this);
        }

        // Aktualisiere Profilbild, Speichere WG-Key lokal, Wechsel zur HomeActivity
        @Override
        public void onClick(View view) {
            updatePicture();
            Utils.setLocalData(getActivity(), wg.getKey(), Utils.getUSERID(), Utils.getUSERNAME());
            Intent homeIntent = new Intent(getContext(), HomeActivity.class);
            startActivity(homeIntent);
            getActivity().overridePendingTransition(R.anim.fade_in, R.anim.fade_out);
            getActivity().finish();
        }

        // Update Profilbild des aktuellen Users, welches in der WG-Collection gespeichert ist
        private void updatePicture() {
            db.collection("users").document(Utils.getUSERID()).get().addOnSuccessListener(new OnSuccessListener<DocumentSnapshot>() {
                @Override
                public void onSuccess(DocumentSnapshot documentSnapshot) {
                    String picPath = documentSnapshot.getString("picPath");
                    Map<String, Object> userImage = new HashMap<>();
                    userImage.put("picPath", picPath);
                    db.collection("wgs").document(wg.getKey()).collection("users").document(Utils.getUSERID()).update(userImage);
                }
            });

        }
    }

    private View mainView;
    private RecyclerView recView;
    private TextView placeholder;
    private Button btn_new, btn_einladungen;
    private ProgressBar progressBar;
    private FirestoreRecyclerAdapter adapter;
    private FirebaseFirestore db;
}
