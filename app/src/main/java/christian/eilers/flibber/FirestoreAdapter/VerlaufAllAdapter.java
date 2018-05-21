package christian.eilers.flibber.FirestoreAdapter;

import android.content.Intent;
import android.graphics.Paint;
import android.support.annotation.NonNull;
import android.support.v7.widget.RecyclerView;
import android.text.format.DateUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.firebase.ui.firestore.FirestoreRecyclerAdapter;
import com.firebase.ui.firestore.FirestoreRecyclerOptions;

import org.fabiomsr.moneytextview.MoneyTextView;

import java.util.HashMap;

import christian.eilers.flibber.Home.Finance.TransactionDetailActivity;
import christian.eilers.flibber.Models.Payment;
import christian.eilers.flibber.Models.User;
import christian.eilers.flibber.R;

import static christian.eilers.flibber.Utils.Strings.BUFFER;
import static christian.eilers.flibber.Utils.Strings.TRANSACTIONID;
import static christian.eilers.flibber.Utils.Strings.USERS;

public class VerlaufAllAdapter extends FirestoreRecyclerAdapter<Payment, VerlaufAllAdapter.TransactionHolder>{

    private String userID;
    private HashMap<String, User> users;

    /**
     * Create a new RecyclerView adapter that listens to a Firestore Query.  See {@link
     * FirestoreRecyclerOptions} for configuration options.
     *
     * @param options
     */
    public VerlaufAllAdapter(@NonNull FirestoreRecyclerOptions<Payment> options, String userID, HashMap<String, User> users) {
        super(options);
        this.userID = userID;
        this.users = users;
    }

    // Create normal/empty Viewholder depending on whether the user is involved in this transaction
    @NonNull
    @Override
    public TransactionHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_transaction, parent, false);
        return new TransactionHolder(view);
    }

    @Override
    protected void onBindViewHolder(@NonNull final TransactionHolder holder, int position, @NonNull final Payment model) {
        // TITLE
        holder.tv_title.setText(model.getTitle());
        if (model.isDeleted())
            holder.tv_title.setPaintFlags(holder.tv_title.getPaintFlags() | Paint.STRIKE_THRU_TEXT_FLAG);
        else
            holder.tv_title.setPaintFlags(holder.tv_title.getPaintFlags() & (~ Paint.STRIKE_THRU_TEXT_FLAG));

        // CREATOR_NAME
        holder.tv_name.setText(users.get(model.getCreatorID()).getName());

        // Compute the Costs for the current user
        long partialPrice = Math.round((double) model.getPrice() / model.getInvolvedIDs().size());
        long totalPriceRounded = partialPrice * model.getInvolvedIDs().size();

        if (model.getInvolvedIDs().contains(userID)) {
            if (model.getPayerID().equals(userID))
                holder.tv_value.setAmount(totalPriceRounded - partialPrice);
            else holder.tv_value.setAmount(-partialPrice);
        } else {
            if (model.getPayerID().equals(userID))
                holder.tv_value.setAmount(totalPriceRounded);
            else {
                holder.tv_value.setAmount(0);
            }
        }

        // TIMESTAMP (Buffer um "in 0 Minuten"-Anzeige zu vermeiden)
        if (model.getTimestamp() != null)
            holder.tv_datum.setText(
                    DateUtils.getRelativeTimeSpanString(model.getTimestamp().getTime(),
                            System.currentTimeMillis() + BUFFER,
                            DateUtils.MINUTE_IN_MILLIS,
                            DateUtils.FORMAT_ABBREV_RELATIVE));

        // Open Detailed View of the current Transaction/Payment
        holder.itemView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (model.isDeleted()) return;
                Intent i_detailed = new Intent(holder.itemView.getContext(), TransactionDetailActivity.class);
                i_detailed.putExtra(TRANSACTIONID, model);
                i_detailed.putExtra(USERS, users);
                holder.itemView.getContext().startActivity(i_detailed);
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

}
