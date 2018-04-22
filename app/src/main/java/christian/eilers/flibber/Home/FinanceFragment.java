package christian.eilers.flibber.Home;


import android.content.Intent;
import android.graphics.Typeface;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.app.Fragment;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.afollestad.materialdialogs.DialogAction;
import com.afollestad.materialdialogs.MaterialDialog;
import com.blackcat.currencyedittext.CurrencyEditText;
import com.bumptech.glide.Glide;
import com.firebase.ui.firestore.FirestoreRecyclerAdapter;
import com.firebase.ui.firestore.FirestoreRecyclerOptions;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.Transaction;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import org.fabiomsr.moneytextview.MoneyTextView;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import christian.eilers.flibber.Adapter.VerlaufAdapter;
import christian.eilers.flibber.MainActivity;
import christian.eilers.flibber.Models.Balancing;
import christian.eilers.flibber.Models.Offset;
import christian.eilers.flibber.Models.Payment;
import christian.eilers.flibber.Models.User;
import christian.eilers.flibber.R;
import christian.eilers.flibber.Utils.GlideApp;
import christian.eilers.flibber.Utils.LocalStorage;
import de.hdodenhof.circleimageview.CircleImageView;

import static christian.eilers.flibber.Utils.Strings.*;

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
            protected void onBindViewHolder(@NonNull final UserHolder holder, int position, @NonNull final User model) {
                // USERNAME & MONEY
                holder.tv_username.setText(model.getName());
                if (model.getUserID().equals(userID)) holder.tv_username.setTypeface(null, Typeface.BOLD);
                else holder.tv_username.setTypeface(null, Typeface.NORMAL);
                holder.tv_money.setAmount(model.getMoney());

                // PROFILE PICTURE
                if(model.getPicPath() != null)
                    GlideApp.with(getContext())
                            .load(storage.child(PROFILE).child(model.getPicPath()))
                            .dontAnimate()
                            .placeholder(R.drawable.profile_placeholder)
                            .into(holder.img_profile);
                else Glide.with(getContext()).clear(holder.img_profile);

                // Open Dialog for "Quick-Transaction" with the clicked-User
                holder.itemView.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        if (userID.equals(model.getUserID())) return; // don't allow transaction with oneself
                        final MaterialDialog dialog = new MaterialDialog.Builder(getContext())
                                .title("Überweisung an " + model.getName())
                                .customView(R.layout.dialog_uberweisung, true)
                                .positiveText("Speichern")
                                .negativeText("Abbrechen")
                                .autoDismiss(false)
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
                                        if (TextUtils.isEmpty(description)) {
                                            Toast.makeText(getContext(), "Beschreibung notwendig!", Toast.LENGTH_SHORT).show();
                                            return;
                                        }

                                        saveTransaction(value, description);
                                        dialog.dismiss();
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
                                                //Toast.makeText(getContext(), "Erfolgreich überwiesen!", Toast.LENGTH_SHORT).show();
                                            }
                                        });
                                    }
                                })
                                .onNegative(new MaterialDialog.SingleButtonCallback() {
                                    @Override
                                    public void onClick(@NonNull MaterialDialog dialog, @NonNull DialogAction which) {
                                        dialog.dismiss();
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

                holder.img_profile.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        AlertDialog.Builder builder = new AlertDialog.Builder(v.getContext());
                        View v_dialog = getLayoutInflater().inflate(R.layout.dialog_profile_image, null);
                        ImageView img_profile = v_dialog.findViewById(R.id.profile_image);
                        // PROFILE PICTURE
                        if(model.getPicPath() != null)
                            GlideApp.with(v.getContext())
                                    .load(storage.child(PROFILE).child(model.getPicPath()))
                                    .dontAnimate()
                                    .placeholder(R.drawable.profile_placeholder)
                                    .into(img_profile);
                        builder.setView(v_dialog);
                        AlertDialog dialog = builder.create();
                        dialog.show();
                        final float scale = getContext().getResources().getDisplayMetrics().density;
                        final int dps = 250;
                        int pixels = (int) (dps * scale + 0.5f);
                        dialog.getWindow().setLayout(pixels, pixels);
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

        users = ((HomeActivity) getActivity()).getUsers();
        adapterVerlauf = new VerlaufAdapter(options, userID, users);

        recVerlauf.setLayoutManager(new LinearLayoutManager(getContext()));
        recVerlauf.setAdapter(adapterVerlauf);
    }

    private void balancingDialog() {
        new MaterialDialog.Builder(getContext())
                .title("Finanzausgleich")
                .content("Die Bilanz jedes Mitglieds wird auf 0,00 \u20ac zurückgesetzt.\n" +
                        "Die zu zahlenden Beträge sind anschließend unter \"Vergangene\" einsehbar.")
                .positiveText("Bestätigen")
                .neutralText("Vergangene")
                .onPositive(new MaterialDialog.SingleButtonCallback() {
                    @Override
                    public void onClick(@NonNull MaterialDialog dialog, @NonNull DialogAction which) {
                        balancing();
                    }
                })
                .onNeutral(new MaterialDialog.SingleButtonCallback() {
                    @Override
                    public void onClick(@NonNull MaterialDialog dialog, @NonNull DialogAction which) {
                        Intent intentVerlauf = new Intent(getContext(), VerlaufBalanceActivity.class);
                        intentVerlauf.putExtra(USERS, ((HomeActivity) getActivity()).getUsers());
                        getActivity().startActivity(intentVerlauf);
                    }
                })
                .show();
    }

    private void balancing() {
        progressBar.setVisibility(View.VISIBLE);
        final DocumentReference ref_group = db.collection(GROUPS).document(groupID);
        users = ((HomeActivity) getActivity()).getUsers();
        db.runTransaction(new Transaction.Function<Void>() {

            ArrayList<Offset> list_offsets;

            public ArrayList<User> offset(ArrayList<User> list_users) {
                User first = list_users.get(0);
                User last = list_users.get(list_users.size()-1);
                list_users.remove(0);
                list_users.remove(list_users.size()-1);
                Offset o;
                if (first.getMoney() < Math.abs(last.getMoney())) {
                    o = new Offset(last.getUserID(), first.getUserID(), first.getMoney());
                    last.setMoney(last.getMoney() + first.getMoney());
                    first.setMoney(0);
                } else {
                    o = new Offset(last.getUserID(), first.getUserID(), -last.getMoney());
                    first.setMoney(first.getMoney() + last.getMoney());
                    last.setMoney(0);
                }
                list_users.add(first);
                list_users.add(last);
                list_offsets.add(o);

                return list_users;
            }

            @Nullable
            @Override
            public Void apply(@NonNull Transaction transaction) throws FirebaseFirestoreException {
                list_offsets = new ArrayList<>();
                // Read out current money from each user
                HashMap<String, Long> map = new HashMap<>();
                ArrayList<User> list_user = new ArrayList<>();
                for (String key : users.keySet()) {
                    DocumentSnapshot snap_user = transaction.get(ref_group.collection(USERS).document(key));
                    map.put(key, snap_user.getLong(MONEY));
                    list_user.add(snap_user.toObject(User.class));
                }
                // Create a new balancing entry
                Balancing balancing = new Balancing(userID, map);
                DocumentReference ref_balance = ref_group.collection(BALANCING).document();
                transaction.set(ref_balance, balancing);
                // Set money for each user to zero
                for (String key : users.keySet()) {
                    HashMap<String, Object> map_money = new HashMap<>();
                    map_money.put(MONEY, 0);
                    transaction.update(ref_group.collection(USERS).document(key), map_money);
                }

                while(true) {
                    // Sorting by money
                    Collections.sort(list_user, new Comparator<User>() {
                        @Override
                        public int compare(User user2, User user1)
                        {
                            if (user1.getMoney() < user2.getMoney()) return -1;
                            return 1;
                        }
                    });
                    if (list_user.get(0).getMoney() == 0) break;
                    list_user = offset(list_user);
                };

                for (Offset o : list_offsets) {
                    transaction.set(ref_balance.collection(ENTRIES).document(), o);
                }
                return null;
            }
        }).addOnCompleteListener(new OnCompleteListener<Void>() {
            @Override
            public void onComplete(@NonNull Task<Void> task) {
                progressBar.setVisibility(View.GONE);
            }
        });
    }

    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.fab:
                Intent intentTransaction = new Intent(getContext(), TransactionActivity.class);
                intentTransaction.putExtra(USERS, ((HomeActivity) getActivity()).getUsers());
                getActivity().startActivity(intentTransaction);
                //getActivity().overridePendingTransition(R.anim.fade_in, R.anim.fade_out);
                break;
            case R.id.zumVerlauf:
                Intent intentVerlauf = new Intent(getContext(), VerlaufActivity.class);
                intentVerlauf.putExtra(USERS, ((HomeActivity) getActivity()).getUsers());
                getActivity().startActivity(intentVerlauf);
                //getActivity().overridePendingTransition(R.anim.fade_in, R.anim.fade_out);
                break;
        }
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);
        inflater.inflate(R.menu.menu_finance, menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_kassensturz:
                balancingDialog();
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
    private RecyclerView recBilanz, recVerlauf;
    private FirestoreRecyclerAdapter adapterBilanz, adapterVerlauf;
    private FloatingActionButton fab;
    private RelativeLayout btn_verlauf;
    private ProgressBar progressBar;
}
