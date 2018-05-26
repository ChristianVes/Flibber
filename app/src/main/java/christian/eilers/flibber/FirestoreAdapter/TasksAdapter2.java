package christian.eilers.flibber.FirestoreAdapter;

import android.content.Intent;
import android.support.annotation.NonNull;
import android.support.v7.widget.RecyclerView;
import android.text.format.DateUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import com.firebase.ui.firestore.FirestoreRecyclerAdapter;
import com.firebase.ui.firestore.FirestoreRecyclerOptions;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.WriteBatch;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.concurrent.TimeUnit;

import christian.eilers.flibber.Home.TaskActivity;
import christian.eilers.flibber.Models.TaskEntry;
import christian.eilers.flibber.Models.TaskModel;
import christian.eilers.flibber.Models.User;
import christian.eilers.flibber.R;
import christian.eilers.flibber.Utils.LocalStorage;

import static christian.eilers.flibber.Utils.Strings.ENTRIES;
import static christian.eilers.flibber.Utils.Strings.GROUPS;
import static christian.eilers.flibber.Utils.Strings.INVOLVEDIDS;
import static christian.eilers.flibber.Utils.Strings.TASKID;
import static christian.eilers.flibber.Utils.Strings.TASKS;
import static christian.eilers.flibber.Utils.Strings.TIMESTAMP;
import static christian.eilers.flibber.Utils.Strings.USERS;

public class TasksAdapter2 extends FirestoreRecyclerAdapter<TaskModel, RecyclerView.ViewHolder> {

    private String userID, groupID;
    private HashMap<String, User> users;

    private final int HIDE = 0;
    private final int SHOW = 1;

    public TasksAdapter2(@NonNull FirestoreRecyclerOptions<TaskModel> options, HashMap<String, User> users, String userID, String groupID) {
        super(options);
        this.users = users;
        this.userID = userID;
        this.groupID = groupID;
    }

    // Create normal/empty Viewholder depending on whether the user is involved in this task
    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        userID = LocalStorage.getUserID(parent.getContext());
        groupID = LocalStorage.getGroupID(parent.getContext());
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

        taskHolder.itemView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent i = new Intent(taskHolder.itemView.getContext(), TaskActivity.class);
                i.putExtra(TASKID, model);
                i.putExtra(USERS, users);
                taskHolder.itemView.getContext().startActivity(i);
            }
        });
        taskHolder.btn_done.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                taskDone(taskHolder, model);
            }
        });
    }

    private void taskDone(TaskHolder taskHolder, TaskModel model) {
        // Change order of the involved user's
        ArrayList<String> newOrder = (ArrayList<String>) model.getInvolvedIDs().clone();
        newOrder.remove(userID);
        newOrder.add(userID);
        // Update Involved-User's and Timestamp
        final HashMap<String, Object> taskMap = new HashMap<>();
        taskMap.put(TIMESTAMP, new Date(System.currentTimeMillis() + TimeUnit.DAYS.toMillis(model.getFrequenz())));
        taskMap.put(INVOLVEDIDS, newOrder);
        // Create a TaskEntry
        final TaskEntry entry = new TaskEntry(userID);

        FirebaseFirestore db = FirebaseFirestore.getInstance();
        final DocumentReference docTask = db.collection(GROUPS).document(groupID).collection(TASKS).document(model.getKey());
        final DocumentReference docEntry = db.collection(GROUPS).document(groupID).collection(TASKS)
                .document(model.getKey()).collection(ENTRIES).document();

        WriteBatch batch = db.batch();
        batch.update(docTask, taskMap);
        batch.set(docEntry, entry);
        batch.commit();
        Toast.makeText(taskHolder.itemView.getContext(), model.getTitle() +" erledigt!", Toast.LENGTH_SHORT).show();
    }

    // Custom ViewHolder for a Task
    public class TaskHolder extends RecyclerView.ViewHolder {
        TextView tv_title, tv_datum;
        ImageButton btn_done;

        public TaskHolder(View itemView) {
            super(itemView);
            tv_datum = itemView.findViewById(R.id.date);
            tv_title = itemView.findViewById(R.id.title);
            btn_done = itemView.findViewById(R.id.btn_done);
        }
    }

    // Just an Empty Viewholder (for tasks the current user is not involved in)
    public class EmptyHolder extends RecyclerView.ViewHolder {
        public EmptyHolder(View itemView) {
            super(itemView);
        }
    }
}
