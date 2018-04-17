package christian.eilers.flibber.Home;


import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.Fragment;
import android.support.v7.widget.SwitchCompat;
import android.text.InputType;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.Toast;

import com.afollestad.materialdialogs.DialogAction;
import com.afollestad.materialdialogs.MaterialDialog;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QuerySnapshot;
import com.google.firebase.functions.FirebaseFunctions;

import java.util.HashMap;
import java.util.Map;

import christian.eilers.flibber.Models.Group;
import christian.eilers.flibber.Profil.ProfilActivity;
import christian.eilers.flibber.R;
import christian.eilers.flibber.Utils.LocalStorage;
import static christian.eilers.flibber.Utils.Strings.*;

public class SettingsFragment extends Fragment implements View.OnClickListener, CompoundButton.OnCheckedChangeListener{

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        mainView = inflater.inflate(R.layout.fragment_settings, container, false);
        db = FirebaseFirestore.getInstance();
        groupID = LocalStorage.getGroupID(getContext());
        initializeViews();
        return mainView;
    }

    // Initialize views from layout file
    private void initializeViews() {
        btn_invite = mainView.findViewById(R.id.btn_invite);
        btn_profil = mainView.findViewById(R.id.btn_profil);
        btn_leave = mainView.findViewById(R.id.btn_leave);
        btn_image = mainView.findViewById(R.id.btn_image);
        switch_notes = mainView.findViewById(R.id.switch_notes);
        switch_shopping = mainView.findViewById(R.id.switch_shopping);
        switch_tasks = mainView.findViewById(R.id.switch_tasks);
        switch_finances = mainView.findViewById(R.id.switch_finances);
        btn_profil.setOnClickListener(this);
        btn_invite.setOnClickListener(this);
        btn_leave.setOnClickListener(this);
        btn_image.setOnClickListener(this);

        // Switch-States from SharedPreferences
        sharedPreferences = getContext().getSharedPreferences(groupID, Context.MODE_PRIVATE);
        switch_notes.setChecked(sharedPreferences.getBoolean(NOTES, true));
        switch_shopping.setChecked(sharedPreferences.getBoolean(SHOPPING, true));
        switch_tasks.setChecked(sharedPreferences.getBoolean(TASKS, true));
        switch_finances.setChecked(sharedPreferences.getBoolean(FINANCES, false));

        switch_notes.setOnCheckedChangeListener(this);
        switch_shopping.setOnCheckedChangeListener(this);
        switch_tasks.setOnCheckedChangeListener(this);
        switch_finances.setOnCheckedChangeListener(this);
    }

    // Send an Invitation to the user matching the given E-Mail Adress to join the WG
    private void invite(final String email) {
        // Check if user with the given email exists
        db.collection(USERS).whereEqualTo(EMAIL, email).get()
                .addOnSuccessListener(new OnSuccessListener<QuerySnapshot>() {
                    @Override
                    public void onSuccess(QuerySnapshot documentSnapshots) {
                        // Check if user with the given email exists
                        if(documentSnapshots.isEmpty()) {
                            Toast.makeText(getContext(), "User existiert nicht", Toast.LENGTH_SHORT).show();
                            return;
                        }
                        // Get the Document with the matching email (IS ALWAYS A SINGLE DOCUMENT!)
                        for(final DocumentSnapshot invitedUserSnapshot : documentSnapshots) {
                            checkIfMember(invitedUserSnapshot);
                        }
                    }
                });
    }

    // Check if user already is member of the WG
    private void checkIfMember(final DocumentSnapshot invitedUserSnapshot) {
        invitedUserSnapshot.getReference().collection(GROUPS).document(groupID).get()
                .addOnSuccessListener(new OnSuccessListener<DocumentSnapshot>() {
                    @Override
                    public void onSuccess(DocumentSnapshot documentSnapshot) {
                        if(documentSnapshot.exists()) {
                            Toast.makeText(getContext(), "User ist bereits Mitglied", Toast.LENGTH_SHORT).show();
                        } else {
                            checkIfInvited(invitedUserSnapshot);
                        }
                    }
                });
    }

    // Check if user already has an invitation
    private void checkIfInvited(final DocumentSnapshot invitedUserSnapshot) {
        invitedUserSnapshot.getReference().collection(INVITATIONS).document(groupID).get()
                .addOnSuccessListener(new OnSuccessListener<DocumentSnapshot>() {
                    @Override
                    public void onSuccess(DocumentSnapshot documentSnapshot) {
                        if(documentSnapshot.exists()) {
                            Toast.makeText(getContext(), "User bereits eingeladen", Toast.LENGTH_SHORT).show();
                        } else {
                            sendInvitation(invitedUserSnapshot);
                        }
                    }
                });
    }

    // Create Invitation-Documents in the Users-Collection and WG-Collection
    private void sendInvitation(final DocumentSnapshot invitedUserSnapshot) {
        db.collection(GROUPS).document(groupID).get()
                .addOnSuccessListener(new OnSuccessListener<DocumentSnapshot>() {
                    @Override
                    public void onSuccess(DocumentSnapshot currentWGSnapshot) {
                        // Create WG-Object of current WG
                        final Group currentWG = currentWGSnapshot.toObject(Group.class);
                        // Create Map of invited User
                        Map<String,Object> map_user = new HashMap<>();
                        map_user.put(EMAIL , invitedUserSnapshot.getString(EMAIL));
                        map_user.put(NAME , invitedUserSnapshot.getString(NAME));
                        map_user.put(PICPATH , invitedUserSnapshot.getString(PICPATH));

                        // Add invited User to Invitation-Collection of WG
                        currentWGSnapshot.getReference().collection(INVITATIONS).document(invitedUserSnapshot.getId()).set(map_user);
                        // Add WG to Invitation-Collection of invited User
                        invitedUserSnapshot.getReference().collection(INVITATIONS).document(groupID).set(currentWG);
                        Toast.makeText(getContext(), "Erfolgreich eingeladen!", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    public void invitationDialog() {
        MaterialDialog.Builder  builder = new MaterialDialog.Builder(getContext())
                .title("Einladen")
                .inputType(InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS)
                .input("E-Mail", null, new MaterialDialog.InputCallback() {
                    @Override
                    public void onInput(@NonNull MaterialDialog dialog, CharSequence input) {

                    }
                })
                .positiveText("Einladen")
                .negativeText("Abbrechen")
                .onPositive(new MaterialDialog.SingleButtonCallback() {
                    @Override
                    public void onClick(@NonNull MaterialDialog dialog, @NonNull DialogAction which) {
                        final String email = dialog.getInputEditText().getText().toString().trim();
                        if(TextUtils.isEmpty(email)) {
                            Toast.makeText(getContext(), "Keine E-Mail Adresse eingegeben", Toast.LENGTH_SHORT).show();
                            return;
                        }
                        dialog.getInputEditText().setText("");
                        invite(email);
                    }
                });

        inviteDialog = builder.build();
        inviteDialog.show();
    }

    // Check which Button has been clicked
    @Override
    public void onClick(View v) {
        int id = v.getId();
        if (id == R.id.btn_invite) {
            invitationDialog();
        }
        else if (id == R.id.btn_profil) {
            // Lösche WG Key und wechsel zur WG&Profil Activity
            LocalStorage.setGroupID(getContext(), null);
            Intent profilIntent = new Intent(getContext(), ProfilActivity.class);
            profilIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
            startActivity(profilIntent);
            getActivity().finish();
        }
        else if (id == R.id.btn_leave) {
            Toast.makeText(getContext(), "Noch nicht möglich...", Toast.LENGTH_SHORT).show();
        }
        else if (id == R.id.btn_image) {
            DialogFragment dialog_image = new GroupImageDialog();
            dialog_image.show(getChildFragmentManager(), "group_image");
        }
    }

    // Save new Notification Settings in the SharedPreferences
    @Override
    public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
        switch (buttonView.getId()) {
            case R.id.switch_notes :
                sharedPreferences.edit().putBoolean(NOTES, isChecked).apply();
                break;
            case R.id.switch_shopping :
                sharedPreferences.edit().putBoolean(SHOPPING, isChecked).apply();
                break;
            case R.id.switch_tasks :
                sharedPreferences.edit().putBoolean(TASKS, isChecked).apply();
                break;
            case R.id.switch_finances :
                sharedPreferences.edit().putBoolean(FINANCES, isChecked).apply();
                break;
        }
    }

    // Class Variables
    private View mainView;
    private Button btn_invite, btn_profil, btn_leave, btn_image;
    private SwitchCompat switch_notes, switch_shopping, switch_tasks, switch_finances;
    private MaterialDialog inviteDialog;

    private FirebaseFirestore db;
    private SharedPreferences sharedPreferences;
    private String groupID;
}
