package christian.eilers.flibber.Home.Finance;

import android.app.Dialog;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.DialogFragment;
import android.app.DatePickerDialog;
import android.widget.DatePicker;

import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.functions.FirebaseFunctions;

import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.Map;

import christian.eilers.flibber.Models.TaskModel;
import christian.eilers.flibber.Utils.LocalStorage;

import static christian.eilers.flibber.Utils.Strings.*;

public class DatePickerFragment extends DialogFragment implements DatePickerDialog.OnDateSetListener {

    private String groupID, username;
    private TaskModel thisTask;
    private boolean called = false;

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
        if (called) return;
        called = true;
        Date date = new GregorianCalendar(year, month, dayOfMonth).getTime();
        HashMap<String, Object> changings = new HashMap<>();
        changings.put(TIMESTAMP, date);
        final FirebaseFirestore db = FirebaseFirestore.getInstance();
        final FirebaseFunctions functions = FirebaseFunctions.getInstance();
        DocumentReference doc = db.collection(GROUPS).document(groupID).collection(TASKS).document(thisTask.getKey());
        doc.update(changings);

        Map<String, Object> data = new HashMap<>();
        data.put("taskName", thisTask.getTitle());
        data.put("userName", username);
        data.put("groupID", groupID);
        data.put("involvedIDs", thisTask.getInvolvedIDs());
        // Calls the Http Function which makes the Notification
        functions.getHttpsCallable("taskDateChanged").call(data);
    }
}
