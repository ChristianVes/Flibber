package christian.eilers.flibber.Home;


import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.app.Fragment;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.firebase.ui.firestore.FirestoreRecyclerAdapter;
import com.firebase.ui.firestore.FirestoreRecyclerOptions;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import java.util.HashMap;

import christian.eilers.flibber.Models.User;
import christian.eilers.flibber.R;
import christian.eilers.flibber.Utils.GlideApp;
import christian.eilers.flibber.Utils.LocalStorage;
import de.hdodenhof.circleimageview.CircleImageView;

public class FinanceFragment extends Fragment {

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {

        mainView= inflater.inflate(R.layout.fragment_finanzen, container, false);
        initializeViews();
        initializeVariables();
        loadBilanz();
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
        db = FirebaseFirestore.getInstance();
        storage = FirebaseStorage.getInstance().getReference();
        groupID = LocalStorage.getGroupID(getContext());
        users = ((HomeActivity) getActivity()).getUsers();
    }

    private void loadBilanz() {
        Query query = db.collection(GROUPS).document(groupID)
                .collection(USERS).orderBy(MONEY);

        FirestoreRecyclerOptions<User> options = new FirestoreRecyclerOptions.Builder<User>()
                .setQuery(query, User.class)
                .build();

        adapterBilanz = new FirestoreRecyclerAdapter<User, UserHolder>(options) {
            @NonNull
            @Override
            public UserHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
                View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_finanzen_user, parent, false);
                return new UserHolder(view);
            }

            @Override
            protected void onBindViewHolder(@NonNull UserHolder holder, int position, @NonNull User model) {
                holder.tv_username.setText(model.getName());
                holder.tv_money.setText(model.getMoney() + " \u20ac");
                // PROFILE PICTURE
                if(model.getPicPath() != null)
                    GlideApp.with(getContext())
                            .load(storage.child(PROFILE).child(model.getPicPath()))
                            .dontAnimate()
                            .placeholder(R.drawable.profile_placeholder)
                            .into(holder.img_profile);
            }
        };

        recBilanz.setLayoutManager(new LinearLayoutManager(getContext()));
        recBilanz.setAdapter(adapterBilanz);

    }

    public class UserHolder extends RecyclerView.ViewHolder {

        CircleImageView img_profile;
        TextView tv_username, tv_money;

        public UserHolder(View itemView) {
            super(itemView);
            img_profile = itemView.findViewById(R.id.profile_image);
            tv_username = itemView.findViewById(R.id.username);
            tv_money = itemView.findViewById(R.id.money);
        }
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
    private FirebaseFirestore db;
    private String groupID;
    private HashMap<String, User> users;

    private View mainView;
    private Toolbar toolbar;
    private RecyclerView recBilanz, recVerlauf;
    private FirestoreRecyclerAdapter adapterBilanz, adapterVerlauf;
    private FloatingActionButton fab;

    private final String GROUPS = "groups";
    private final String USERS = "users";
    private final String FINANCES = "finances";
    private final String TIMESTAMP = "timestamp";
    private final String MONEY = "money";
    private final String PROFILE = "profile";
}
