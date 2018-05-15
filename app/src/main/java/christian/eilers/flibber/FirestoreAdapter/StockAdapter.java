package christian.eilers.flibber.FirestoreAdapter;

import android.support.annotation.NonNull;
import android.support.v7.widget.RecyclerView;
import android.text.format.DateUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;

import com.firebase.ui.firestore.FirestoreRecyclerAdapter;
import com.firebase.ui.firestore.FirestoreRecyclerOptions;

import java.util.HashMap;

import christian.eilers.flibber.Home.StockActivity;
import christian.eilers.flibber.Models.StockProduct;
import christian.eilers.flibber.Models.TaskModel;
import christian.eilers.flibber.Models.User;
import christian.eilers.flibber.R;

public class StockAdapter extends FirestoreRecyclerAdapter<StockProduct, RecyclerView.ViewHolder>{

    private String userID, groupID;
    private HashMap<String, User> users;
    private StockActivity activity;

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
        final StockHolder taskHolder = (StockAdapter.StockHolder) holder;

        taskHolder.tv_name.setText(model.getName());
        taskHolder.tv_count.setText(model.getPurchaserIDs().size() + "");

    }

    // Custom ViewHolder for a Task
    public class StockHolder extends RecyclerView.ViewHolder {
        // ImageButton btn_add, btn_remove;
        TextView tv_name, tv_count;

        public StockHolder(View itemView) {
            super(itemView);
            tv_name = itemView.findViewById(R.id.name);
            tv_count = itemView.findViewById(R.id.count);
        }
    }

    // Just an Empty Viewholder (for tasks the current user is not involved in)
    public class EmptyHolder extends RecyclerView.ViewHolder {
        public EmptyHolder(View itemView) {
            super(itemView);
        }
    }
}
