package christian.eilers.flibber.Home;


import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.app.Fragment;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.firebase.ui.firestore.FirestoreRecyclerAdapter;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import java.util.HashMap;

import christian.eilers.flibber.Models.User;
import christian.eilers.flibber.R;
import christian.eilers.flibber.Utils.LocalStorage;

public class FinanceFragment extends Fragment {

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {

        mainView= inflater.inflate(R.layout.fragment_finanzen, container, false);
        initializeViews();
        initializeVariables();
        return mainView;
    }

    private void initializeViews() {
        toolbar = mainView.findViewById(R.id.toolbar);
        recBilanz = mainView.findViewById(R.id.recProfils);
        recVerlauf = mainView.findViewById(R.id.recVerlauf);
        fab = mainView.findViewById(R.id.fab);

        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                getActivity().startActivity(new Intent(getContext(), FinanceEntryActivity.class));
                getActivity().overridePendingTransition(R.anim.fade_in, R.anim.fade_out);
            }
        });
    }

    // Initialize variables
    private void initializeVariables() {
        storage = FirebaseStorage.getInstance().getReference();
        groupID = LocalStorage.getGroupID(getContext());
        users = ((HomeActivity) getActivity()).getUsers();
    }

    private void loadBilanz() {

    }

    @Override
    public void onStart() {
        super.onStart();
        if(adapterBilanz != null) adapterBilanz.startListening();
        if(adapterVerlauf != null) adapterVerlauf.startListening();
    }

    @Override
    public void onStop() {
        super.onStop();
        if(adapterBilanz != null) adapterBilanz.stopListening();
        if(adapterVerlauf != null) adapterVerlauf.stopListening();
    }

    private StorageReference storage;
    private String groupID;
    private HashMap<String, User> users;

    private View mainView;
    private Toolbar toolbar;
    private RecyclerView recBilanz, recVerlauf;
    private FirestoreRecyclerAdapter adapterBilanz, adapterVerlauf;
    private FloatingActionButton fab;
}
