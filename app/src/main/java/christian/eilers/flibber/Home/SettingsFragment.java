package christian.eilers.flibber.Home;


import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
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
import android.widget.ProgressBar;
import android.widget.Toast;

import com.afollestad.materialdialogs.DialogAction;
import com.afollestad.materialdialogs.MaterialDialog;
import com.bumptech.glide.Glide;
import com.bumptech.glide.signature.ObjectKey;
import com.crashlytics.android.Crashlytics;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.EventListener;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.firestore.QuerySnapshot;
import com.google.firebase.functions.FirebaseFunctions;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;
import com.theartofdev.edmodo.cropper.CropImage;
import com.theartofdev.edmodo.cropper.CropImageView;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import christian.eilers.flibber.MainActivity;
import christian.eilers.flibber.Models.Group;
import christian.eilers.flibber.Profil.ProfilActivity;
import christian.eilers.flibber.ProfileActivity;
import christian.eilers.flibber.R;
import christian.eilers.flibber.Utils.GlideApp;
import christian.eilers.flibber.Utils.LocalStorage;
import de.hdodenhof.circleimageview.CircleImageView;

import static christian.eilers.flibber.Utils.Strings.*;

public class SettingsFragment extends Fragment implements View.OnClickListener, CompoundButton.OnCheckedChangeListener{

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        mainView = inflater.inflate(R.layout.fragment_settings, container, false);
        db = FirebaseFirestore.getInstance();
        groupID = LocalStorage.getGroupID(getContext());
        if (groupID == null) {
            Intent main = new Intent(getContext(), MainActivity.class);
            main.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
            startActivity(main);
            getActivity().overridePendingTransition(R.anim.fade_in, R.anim.fade_out);
            getActivity().finish();
        } else {
            groupPicPath = LocalStorage.getGroupPicPath(getContext()); // CAN BE NULL !!!
            storage_groups = FirebaseStorage.getInstance().getReference().child(GROUPS);
            initializeViews();
            // Update Profile Picture on change
            db.collection(GROUPS).document(groupID).addSnapshotListener(new EventListener<DocumentSnapshot>() {
                @Override
                public void onEvent(DocumentSnapshot documentSnapshot, FirebaseFirestoreException e) {
                    String picPath = documentSnapshot.getString(PICPATH);
                    if (groupPicPath == null) {
                        if (picPath == null) return;
                        groupPicPath = picPath;
                        LocalStorage.setGroupPicPath(getActivity(), picPath);
                        GlideApp.with(getActivity())
                                .load(storage_groups.child(groupPicPath))
                                .dontAnimate()
                                .placeholder(R.drawable.placeholder_group)
                                .into(img_group);
                        return;
                    }
                    else {
                        if (picPath == null) {
                            groupPicPath = null;
                            LocalStorage.setGroupPicPath(getActivity(), null);
                            Glide.with(getActivity()).clear(img_group);
                            return;
                        }
                        if (groupPicPath.equals(picPath)) return;
                        groupPicPath = picPath;
                        LocalStorage.setGroupPicPath(getActivity(), picPath);
                        GlideApp.with(getActivity())
                                .load(storage_groups.child(groupPicPath))
                                .dontAnimate()
                                .placeholder(R.drawable.placeholder_group)
                                .into(img_group);
                        return;
                    }

                }
            });
        }

