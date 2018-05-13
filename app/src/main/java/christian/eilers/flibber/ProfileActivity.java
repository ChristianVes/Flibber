package christian.eilers.flibber;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.text.InputType;
import android.text.TextUtils;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.afollestad.materialdialogs.DialogAction;
import com.afollestad.materialdialogs.MaterialDialog;
import com.crashlytics.android.Crashlytics;
import com.firebase.ui.firestore.FirestoreRecyclerAdapter;
import com.firebase.ui.firestore.FirestoreRecyclerOptions;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;
import com.google.firebase.firestore.Transaction;
import com.google.firebase.firestore.WriteBatch;
import com.google.firebase.iid.FirebaseInstanceId;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;
import com.theartofdev.edmodo.cropper.CropImage;
import com.theartofdev.edmodo.cropper.CropImageView;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import christian.eilers.flibber.FirestoreAdapter.GroupAdapter;
import christian.eilers.flibber.FirestoreAdapter.InvitationAdapter;
import christian.eilers.flibber.Home.HomeActivity;
import christian.eilers.flibber.Models.Group;
import christian.eilers.flibber.Models.User;
import christian.eilers.flibber.Utils.GlideApp;
import christian.eilers.flibber.Utils.LocalStorage;
import de.hdodenhof.circleimageview.CircleImageView;

import static christian.eilers.flibber.Utils.Strings.DEVICETOKEN;
import static christian.eilers.flibber.Utils.Strings.EMAIL;
import static christian.eilers.flibber.Utils.Strings.GROUPS;
import static christian.eilers.flibber.Utils.Strings.INVITATIONS;
import static christian.eilers.flibber.Utils.Strings.NAME;
import static christian.eilers.flibber.Utils.Strings.PICPATH;
import static christian.eilers.flibber.Utils.Strings.PROFILE;
import static christian.eilers.flibber.Utils.Strings.TIMESTAMP;
import static christian.eilers.flibber.Utils.Strings.USERS;

