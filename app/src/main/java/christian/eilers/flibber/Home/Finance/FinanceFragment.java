package christian.eilers.flibber.Home.Finance;


import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.app.Fragment;
import android.support.v7.widget.DividerItemDecoration;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.afollestad.materialdialogs.DialogAction;
import com.afollestad.materialdialogs.MaterialDialog;
import com.firebase.ui.firestore.FirestoreRecyclerAdapter;
import com.firebase.ui.firestore.FirestoreRecyclerOptions;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
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

import christian.eilers.flibber.FirestoreAdapter.BalanceAdapter;
import christian.eilers.flibber.FirestoreAdapter.VerlaufAdapter;
import christian.eilers.flibber.Home.HomeActivity;
import christian.eilers.flibber.MainActivity;
import christian.eilers.flibber.Models.Balancing;
import christian.eilers.flibber.Models.Offset;
import christian.eilers.flibber.Models.Payment;
import christian.eilers.flibber.Models.User;
import christian.eilers.flibber.R;
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
        if (hasNulls()) {
            Intent main = new Intent(getContext(), MainActivity.class);
            main.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
            startActivity(main);
            getActivity().overridePendingTransition(R.anim.fade_in, R.anim.fade_out);
            getActivity().finish();
        } else {
            loadBalance();
            loadVerlauf();
        }

        return mainView;
    }

    // Initialize views
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

    // check for null pointers
    private boolean hasNulls() {
        if (users == null || userID == null || groupID == null) return true;
        else return false;
    }

    // Load user's finance-balance
    private void loadBalance() {
        Query query = db.collection(GROUPS).document(groupID)
                .collection(USERS).orderBy(MONEY, Query.Direction.DESCENDING); // order by money-value

        FirestoreRecyclerOptions<User> options = new FirestoreRecyclerOptions.Builder<User>()
                .setQuery(query, User.class)
                .build();

        adapterBilanz = new BalanceAdapter(options);

        recBilanz.setLayoutManager(new LinearLayoutManager(getContext()));
        recBilanz.addItemDecoration(new DividerItemDecoration(recBilanz.getContext(), DividerItemDecoration.VERTICAL));
        recBilanz.setAdapter(adapterBilanz);

    }

    // Load the last transactions/payments
    private void loadVerlauf() {
        Query query = db.collection(GROUPS).document(groupID)
                .collection(FINANCES)
                .orderBy(TIMESTAMP, Query.Direction.DESCENDING) // order by date
                .whereGreaterThan(TIMESTAMP, new Date(System.currentTimeMillis() - ONE_WEEK)); // only from last 7 Days

        FirestoreRecyclerOptions<Payment> options = new FirestoreRecyclerOptions.Builder<Payment>()
                .setQuery(query, Payment.class)
                .build();

        users = ((HomeActivity) getActivity()).getUsers();
        adapterVerlauf = new VerlaufAdapter(options, userID, users);

        recVerlauf.setLayoutManager(new LinearLayoutManager(getContext()));
        recVerlauf.addItemDecoration(new DividerItemDecoration(recVerlauf.getContext(), DividerItemDecoration.VERTICAL));
        recVerlauf.setAdapter(adapterVerlauf);
    }

    private void balancingDialog() {
        new MaterialDialog.Builder(getContext())
                .title("Abrechnung")
                .content("Die Bilanz jedes Mitglieds wird auf 0,00 \u20ac zurückgesetzt.\n" +
                        "Alle Einträge im Verlauf bleiben erhalten.\n" +
                        "Die Ausgleichszahlungen sind anschließend unter \"Vergangene\" einsehbar.")
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
        db.runTransaction(new Transaction.Function<String>() {

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
            public String apply(@NonNull Transaction transaction) throws FirebaseFirestoreException {
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
                return ref_balance.getId();
            }
        }).addOnCompleteListener(new OnCompleteListener<String>() {
            @Override
            public void onComplete(@NonNull Task<String> task) {
                progressBar.setVisibility(View.GONE);
                Intent intent = new Intent(getContext(), BalanceActivity.class);
                intent.putExtra(BALANCING, task.getResult());
                intent.putExtra(USERS, ((HomeActivity) getActivity()).getUsers());
                getActivity().startActivity(intent);
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
                break;
            case R.id.zumVerlauf:
                Intent intentVerlauf = new Intent(getContext(), VerlaufActivity.class);
                intentVerlauf.putExtra(USERS, ((HomeActivity) getActivity()).getUsers());
                getActivity().startActivity(intentVerlauf);
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