        return mainView;
    }

    // Initialize views from layout file
    private void initializeViews() {
        btn_invite = mainView.findViewById(R.id.btn_invite);
        btn_profil = mainView.findViewById(R.id.btn_profil);
        btn_leave = mainView.findViewById(R.id.btn_leave);
        img_group = mainView.findViewById(R.id.group_image);
        switch_notes = mainView.findViewById(R.id.switch_notes);
        switch_shopping = mainView.findViewById(R.id.switch_shopping);
        switch_tasks = mainView.findViewById(R.id.switch_tasks);
        switch_finances = mainView.findViewById(R.id.switch_finances);
        progressBar = mainView.findViewById(R.id.progressBar);
        btn_profil.setOnClickListener(this);
        btn_invite.setOnClickListener(this);
        btn_leave.setOnClickListener(this);
        img_group.setOnClickListener(this);

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

        if (groupPicPath != null)
            GlideApp.with(getContext())
                    .load(storage_groups.child(groupPicPath))
                    .dontAnimate()
                    .placeholder(R.drawable.placeholder_group)
                    .into(img_group);
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
                        map_user.put(PICPATH, invitedUserSnapshot.getString(PICPATH));

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

    // Update all References to the Group-Picture
    public void saveImageToDB() {
        final Map<String, Object> groupData = new HashMap<>();
        groupData.put(PICPATH, groupPicPath);

        // Update Reference in the groups-collection
        db.collection(GROUPS).document(groupID).update(groupData)
                .addOnSuccessListener(new OnSuccessListener<Void>() {
                    @Override
                    public void onSuccess(Void aVoid) {
                        // Get all users from the group
                        db.collection(GROUPS).document(groupID).collection(USERS).get()
                                .addOnSuccessListener(new OnSuccessListener<QuerySnapshot>() {
                                    @Override
                                    public void onSuccess(QuerySnapshot documentSnapshots) {
                                        // Update Group Picture-References for each user
                                        for (DocumentSnapshot doc : documentSnapshots) {
                                            db.collection(USERS).document(doc.getId()).collection(GROUPS).document(groupID).update(groupData);
                                        }
                                        progressBar.setVisibility(View.GONE);
                                    }
                                });
                    }
                });
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
            LocalStorage.setGroupPicPath(getContext(), null);
            LocalStorage.setGroupName(getContext(), null);
            Intent profileIntent = new Intent(getContext(), ProfileActivity.class);
            profileIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
            startActivity(profileIntent);
            getActivity().finish();
        }
        else if (id == R.id.btn_leave) {
            Toast.makeText(getContext(), "Noch nicht möglich...", Toast.LENGTH_SHORT).show();
        }
        else if (id == R.id.group_image) {
            Intent galleryIntent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
            startActivityForResult(galleryIntent, REQUEST_CODE_GALLERY);
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        // Zuschneiden des Bildes auf 1:1 Ratio
        if (resultCode != Activity.RESULT_CANCELED) {
            if (requestCode == REQUEST_CODE_GALLERY) {
                CropImage.activity(data.getData())
                        .setGuidelines(CropImageView.Guidelines.ON)
                        .setAspectRatio(1, 1)
                        .start(getContext(), this);
            } else super.onActivityResult(requestCode, resultCode, data);
        }

        // Zurückliefern des zugeschnitten Bildes
        if (requestCode == CropImage.CROP_IMAGE_ACTIVITY_REQUEST_CODE) {
            CropImage.ActivityResult result = CropImage.getActivityResult(data);
            if (resultCode == Activity.RESULT_OK) {
                progressBar.setVisibility(View.VISIBLE);
                final Uri uri = result.getUri();
                final String path_new = UUID.randomUUID().toString().replaceAll("-","");
                storage_groups.child(path_new).putFile(uri).addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() {
                    @Override
                    public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {
                        GlideApp.with(getContext())
                                .load(storage_groups.child(path_new))
                                .dontAnimate()
                                .placeholder(R.drawable.placeholder_group)
                                .into(img_group);
                        if (groupPicPath != null) storage_groups.child(groupPicPath).delete();
                        LocalStorage.setGroupPicPath(getContext(), path_new);
                        groupPicPath = path_new;

                        saveImageToDB();
                    }
                }).addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        progressBar.setVisibility(View.GONE);
                        Toast.makeText(getContext(), "Fehler beim Upload!", Toast.LENGTH_SHORT).show();
                    }
                });
            }
            else if (resultCode == CropImage.CROP_IMAGE_ACTIVITY_RESULT_ERROR_CODE) {
                Crashlytics.logException(result.getError());
            }
        }

        super.onActivityResult(requestCode, resultCode, data);

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
    private Button btn_invite, btn_profil, btn_leave;
    private CircleImageView img_group;
    private ProgressBar progressBar;
    private SwitchCompat switch_notes, switch_shopping, switch_tasks, switch_finances;
    private MaterialDialog inviteDialog;

    private FirebaseFirestore db;
    private StorageReference storage_groups;
    private SharedPreferences sharedPreferences;
    private String groupID, groupPicPath;

    private final int REQUEST_CODE_GALLERY = 0;
}
