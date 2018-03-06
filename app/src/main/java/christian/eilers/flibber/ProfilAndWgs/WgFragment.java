package christian.eilers.flibber.ProfilAndWgs;

import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.firebase.ui.firestore.FirestoreRecyclerAdapter;
import com.firebase.ui.firestore.FirestoreRecyclerOptions;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;

import christian.eilers.flibber.Home.HomeActivity;
import christian.eilers.flibber.Models.Wg;
import christian.eilers.flibber.R;
import christian.eilers.flibber.Utils.LocalStorage;

public class WgFragment extends Fragment {

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        mainView = inflater.inflate(R.layout.fragment_wg, container, false);
        initializeViews();
        userID = LocalStorage.getUserID(getContext());
        loadData();
        btn_new.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                WgErstellenFragment frag = WgErstellenFragment.newInstance();
                frag.setTargetFragment(WgFragment.this, 1);
                frag.show(getFragmentManager(), "wg_erstellen");
            }
        });
        btn_einladungen.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                EinladungenFragment frag = EinladungenFragment.newInstance();
                frag.setTargetFragment(WgFragment.this, 2);
                frag.show(getFragmentManager(), "einladungen");
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
            LocalStorage.setGroupID(getContext(), wg.getKey());
            Intent homeIntent = new Intent(getContext(), HomeActivity.class);
            startActivity(homeIntent);
            getActivity().overridePendingTransition(R.anim.fade_in, R.anim.fade_out);
        }
    }

    private View mainView;
    private RecyclerView recView;
    private TextView placeholder;
    private Button btn_new, btn_einladungen;
    private ProgressBar progressBar;

    private FirestoreRecyclerAdapter adapter;
    private String userID;
}
