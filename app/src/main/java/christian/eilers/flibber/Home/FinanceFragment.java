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
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.afollestad.materialdialogs.DialogAction;
import com.afollestad.materialdialogs.MaterialDialog;
import com.blackcat.currencyedittext.CurrencyEditText;
import com.firebase.ui.firestore.FirestoreRecyclerAdapter;
import com.firebase.ui.firestore.FirestoreRecyclerOptions;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.Transaction;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import org.fabiomsr.moneytextview.MoneyTextView;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;

import christian.eilers.flibber.Adapter.VerlaufAdapter;
import christian.eilers.flibber.MainActivity;
import christian.eilers.flibber.Models.Payment;
import christian.eilers.flibber.Models.User;
import christian.eilers.flibber.R;
import christian.eilers.flibber.Utils.GlideApp;
import christian.eilers.flibber.Utils.LocalStorage;
import de.hdodenhof.circleimageview.CircleImageView;

public class FinanceFragment extends Fragment implements View.OnClickListener{

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
        btn_verlauf = mainView.findViewById(R.id.zumVerlauf);
        progressBar = mainView.findViewById(R.id.progressBar);

        fab.setOnClickListener(this);
        btn_verlauf.setOnClickListener(this);
    }

    // Initialize variables
    private void initializeVariables() {
        db = FirebaseFirestore.getInstance();
        storage = FirebaseStorage.getInstance().getReference();
        userID = LocalStorage.getUserID(getContext());
        groupID = LocalStorage.getGroupID(getContext());
        users = ((HomeActivity) getActivity()).getUsers();

        setHasOptionsMenu(true);
    }

    // Load user's finance-balance
    private void loadBilanz() {
        Query query = db.collection(GROUPS).document(groupID)
                .collection(USERS).orderBy(MONEY, Query.Direction.DESCENDING); // order by money-value

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
            protected void onBindViewHolder(@NonNull UserHolder holder, int position, @NonNull final User model) {
                // USERNAME & MONEY
                holder.tv_username.setText(model.getName());
                holder.tv_money.setAmount(model.getMoney());

                // PROFILE PICTURE
                if(model.getPicPath() != null)
                    GlideApp.with(getContext())
                            .load(storage.child(PROFILE).child(model.getPicPath()))
                            .dontAnimate()
                            .placeholder(R.drawable.profile_placeholder)
                            .into(holder.img_profile);

                // Open Dialog for "Quick-Transaction" with the clicked-User
                holder.itemView.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        if (userID.equals(model.getUserID())) return; // don't allow transaction with oneself
                        MaterialDialog dialog = new MaterialDialog.Builder(getContext())
                                .title("Überweisung an " + model.getName())
                                .customView(R.layout.dialog_uberweisung, true)
                                .positiveText("Speichern")
                                .neutralText("Abbrechen")
                                .onPositive(new MaterialDialog.SingleButtonCallback() {
                                    // Read out the input value & description and save the transaction
                                    @Override
                                    public void onClick(@NonNull MaterialDialog dialog, @NonNull DialogAction which) {
                                        View v = dialog.getCustomView();
                                        CurrencyEditText et_value = v.findViewById(R.id.input_price);
                                        EditText et_description = v.findViewById(R.id.input_description);

                                        final long value = et_value.getRawValue();
                                        final String description = et_description.getText().toString().trim();
                                        if (value <= 0) return;
                                        if (TextUtils.isEmpty(description)) return;

                                        saveTransaction(value, description);
                                    }

                                    // Save the Payment to the database and offset the costs/money
                                    private void saveTransaction(final long value, final String description) {
                                        progressBar.setVisibility(View.VISIBLE);
                                        final CollectionReference ref_finances = db.collection(GROUPS).document(groupID).collection(FINANCES);
                                        final CollectionReference ref_users = db.collection(GROUPS).document(groupID).collection(USERS);

                                        ArrayList<String> involved = new ArrayList<>(); // Save the current user as Array-List
                                        involved.add(userID);

                                        final Payment payment = new Payment(        // Create the Payment-Object
                                                ref_finances.document().getId(),
                                                "Überweisung",
                                                description,
                                                model.getUserID(),
                                                userID,
                                                involved,
                                                value
                                        );

                                        // Run a Transaction to charge the costs between the two user's and save the payment
                                        // in the finance-collection
                                        db.runTransaction(new Transaction.Function<Void>() {
                                            @Nullable
                                            @Override
                                            public Void apply(@NonNull Transaction transaction) throws FirebaseFirestoreException {
                                                DocumentSnapshot snap_from = transaction.get(ref_users.document(userID));
                                                long money_from = snap_from.getLong(MONEY) - payment.getPrice();
                                                DocumentSnapshot snap_to = transaction.get(ref_users.document(payment.getPayerID()));
                                                long money_to = snap_to.getLong(MONEY) + payment.getPrice();

                                                transaction.update(ref_users.document(userID), MONEY, money_from);
                                                transaction.update(ref_users.document(payment.getPayerID()), MONEY, money_to);
                                                transaction.set(ref_finances.document(payment.getKey()), payment);
                                                return null;
                                            }
                                        }).addOnSuccessListener(new OnSuccessListener<Void>() {
                                            @Override
                                            public void onSuccess(Void aVoid) {
                                                progressBar.setVisibility(View.GONE);
                                                Toast.makeText(getContext(), "Erfolgreich überwiesen!", Toast.LENGTH_SHORT).show();
                                            }
                                        });
                                    }
                                })
                                .show();
                        // Show the Keyboard
                        dialog.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE);
                        CurrencyEditText et_price = dialog.getCustomView().findViewById(R.id.input_price);
                        et_price.setLocale(Locale.GERMANY);
                        et_price.configureViewForLocale(Locale.GERMANY);
                    }
                });
            }
        };

        recBilanz.setLayoutManager(new LinearLayoutManager(getContext()));
        recBilanz.setAdapter(adapterBilanz);

    }

    // Custom Viewholder for the User's
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

    // Load the last 5 transactions/payments
    private void loadVerlauf() {
        Query query = db.collection(GROUPS).document(groupID)
                .collection(FINANCES)
                .orderBy(TIMESTAMP, Query.Direction.DESCENDING) // order by date
                .whereGreaterThan(TIMESTAMP, new Date(System.currentTimeMillis() - 7L * 24 * 3600 * 1000)); // only from last 7 Days

        FirestoreRecyclerOptions<Payment> options = new FirestoreRecyclerOptions.Builder<Payment>()
                .setQuery(query, Payment.class)
                .build();

        adapterVerlauf = new VerlaufAdapter(options, userID, users);

        recVerlauf.setLayoutManager(new LinearLayoutManager(getContext()));
        recVerlauf.setAdapter(adapterVerlauf);
    }

    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.fab:
                getActivity().startActivity(new Intent(getContext(), TransactionActivity.class));
                getActivity().overridePendingTransition(R.anim.fade_in, R.anim.fade_out);
                break;
            case R.id.zumVerlauf:
                getActivity().startActivity(new Intent(getContext(), VerlaufActivity.class));
                getActivity().overridePendingTransition(R.anim.fade_in, R.anim.fade_out);
                break;
        }
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.menu_finance, menu);
        super.onCreateOptionsMenu(menu, inflater);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_kassensturz:
                // TODO: KASSENSTURZ
                return true;
            default:
                return super.onOptionsItemSelected(item);
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
    private RelativeLayout btn_verlauf;
    private ProgressBar progressBar;

    private final String GROUPS = "groups";
    private final String USERS = "users";
    private final String FINANCES = "finances";
    private final String TIMESTAMP = "timestamp";
    private final String MONEY = "money";
    private final String PROFILE = "profile";
}