public class ProfileActivity extends AppCompatActivity implements View.OnClickListener{

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile);
        initializeVariables();
        if (hasNulls()) {
            Intent main = new Intent(this, MainActivity.class);
            main.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
            startActivity(main);
            overridePendingTransition(R.anim.fade_in, R.anim.fade_out);
            finish();
            return;
        }
        initializeViews();
        progressBar.setVisibility(View.VISIBLE);
        loadUserInformation();
        loadGroups();
        loadInvitations();
    }

    // Initialize views
    private void initializeViews() {
        toolbar = findViewById(R.id.toolbar);
        recView_groups = findViewById(R.id.recView_groups);
        recView_invitations = findViewById(R.id.recView_invitations);
        tv_name = findViewById(R.id.name);
        tv_invitations = findViewById(R.id.tv_invitations);
        placeholder = findViewById(R.id.placeholder);
        img_profile = findViewById(R.id.profile_image);
        progressBar = findViewById(R.id.progressBar);
        progressBar_img = findViewById(R.id.progressBar_img);
        btn_add = findViewById(R.id.btn_add);

        setSupportActionBar(toolbar); // Toolbar als Actionbar setzen
        btn_add.setOnClickListener(this);
        img_profile.setOnClickListener(this);
    }

    // Initialize variables
    private void initializeVariables() {
        userID = LocalStorage.getUserID(this);
        userName = LocalStorage.getUsername(this);
        picPath = LocalStorage.getPicPath(this);
        db = FirebaseFirestore.getInstance();
        storage = FirebaseStorage.getInstance().getReference();
    }

    // check for null pointers
    private boolean hasNulls() {
        if (userID == null || userName == null) return true;
        else return false;
    }

    // Display the user information (name & picture)
    private void loadUserInformation() {
        tv_name.setText(userName);
        if (picPath != null)
            GlideApp.with(ProfileActivity.this)
                    .load(storage.child(PROFILE).child(picPath))
                    .dontAnimate()
                    .placeholder(R.drawable.profile_placeholder)
                    .into(img_profile);
    }

    // Load the user's groups into @recView_groups
    private void loadGroups() {
        final Query groupsQuery = db.collection(USERS).document(userID).collection(GROUPS).orderBy(TIMESTAMP);

        final FirestoreRecyclerOptions<Group> options = new FirestoreRecyclerOptions.Builder<Group>()
                .setQuery(groupsQuery, Group.class)
                .build();

        adapter_group = new GroupAdapter(options, ProfileActivity.this) {
            // Update placeholder and progressbar on data change
            @Override
            public void onDataChanged() {
                super.onDataChanged();
                if (getItemCount() == 0) placeholder.setVisibility(View.VISIBLE);
                else placeholder.setVisibility(View.GONE);
                progressBar.setVisibility(View.GONE);
            }
        };

        recView_groups.setLayoutManager(new LinearLayoutManager(this));
        recView_groups.setAdapter(adapter_group);
    }

    // Load the user's group-invitations into @recView_invitations
    private void loadInvitations() {
        final Query invitesQuery = db.collection(USERS).document(userID).collection(INVITATIONS).orderBy(TIMESTAMP);

        final FirestoreRecyclerOptions<Group> options = new FirestoreRecyclerOptions.Builder<Group>()
                .setQuery(invitesQuery, Group.class)
                .build();

        adapter_invitations = new InvitationAdapter(options, ProfileActivity.this) {
            // Update placeholder and recycler view visibility
            @Override
            public void onDataChanged() {
                super.onDataChanged();
                if (getItemCount() > 0) {
                    recView_invitations.setVisibility(View.VISIBLE);
                    tv_invitations.setVisibility(View.VISIBLE);
                }
                else {
                    recView_invitations.setVisibility(View.GONE);
                    tv_invitations.setVisibility(View.GONE);
                }
            }
        };

        recView_invitations.setLayoutManager(new LinearLayoutManager(this));
        recView_invitations.setAdapter(adapter_invitations);
    }

    // Save the new profile picture in the database
    private void saveProfileImage() {
        progressBar_img.setVisibility(View.VISIBLE);
        // Generate a random String for the picture path
        final String newPicPath = UUID.randomUUID().toString().replaceAll("-","");
        storage.child(PROFILE).child(newPicPath).putFile(imageUri)
                .addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() {
                    @Override
                    public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {
                        // Delete old Picture from Storage
                        if (picPath != null)
                            storage.child(PROFILE).child(picPath).delete();
                        picPath = newPicPath;
                        updateProfileImageRefs();
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        imageUri = null;
                        progressBar_img.setVisibility(View.GONE);
                        Toast.makeText(ProfileActivity.this, "Fehler beim Upload!", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    // Update all references to the profile picture of the current user in the database
    private void updateProfileImageRefs() {
        final Map<String, Object> userData = new HashMap<>();
        userData.put(PICPATH, picPath);
        // Update Reference in the users-collection
        db.collection(USERS).document(userID).update(userData)
                .addOnSuccessListener(new OnSuccessListener<Void>() {
                    @Override
                    public void onSuccess(Void aVoid) {
                        // Get all groups the user is part of
                        db.collection(USERS).document(userID).collection(GROUPS).get()
                                .addOnSuccessListener(new OnSuccessListener<QuerySnapshot>() {
                                    @Override
                                    public void onSuccess(QuerySnapshot documentSnapshots) {
                                        // Update Picture-References in each group
                                        for (DocumentSnapshot doc : documentSnapshots) {
                                            db.collection(GROUPS).document(doc.getId()).collection(USERS).document(userID).update(userData);
                                        }
                                        LocalStorage.setPicPath(ProfileActivity.this, picPath);
                                        loadUserInformation();
                                        progressBar_img.setVisibility(View.GONE);
                                    }
                                });
                    }
                });
    }

    // Show a dialog to create a new group
    public void createGroupDialog() {
        new MaterialDialog.Builder(this)
                .title("Neue Gruppe erstellen")
                .inputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_CAP_WORDS)
                .input("Gruppenname", null, new MaterialDialog.InputCallback() {
                    @Override
                    public void onInput(@NonNull MaterialDialog dialog, CharSequence input) {
                    }
                })
                .positiveText("Erstellen")
                .negativeText("Abbrechen")
                .onPositive(new MaterialDialog.SingleButtonCallback() {
                    @Override
                    public void onClick(@NonNull MaterialDialog dialog, @NonNull DialogAction which) {
                        // Read out input for the group name
                        String groupname = dialog.getInputEditText().getText().toString().trim();
                        if(TextUtils.isEmpty(groupname)) {
                            Toast.makeText(ProfileActivity.this, "Keinen Gruppennamen eingegeben",
                                    Toast.LENGTH_SHORT).show();
                            return;
                        }
                        dialog.getInputEditText().setText("");
                        createGroup(groupname);
                    }
                }).show();
    }

    // Create a new group, add this user to the group and open the @HomeActivity
    private void createGroup(final String groupname) {
        progressBar.setVisibility(View.VISIBLE);

        final DocumentReference ref_group = db.collection(GROUPS).document();
        final DocumentReference ref_user = db.collection(USERS).document(userID);
        final Group group = new Group(groupname, ref_group.getId(), null);

        // save the group in the group collection
        ref_group.set(group).addOnSuccessListener(new OnSuccessListener<Void>() {
            @Override
            public void onSuccess(Void aVoid) {

                // get the user information
                ref_user.get().addOnSuccessListener(new OnSuccessListener<DocumentSnapshot>() {
                    @Override
                    public void onSuccess(DocumentSnapshot snap_user) {

                        // read out user information
                        String username = snap_user.getString(NAME);
                        String email = snap_user.getString(EMAIL);
                        String picPath = snap_user.getString(PICPATH);
                        String deviceToken = snap_user.getString(DEVICETOKEN);
                        final User user = new User(username, email, userID, picPath, deviceToken);

                        // Save user in this group's user collection
                        ref_group.collection(USERS).document(userID).set(user).addOnSuccessListener(new OnSuccessListener<Void>() {
                            @Override
                            public void onSuccess(Void aVoid) {

                                // save group in the user's group collection
                                ref_user.collection(GROUPS).document(group.getKey()).set(group).addOnSuccessListener(new OnSuccessListener<Void>() {
                                    @Override
                                    public void onSuccess(Void aVoid) {
                                        progressBar.setVisibility(View.GONE);
                                        // save group data in the local storage
                                        LocalStorage.setGroupID(ProfileActivity.this, group.getKey());
                                        LocalStorage.setGroupName(ProfileActivity.this, group.getName());
                                        // open @HomeActivity
                                        Intent homeIntent = new Intent(ProfileActivity.this, HomeActivity.class);
                                        startActivity(homeIntent);
                                        finish();
                                    }
                                });
                            }
                        });
                    }
                });
            }
        });
    }

    // Log the current user out && Delete DeviceTokens in database
    private void logout() {
        progressBar.setVisibility(View.VISIBLE);
        // Delete the DeviceToken from the current User
        final Map<String,Object> map_devicetoken = new HashMap<>();
        map_devicetoken.put(DEVICETOKEN, FieldValue.delete());
        // Get a new write batch
        final WriteBatch batch = db.batch();
        // Delete DeviceToken from User
        DocumentReference doc_user = db.collection(USERS).document(userID);
        batch.update(doc_user, map_devicetoken);
        // Delete DeviceToken from user in each of his groups
        db.collection(USERS).document(userID).collection(GROUPS).get().addOnSuccessListener(new OnSuccessListener<QuerySnapshot>() {
            @Override
            public void onSuccess(QuerySnapshot queryDocumentSnapshots) {
                for (QueryDocumentSnapshot document : queryDocumentSnapshots) {
                    DocumentReference doc_user = db.collection(GROUPS).document(document.getId()).collection(USERS).document(userID);
                    batch.update(doc_user, map_devicetoken);
                }
                // Commit DeviceToken-Deletions and log out the user
                batch.commit().addOnSuccessListener(new OnSuccessListener<Void>() {
                    @Override
                    public void onSuccess(Void aVoid) {
                        progressBar.setVisibility(View.GONE);
                        FirebaseAuth.getInstance().signOut(); // sign out the current user
                        // delete local data
                        LocalStorage.setData(ProfileActivity.this, null, null, null, null);
                        // switch to LoginActivity
                        Intent login = new Intent(ProfileActivity.this, LoginActivity.class);
                        login.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                        startActivity(login);
                        overridePendingTransition(R.anim.fade_in, R.anim.fade_out);
                        finish();
                    }
                });
            }
        });
    }

    @Override
    public void onClick(View v) {
        int id = v.getId();
        // To Gallery
        if (id == R.id.profile_image) {
            Intent galleryIntent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
            startActivityForResult(galleryIntent, REQUEST_CODE_GALLERY);
        }
        if (id == R.id.btn_add) {
            createGroupDialog();
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        // cut the profile picture to 1:1 ratio
        if (resultCode != Activity.RESULT_CANCELED) {
            if (requestCode == REQUEST_CODE_GALLERY) {
                CropImage.activity(data.getData())
                        .setGuidelines(CropImageView.Guidelines.ON)
                        .setAspectRatio(1, 1)
                        .start(this);
            } else super.onActivityResult(requestCode, resultCode, data);
        }

        // then save the cut out profile picture
        if (requestCode == CropImage.CROP_IMAGE_ACTIVITY_REQUEST_CODE) {
            CropImage.ActivityResult result = CropImage.getActivityResult(data);
            if (resultCode == Activity.RESULT_OK) {
                imageUri = result.getUri();
                saveProfileImage();
            }
            else if (resultCode == CropImage.CROP_IMAGE_ACTIVITY_RESULT_ERROR_CODE) {
                Crashlytics.logException(result.getError());
            }
        }
        super.onActivityResult(requestCode, resultCode, data);

    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_profile, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_logout:
                logout();
                return true;
            case R.id.action_name:
                Toast.makeText(this, "Noch nicht möglich...", Toast.LENGTH_SHORT).show();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    @Override
    public void onStart() {
        super.onStart();
        if (adapter_group != null) adapter_group.startListening();
        if (adapter_invitations != null) adapter_invitations.startListening();
    }

    @Override
    public void onStop() {
        super.onStop();
        if (adapter_group != null) adapter_group.stopListening();
        if (adapter_invitations != null) adapter_invitations.stopListening();
    }

    private String userID, userName, picPath;
    private Uri imageUri;
    private FirebaseFirestore db;
    private StorageReference storage;
    private FirestoreRecyclerAdapter adapter_group, adapter_invitations;

    private Toolbar toolbar;
    private RecyclerView recView_groups, recView_invitations;
    private TextView tv_name, tv_invitations, placeholder;
    private CircleImageView img_profile;
    private ProgressBar progressBar, progressBar_img;
    private ImageButton btn_add;

    private final int REQUEST_CODE_GALLERY = 0;
}
