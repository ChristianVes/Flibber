package christian.eilers.flibber.Adapter;

import android.content.Intent;
import android.support.annotation.NonNull;
import android.support.v7.widget.RecyclerView;
import android.text.format.DateUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.crashlytics.android.Crashlytics;
import com.firebase.ui.firestore.FirestoreRecyclerAdapter;
import com.firebase.ui.firestore.FirestoreRecyclerOptions;

import org.fabiomsr.moneytextview.MoneyTextView;

import java.util.HashMap;

import christian.eilers.flibber.Home.TransactionDetailActivity;
import christian.eilers.flibber.Models.Payment;
import christian.eilers.flibber.Models.User;
import christian.eilers.flibber.R;

import static christian.eilers.flibber.Utils.Strings.*;

public class VerlaufAdapter extends FirestoreRecyclerAdapter<Payment, RecyclerView.ViewHolder>{

    private final int HIDE = 0;
    private final int SHOW = 1;
    private String userID;
    private HashMap<String, User> users;

    /**
     * Create a new RecyclerView adapter that listens to a Firestore Query.  See {@link
     * FirestoreRecyclerOptions} for configuration options.
     *
     * @param options
     */
    public VerlaufAdapter(@NonNull FirestoreRecyclerOptions<Payment> options, String userID, HashMap<String, User> users) {
        super(options);
        this.userID = userID;
        this.users = users;
    }

    // Create normal/empty Viewholder depending on whether the user is involved in this transaction
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

    // Determine whether the user is involved in the transaction at the given adapter-position
    @Override
    public int getItemViewType(int position) {
        final Payment thisPayment = getSnapshots().get(position);
        boolean isPayer = thisPayment.getPayerID().equals(userID);
        boolean isInvolved = thisPayment.getInvolvedIDs().contains(userID);
        boolean isDeleted = thisPayment.isDeleted();
        if ((isPayer || isInvolved) && !isDeleted) return SHOW;
        return HIDE;
    }

    @Override
    protected void onBindViewHolder(@NonNull final RecyclerView.ViewHolder holder, int position, @NonNull final Payment model) {
        if (holder.getItemViewType() == HIDE) return; // show nothing if user is not involved

        // specialize the ViewHolder as TransactionHolder to bind the data to the item
        final TransactionHolder transHolder = (TransactionHolder) holder;

        transHolder.tv_title.setText(model.getTitle()); // set the Title (of the transaction)
        transHolder.tv_name.setText(users.get(model.getCreatorID()).getName()); // set Creator-Name

        // Compute the Costs for the current user
        long partialPrice = Math.round((double) model.getPrice() / model.getInvolvedIDs().size());
        long totalPriceRounded = partialPrice * model.getInvolvedIDs().size();

        if (model.getInvolvedIDs().contains(userID)) {
            if (model.getPayerID().equals(userID))
                transHolder.tv_value.setAmount(totalPriceRounded - partialPrice);
            else transHolder.tv_value.setAmount(-partialPrice);
        } else {
            if (model.getPayerID().equals(userID))
                transHolder.tv_value.setAmount(totalPriceRounded);
            else Crashlytics.logException(new Exception("Falscher Viewholder angezeigt!")); // should never get in this case
        }

        // TIMESTAMP (Buffer um "in 0 Minuten"-Anzeige zu vermeiden)
        if (model.getTimestamp() != null)
            transHolder.tv_datum.setText(
                    DateUtils.getRelativeTimeSpanString(model.getTimestamp().getTime(),
                            System.currentTimeMillis() + BUFFER,
                            DateUtils.MINUTE_IN_MILLIS,
                            DateUtils.FORMAT_ABBREV_RELATIVE));

        // Open Detailed View of the current Transaction/Payment
        transHolder.itemView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent i_detailed = new Intent(transHolder.itemView.getContext(), TransactionDetailActivity.class);
                i_detailed.putExtra(TRANSACTIONID, model.getKey());
                i_detailed.putExtra(USERS, users);
                transHolder.itemView.getContext().startActivity(i_detailed);
            }
        });
    }

    // Custom ViewHolder for a Transaction
    public class TransactionHolder extends RecyclerView.ViewHolder {
        TextView tv_title, tv_name, tv_datum, tv_text;
        MoneyTextView tv_value;

        public TransactionHolder(View itemView) {
            super(itemView);
            tv_datum = itemView.findViewById(R.id.datum);
            tv_name = itemView.findViewById(R.id.name);
            tv_title = itemView.findViewById(R.id.title);
            tv_value = itemView.findViewById(R.id.value);
            tv_text = itemView.findViewById(R.id.text);
        }
    }

    // Just an Empty Viewholder (for transactions the current user is not involved in)
    public class EmptyHolder extends RecyclerView.ViewHolder {

        public EmptyHolder(View itemView) {
            super(itemView);
        }
    }

}
