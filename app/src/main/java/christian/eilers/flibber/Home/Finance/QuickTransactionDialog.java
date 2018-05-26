package christian.eilers.flibber.Home.Finance;

import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.TextUtils;
import android.view.View;
import android.view.WindowManager;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.blackcat.currencyedittext.CurrencyEditText;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.firestore.Transaction;

import java.util.ArrayList;
import java.util.Locale;

import christian.eilers.flibber.Models.NotificationModel;
import christian.eilers.flibber.Models.Payment;
import christian.eilers.flibber.Models.User;
import christian.eilers.flibber.R;
import christian.eilers.flibber.RecyclerAdapter.UserSelectionAdapter;
import christian.eilers.flibber.Utils.LocalStorage;

import static christian.eilers.flibber.Utils.Strings.FINANCES;
import static christian.eilers.flibber.Utils.Strings.GROUPS;
import static christian.eilers.flibber.Utils.Strings.MONEY;
import static christian.eilers.flibber.Utils.Strings.NOTIFICATIONS;
import static christian.eilers.flibber.Utils.Strings.USERS;

public class QuickTransactionDialog extends Dialog {

    public QuickTransactionDialog(@NonNull Context context, int themeResId, User user) {
        super(context, themeResId);
        // set the background to a translucent black
        getWindow().setBackgroundDrawableResource(R.color.translucent_black);
        this.context = context;
        this.user = user;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.dialog_quick_transaction);

        tv_title = findViewById(R.id.tv_title);
        et_price = findViewById(R.id.input_price);
        et_description = findViewById(R.id.input_description);
        btn_save = findViewById(R.id.btn_save);
        btn_cancel = findViewById(R.id.btn_cancel);
        progressBar = findViewById(R.id.progressBar);

        et_price.setLocale(Locale.GERMANY);
        et_price.configureViewForLocale(Locale.GERMANY);

        String[] user_names = user.getName().split(" ", 2);
        tv_title.setText("Überweisung an " + user_names[0]);

        btn_cancel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dismiss();
            }
        });

        btn_save.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                final long value = et_price.getRawValue();
                final String description = et_description.getText().toString().trim();
                if (value <= 0) return;
                if (TextUtils.isEmpty(description)) {
                    Toast.makeText(getContext(), "Beschreibung notwendig!", Toast.LENGTH_SHORT).show();
                    return;
                }

                saveTransaction(value, description);
            }
        });
    }

    // Save the Payment to the database and offset the costs/money
    private void saveTransaction(final long value, final String description) {
        // Check if no view has focus:
        View view = this.getCurrentFocus();
        if (view != null) {
            InputMethodManager imm = (InputMethodManager)context.getSystemService(Context.INPUT_METHOD_SERVICE);
            imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
        }
        progressBar.setVisibility(View.VISIBLE);
        final FirebaseFirestore db = FirebaseFirestore.getInstance();
        final String userID = LocalStorage.getUserID(context);
        final String groupID = LocalStorage.getGroupID(context);
        final CollectionReference ref_finances = db.collection(GROUPS).document(groupID).collection(FINANCES);
        final CollectionReference ref_users = db.collection(GROUPS).document(groupID).collection(USERS);
        final DocumentReference ref_not = ref_users.document(user.getUserID()).collection(NOTIFICATIONS).document();

        ArrayList<String> involved = new ArrayList<>(); // Save the current user as Array-List
        involved.add(userID);

        final Payment payment = new Payment(        // Create the Payment-Object
                ref_finances.document().getId(),
                "Überweisung",
                description,
                user.getUserID(),
                userID,
                involved,
                value
        );
        final String not_description = "Neue Überweisung \"" + description + "\"";
        final NotificationModel not = new NotificationModel(
                ref_not.getId(), not_description,
                FINANCES, userID);

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
                transaction.set(ref_not, not);
                return null;
            }
        }).addOnSuccessListener(new OnSuccessListener<Void>() {
            @Override
            public void onSuccess(Void aVoid) {
                progressBar.setVisibility(View.GONE);
                dismiss();
            }
        });
    }

    private Context context;
    private User user;

    private TextView tv_title;
    private CurrencyEditText et_price;
    private EditText et_description;
    private Button btn_save, btn_cancel;
    private ProgressBar progressBar;
}
