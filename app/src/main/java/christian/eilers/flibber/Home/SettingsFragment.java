package christian.eilers.flibber.Home;


import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v7.app.AlertDialog;
import android.text.TextUtils;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QuerySnapshot;

import java.util.HashMap;
import java.util.Map;

import christian.eilers.flibber.Models.Wg;
import christian.eilers.flibber.ProfilAndWgs.ProfilActivity;
import christian.eilers.flibber.R;
import christian.eilers.flibber.Utils.LocalStorage;

public class SettingsFragment extends Fragment implements View.OnClickListener{

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        mainView = inflater.inflate(R.layout.fragment_settings, container, false);
        initializeViews();
        db = FirebaseFirestore.getInstance();
        userID = LocalStorage.getUserID(getContext());
        groupID = LocalStorage.getGroupID(getContext());
        return mainView;
    }

    // Initialize views from layout file
    private void initializeViews() {
        btn_invite = mainView.findViewById(R.id.btn_invite);
        btn_profil = mainView.findViewById(R.id.btn_profil);
        btn_profil.setOnClickListener(this);
        btn_invite.setOnClickListener(this);
    }

    // Creates Dialog for inviting new user
    private void createDialog() {
        final View v = getLayoutInflater().inflate(R.layout.dialog_invite, null);
        final Button btn = v.findViewById(R.id.button_ok);
        eT_email = v.findViewById(R.id.editText_email);
        progressBar = v.findViewById(R.id.progressBar);
        dialog = makeAlertDialog(v);
        btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String email = eT_email.getText().toString().trim();
                invite(email);
            }
        });
        eT_email.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView textView, int i, KeyEvent keyEvent) {
                if (i == EditorInfo.IME_ACTION_GO) {
                    String email = eT_email.getText().toString().trim();
                    invite(email);
                    return true;
                }
                return false;
            }
        });
    }

    // Send an Invitation to the user matching the given E-Mail Adress to join the WG
    private void invite(final String email) {
        // Check if E-Mail is not empty
        if(TextUtils.isEmpty(email)){
            Toast.makeText(getContext(), "Keine E-Mail eingegeben", Toast.LENGTH_SHORT).show();
            dialog.dismiss();
            return;
        }
        // Set Loading Animation
        progressBar.setVisibility(View.VISIBLE);
        eT_email.setVisibility(View.GONE);

        // Check if user with the given email exists
        db.collection("users").whereEqualTo("email", email).get()
                .addOnSuccessListener(new OnSuccessListener<QuerySnapshot>() {
                    @Override
                    public void onSuccess(QuerySnapshot documentSnapshots) {
                        // Check if user with the given email exists
                        if(documentSnapshots.isEmpty()) {
                            Toast.makeText(getContext(), "User not found", Toast.LENGTH_SHORT).show();
                            dialog.dismiss();
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
        invitedUserSnapshot.getReference().collection("wgs").document(groupID).get()
                .addOnSuccessListener(new OnSuccessListener<DocumentSnapshot>() {
                    @Override
                    public void onSuccess(DocumentSnapshot documentSnapshot) {
                        if(documentSnapshot.exists()) {
                            Toast.makeText(getContext(), "Already member of WG", Toast.LENGTH_SHORT).show();
                            dialog.dismiss();
                        } else {
                            checkIfInvited(invitedUserSnapshot);
                        }
                    }
                });
    }

    // Check if user already has an invitation
    private void checkIfInvited(final DocumentSnapshot invitedUserSnapshot) {
        invitedUserSnapshot.getReference().collection("invitations").document(groupID).get()
                .addOnSuccessListener(new OnSuccessListener<DocumentSnapshot>() {
                    @Override
                    public void onSuccess(DocumentSnapshot documentSnapshot) {
                        if(documentSnapshot.exists()) {
                            Toast.makeText(getContext(), "Already has an invitation", Toast.LENGTH_SHORT).show();
                            dialog.dismiss();
                        } else {
                            sendInvitation(invitedUserSnapshot);
                        }
                    }
                });
    }

    // Create Invitation-Documents in the Users-Collection and WG-Collection
    private void sendInvitation(final DocumentSnapshot invitedUserSnapshot) {
        db.collection("wgs").document(groupID).get()
                .addOnSuccessListener(new OnSuccessListener<DocumentSnapshot>() {
                    @Override
                    public void onSuccess(DocumentSnapshot currentWGSnapshot) {
                        // Create WG-Object of current WG
                        final Wg currentWG = currentWGSnapshot.toObject(Wg.class);
                        // Create Map of invited User
                        Map<String,Object> map_user = new HashMap<>();
                        map_user.put("email" , invitedUserSnapshot.getString("email"));
                        map_user.put("name" , invitedUserSnapshot.getString("name"));
                        map_user.put("picPath" , invitedUserSnapshot.getString("picPath"));

                        // Add invited User to Invitation-Collection of WG
                        currentWGSnapshot.getReference().collection("invitations").document(invitedUserSnapshot.getId()).set(map_user);
                        // Add WG to Invitation-Collection of invited User
                        invitedUserSnapshot.getReference().collection("invitations").document(groupID).set(currentWG);
                        Toast.makeText(getContext(), "User invited", Toast.LENGTH_SHORT).show();
                        dialog.dismiss();
                    }
                });
    }

    // Make an AlertDialog from a View
    public AlertDialog makeAlertDialog(View v) {
        AlertDialog.Builder mBuilder = new AlertDialog.Builder(getContext());
        mBuilder.setView(v);
        AlertDialog dialog = mBuilder.create();
        dialog.show();
        return dialog;
    }

    // Check which Button has been clicked
    @Override
    public void onClick(View v) {
        int id = v.getId();
        if (id == R.id.btn_invite) {
            createDialog();
        }
        else if (id == R.id.btn_profil) {
            // Lösche WG Key und wechsel zur WG&Profil Activity
            LocalStorage.setGroupID(getContext(), null);
            Intent profilIntent = new Intent(getContext(), ProfilActivity.class);
            startActivity(profilIntent);
            getActivity().overridePendingTransition(R.anim.fade_in, R.anim.fade_out);
            getActivity().finish();
        }
    }

    // Class Variables
    private View mainView;
    private Button btn_invite, btn_profil;
    private FirebaseFirestore db;
    private String userID, groupID;
    // Dialog Variables
    private ProgressBar progressBar;
    private AlertDialog dialog;
    private EditText eT_email;
}
