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
import android.text.format.DateUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.crashlytics.android.Crashlytics;
import com.firebase.ui.firestore.FirestoreRecyclerAdapter;
import com.firebase.ui.firestore.FirestoreRecyclerOptions;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import org.fabiomsr.moneytextview.MoneyTextView;

import java.util.HashMap;

import christian.eilers.flibber.MainActivity;
import christian.eilers.flibber.Models.Payment;
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
        if(users != null) {
            loadBilanz();
            loadVerlauf();
        }
        else {
            Intent main = new Intent(getContext(), MainActivity.class);
            startActivity(main);
            getActivity().overridePendingTransition(R.anim.fade_in, R.anim.fade_out);
            getActivity().finish();
        }
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
                getActivity().startActivity(new Intent(getContext(), TransactionActivity.class));
                getActivity().overridePendingTransition(R.anim.fade_in, R.anim.fade_out);
            }
        });
    }

    // Initialize variables
    private void initializeVariables() {
        db = FirebaseFirestore.getInstance();
        storage = FirebaseStorage.getInstance().getReference();
        userID = LocalStorage.getUserID(getContext());
        groupID = LocalStorage.getGroupID(getContext());
        users = ((HomeActivity) getActivity()).getUsers();
    }

    private void loadBilanz() {
        Query query = db.collection(GROUPS).document(groupID)
                .collection(USERS).orderBy(MONEY, Query.Direction.DESCENDING);

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
                holder.tv_money.setAmount(model.getMoney());

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
        TextView tv_username;
        MoneyTextView tv_money;

        public UserHolder(View itemView) {
            super(itemView);
            img_profile = itemView.findViewById(R.id.profile_image);
            tv_username = itemView.findViewById(R.id.username);
            tv_money = itemView.findViewById(R.id.money);
        }
    }

    private void loadVerlauf() {
        Query query = db.collection(GROUPS).document(groupID)
                .collection(FINANCES).orderBy(TIMESTAMP, Query.Direction.DESCENDING);

        FirestoreRecyclerOptions<Payment> options = new FirestoreRecyclerOptions.Builder<Payment>()
                .setQuery(query, Payment.class)
                .build();

        adapterVerlauf = new FirestoreRecyclerAdapter<Payment, RecyclerView.ViewHolder>(options) {
            @NonNull
            @Override
            public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
                switch (viewType) {
                    case SHOW: {
                        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_transaction, parent, false);
                        return new TransactionHolder(view);
                    }
                    default: {
                        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_empty, parent, false);
                        return new EmptyHolder(view);
                    }
                }
            }

            @Override
            public int getItemViewType(int position) {
                boolean isPayer = getSnapshots().get(position).getPayerID().equals(userID);
                boolean isInvolved = getSnapshots().get(position).getInvolvedIDs().contains(userID);
                if (isPayer || isInvolved) return SHOW;
                return HIDE;
            }

            @Override
            protected void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position, @NonNull Payment model) {
                if (holder.getItemViewType() == HIDE) return;

                TransactionHolder transHolder = (TransactionHolder) holder;
                transHolder.tv_title.setText(model.getTitle());

                // Costs for the current user
                long partialPrice = Math.round((double) model.getPrice() / model.getInvolvedIDs().size());
                long totalPriceRounded = partialPrice * model.getInvolvedIDs().size();

                if (model.getInvolvedIDs().contains(userID)) {
                    if (model.getPayerID().equals(userID))
                        transHolder.tv_value.setAmount(totalPriceRounded - partialPrice);
                    else transHolder.tv_value.setAmount(-partialPrice);
                } else {
                    if (model.getPayerID().equals(userID))
                        transHolder.tv_value.setAmount(totalPriceRounded);
                    else Crashlytics.logException(new Exception("Falscher Viewholder angezeigt!"));
                }

                // TIMESTAMP (Buffer um "in 0 Minuten"-Anzeige zu vermeiden)
                if (model.getTimestamp() != null)
                    transHolder.tv_datum.setText(
                            DateUtils.getRelativeTimeSpanString(model.getTimestamp().getTime(),
                                    System.currentTimeMillis() + BUFFER,
                                    DateUtils.MINUTE_IN_MILLIS,
                                    DateUtils.FORMAT_ABBREV_RELATIVE));

                // Payer-Name
                transHolder.tv_name.setText(users.get(model.getPayerID()).getName());

            }
        };

        recVerlauf.setLayoutManager(new LinearLayoutManager(getContext()));
        recVerlauf.setAdapter(adapterVerlauf);
    }

    public class TransactionHolder extends RecyclerView.ViewHolder {
        TextView tv_title, tv_name, tv_datum;
        MoneyTextView tv_value;

        public TransactionHolder(View itemView) {
            super(itemView);
            tv_datum = itemView.findViewById(R.id.datum);
            tv_name = itemView.findViewById(R.id.name);
            tv_title = itemView.findViewById(R.id.title);
            tv_value = itemView.findViewById(R.id.value);
        }
    }

    public class EmptyHolder extends RecyclerView.ViewHolder {

        public EmptyHolder(View itemView) {
            super(itemView);
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
    private String userID;
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
    private final int BUFFER = 10000; // Millisekunden // entspricht 10 Sekunden
    private final int HIDE = 0;
    private final int SHOW = 1;
}
