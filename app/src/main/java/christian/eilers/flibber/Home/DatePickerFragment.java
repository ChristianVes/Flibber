package christian.eilers.flibber.Home;

import android.app.Dialog;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.DialogFragment;
import android.app.DatePickerDialog;
import android.widget.DatePicker;

import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.WriteBatch;
import com.google.firebase.functions.FirebaseFunctions;

import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.Map;

import christian.eilers.flibber.Models.NotificationModel;
import christian.eilers.flibber.Models.TaskModel;
import christian.eilers.flibber.Utils.LocalStorage;

import static christian.eilers.flibber.Utils.Strings.*;

public class DatePickerFragment extends DialogFragment implements DatePickerDialog.OnDateSetListener {

    private String groupID, userID, username;
    private TaskModel thisTask;
    private boolean called = false;

    // creates a new instance with the given objects
    public static DatePickerFragment newInstance(String groupID, TaskModel thisTask) {
        DatePickerFragment f = new DatePickerFragment();
        Bundle args = new Bundle();
        args.putString(GROUPID, groupID);
        args.putSerializable(TASKID, thisTask);
        f.setArguments(args);
        return f;
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        this.groupID = getArguments().getString(GROUPID);
        this.thisTask = (TaskModel) getArguments().getSerializable(TASKID);
        username = LocalStorage.getUsername(getContext());
        userID = LocalStorage.getUserID(getContext());
        // Use the current date as the default date in the picker
        final Calendar c = Calendar.getInstance();
        int year = c.get(Calendar.YEAR);
        int month = c.get(Calendar.MONTH);
        int day = c.get(Calendar.DAY_OF_MONTH);

        // Create a new instance of DatePickerDialog and return it
        return new DatePickerDialog(getActivity(), this, year, month, day);
    }

    @Override
    public void onDateSet(DatePicker view, int year, int month, int dayOfMonth) {
        // avoid that the function is executed a second time
        if (called) return;
        called = true;
        // Update the date/timestamp in the database
        Date date = new GregorianCalendar(year, month, dayOfMonth).getTime();
        HashMap<String, Object> changings = new HashMap<>();
        changings.put(TIMESTAMP, date);
        final FirebaseFirestore db = FirebaseFirestore.getInstance();
        final FirebaseFunctions functions = FirebaseFunctions.getInstance();
        final DocumentReference doc_task = db.collection(GROUPS).document(groupID).collection(TASKS).document(thisTask.getKey());

        String not_description = "Datum von \"" + thisTask.getTitle() + "\" geändert";

        WriteBatch batch = db.batch();
        for (String id : thisTask.getInvolvedIDs()) {
            if (id.equals(userID)) continue;
            DocumentReference doc = db.collection(GROUPS).document(groupID).collection(USERS).document(id).collection(NOTIFICATIONS).document();
            NotificationModel not = new NotificationModel(doc.getId(), not_description, TASKS, userID);
            batch.set(doc, not);
        }
        batch.update(doc_task, changings);
        batch.commit();

        // Call the corresponding function to notify the other users
        Map<String, Object> data = new HashMap<>();
        data.put("taskName", thisTask.getTitle());
        data.put("userName", username);
        data.put("groupID", groupID);
        data.put("involvedIDs", thisTask.getInvolvedIDs());
        // Calls the Http Function which makes the Notification
        //functions.getHttpsCallable("taskDateChanged").call(data);
    }
}
