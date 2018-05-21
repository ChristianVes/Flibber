package christian.eilers.flibber.FirestoreAdapter;

import android.content.Intent;
import android.support.annotation.NonNull;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.firebase.ui.firestore.FirestoreRecyclerAdapter;
import com.firebase.ui.firestore.FirestoreRecyclerOptions;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.WriteBatch;

import java.util.HashMap;

import christian.eilers.flibber.Home.StockActivity;
import christian.eilers.flibber.Home.StockProductActivity;
import christian.eilers.flibber.Models.Article;
import christian.eilers.flibber.Models.StockProduct;
import christian.eilers.flibber.Models.User;
import christian.eilers.flibber.R;

import static christian.eilers.flibber.Utils.Strings.*;

public class StockAdapter extends FirestoreRecyclerAdapter<StockProduct, RecyclerView.ViewHolder>{

    private String userID, groupID;
    private HashMap<String, User> users;
    private StockActivity activity;
    private CollectionReference ref_stock, ref_users;

    private final int HIDE = 0;
    private final int SHOW = 1;

    /**
     * Create a new RecyclerView adapter that listens to a Firestore Query.  See {@link
     * FirestoreRecyclerOptions} for configuration options.
     *
     * @param options
     */
    public StockAdapter(@NonNull FirestoreRecyclerOptions<StockProduct> options, StockActivity activity,
                        String userID, String groupID, HashMap<String, User> users) {
        super(options);
        this.activity = activity;
        this.userID = userID;
        this.groupID = groupID;
        this.users = users;
        ref_users = FirebaseFirestore.getInstance().collection(GROUPS).document(groupID).collection(USERS);
        ref_stock = FirebaseFirestore.getInstance().collection(GROUPS).document(groupID).collection(STOCK);
    }

    // Create normal/empty Viewholder depending on whether the user is involved in this task
    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        switch (viewType) {
            case SHOW: {
                View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_stock, parent, false);
                return new StockHolder(view);
            }
            default: {
                View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_empty, parent, false);
                return new EmptyHolder(view);
            }
        }
    }

    // Determine whether the user is involved in the task at the given adapter-position
    @Override
    public int getItemViewType(int position) {
        boolean isInvolved = getSnapshots().get(position).getInvolvedIDs().contains(userID);
        if (isInvolved) return SHOW;
        return HIDE;
    }

    @Override
    protected void onBindViewHolder(@NonNull final RecyclerView.ViewHolder holder, final int position, @NonNull final StockProduct model) {
        if (holder.getItemViewType() == HIDE) return; // show nothing if user is not involved
        // specialize the ViewHolder as StockHolder to bind the data to the item
        final StockHolder taskHolder = (StockHolder) holder;

        // NAME & COUNT
        taskHolder.tv_name.setText(model.getName());
        if (model.getPurchaserIDs().size() == 0)
            taskHolder.tv_count.setText("Nicht vorhanden");
        else if (model.getPurchaserIDs().size() == 1)
            taskHolder.tv_count.setText("Vorhanden");
        else
            taskHolder.tv_count.setText(model.getPurchaserIDs().size() +" vorhanden");

        // Group Icon Visibility
        if (model.getInvolvedIDs().size() > 1)
            taskHolder.ic_group.setVisibility(View.VISIBLE);
        else taskHolder.ic_group.setVisibility(View.GONE);

        // open detailed view on middle part clicked
        taskHolder.itemView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent i = new Intent(activity, StockProductActivity.class);
                i.putExtra(STOCKID, model);
                i.putExtra(USERS, users);
                activity.startActivity(i);
            }
        });

        // add the userID to the end of the purchaser's list
        taskHolder.btn_add.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                model.getPurchaserIDs().add(userID);
                ref_stock.document(model.getKey()).set(model);
            }
        });

        // remove the first userID of the purchaser's list
        // add the product to the shopping list for all involved if count got from 1 to 0
        taskHolder.btn_remove.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                int count = model.getPurchaserIDs().size();
                if (count == 0) return;
                if (count == 1) {
                    Article article = new Article(model.getKey(),
                            model.getName(),
                            userID,
                            model.getInvolvedIDs().size() == 1);
                    WriteBatch batch = FirebaseFirestore.getInstance().batch();
                    for (String id : model.getInvolvedIDs()) {
                        batch.set(ref_users.document(id).collection(SHOPPING).document(article.getKey()), article);
                    }
                    batch.commit();
                }
                model.getPurchaserIDs().remove(count-1);
                ref_stock.document(model.getKey()).set(model);
            }
        });

    }

    // Custom ViewHolder for a Task
    public class StockHolder extends RecyclerView.ViewHolder {
        ImageButton btn_add, btn_remove;
        ImageView ic_group;
        TextView tv_name, tv_count;

        public StockHolder(View itemView) {
            super(itemView);
            tv_name = itemView.findViewById(R.id.name);
            tv_count = itemView.findViewById(R.id.count);
            btn_add = itemView.findViewById(R.id.btn_add);
            btn_remove = itemView.findViewById(R.id.btn_remove);
            ic_group = itemView.findViewById(R.id.ic_group);
        }
    }

    // Just an Empty Viewholder (for tasks the current user is not involved in)
    public class EmptyHolder extends RecyclerView.ViewHolder {
        public EmptyHolder(View itemView) {
            super(itemView);
        }
    }
}
