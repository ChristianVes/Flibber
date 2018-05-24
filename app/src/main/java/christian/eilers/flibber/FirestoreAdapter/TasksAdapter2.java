package christian.eilers.flibber.FirestoreAdapter;

import android.support.annotation.NonNull;
import android.support.v7.widget.RecyclerView;
import android.text.format.DateUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.firebase.ui.firestore.FirestoreRecyclerAdapter;
import com.firebase.ui.firestore.FirestoreRecyclerOptions;

import christian.eilers.flibber.Models.TaskModel;
import christian.eilers.flibber.R;

public class TasksAdapter2 extends FirestoreRecyclerAdapter<TaskModel, RecyclerView.ViewHolder>{

    private String userID;

    private final int HIDE = 0;
    private final int SHOW = 1;

    public TasksAdapter2(@NonNull FirestoreRecyclerOptions<TaskModel> options, String userID) {
        super(options);
        this.userID = userID;
    }

    // Create normal/empty Viewholder depending on whether the user is involved in this task
    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        switch (viewType) {
            case SHOW: {
                View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_task_small, parent, false);
                return new TaskHolder(view);
            }
            default: {
                View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_empty, parent, false);
                return new EmptyHolder(view);
            }
        }
    }

    // Determine whether the user is involved/first one in the task at the given adapter-position
    @Override
    public int getItemViewType(int position) {
        TaskModel model = getSnapshots().get(position);
        if (!model.isOrdered() && model.getInvolvedIDs().contains(userID) ||
                model.isOrdered() && model.getInvolvedIDs().get(0).equals(userID)) {
            return SHOW;
        }

        return HIDE;
    }

    @Override
    protected void onBindViewHolder(@NonNull final RecyclerView.ViewHolder holder, final int position, @NonNull final TaskModel model) {
        if (holder.getItemViewType() == HIDE) return; // show nothing if user is not involved
        // specialize the ViewHolder as TransactionHolder to bind the data to the item
        final TaskHolder taskHolder = (TaskHolder) holder;

        // TITLE
        taskHolder.tv_title.setText(model.getTitle());

        // DATE
        taskHolder.tv_datum.setText(DateUtils.getRelativeTimeSpanString(model.getTimestamp().getTime(),
                System.currentTimeMillis(),
                DateUtils.DAY_IN_MILLIS,
                DateUtils.FORMAT_ABBREV_RELATIVE));
    }

    // Custom ViewHolder for a Task
    public class TaskHolder extends RecyclerView.ViewHolder {
        TextView tv_title, tv_datum;

        public TaskHolder(View itemView) {
            super(itemView);
            tv_datum = itemView.findViewById(R.id.date);
            tv_title = itemView.findViewById(R.id.title);
        }
    }

    // Just an Empty Viewholder (for tasks the current user is not involved in)
    public class EmptyHolder extends RecyclerView.ViewHolder {
        public EmptyHolder(View itemView) {
            super(itemView);
        }
    }
}
