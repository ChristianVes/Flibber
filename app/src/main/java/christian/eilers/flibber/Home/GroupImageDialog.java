package christian.eilers.flibber.Home;

import android.app.Activity;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.support.annotation.NonNull;
import android.support.v4.app.DialogFragment;
import android.support.v7.app.AlertDialog;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.Toast;

import com.bumptech.glide.signature.ObjectKey;
import com.crashlytics.android.Crashlytics;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;
import com.theartofdev.edmodo.cropper.CropImage;
import com.theartofdev.edmodo.cropper.CropImageView;

import christian.eilers.flibber.R;
import christian.eilers.flibber.Utils.GlideApp;
import christian.eilers.flibber.Utils.LocalStorage;

import static christian.eilers.flibber.Utils.Strings.*;

public class GroupImageDialog extends DialogFragment {

    private final int REQUEST_CODE_GALLERY = 0;
    private ImageView img_group;
    private ProgressBar progressBar;
    private StorageReference storage;
    private Uri imageUri;
    private String groupID, picPath;

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        // Get the layout inflater
        LayoutInflater inflater = getActivity().getLayoutInflater();

        // Inflate and set the layout for the dialog
        // Pass null as the parent view because its going in the dialog layout
        View v_dialog = inflater.inflate(R.layout.dialog_group_image, null);
        builder.setView(v_dialog)
                // Add action buttons
                .setPositiveButton("Ändern", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int id) {
                        Intent galleryIntent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
                        startActivityForResult(galleryIntent, REQUEST_CODE_GALLERY);
                    }
                })
                .setNegativeButton("Löschen", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        if(picPath == null) {
                            Toast.makeText(getContext(), "Kein Bild vorhanden", Toast.LENGTH_SHORT).show();
                            return;
                        }
                        // TODO DELETE
                    }
                });

        img_group = v_dialog.findViewById(R.id.groupImage);
        progressBar = v_dialog.findViewById(R.id.progressBar);
        storage = FirebaseStorage.getInstance().getReference();
        groupID = LocalStorage.getGroupID(getContext());

        GlideApp.with(getContext())
                .load(storage.child(GROUPS).child(groupID))
                .dontAnimate()
                .placeholder(R.drawable.placeholder_group)
                .into(img_group);

        return builder.create();
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
                imageUri = result.getUri();
                saveImage();
            }
            else if (resultCode == CropImage.CROP_IMAGE_ACTIVITY_RESULT_ERROR_CODE) {
                Crashlytics.logException(result.getError());
            }
        }

        super.onActivityResult(requestCode, resultCode, data);

    }

    // Save the Image in the FirebaseStorage and its Reference in the Database
    private void saveImage() {
        progressBar.setVisibility(View.VISIBLE);
        //final String newPicPath = imageUri.getLastPathSegment();
        storage.child(GROUPS).child(groupID).putFile(imageUri)
                .addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() {
                    @Override
                    public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {
                        progressBar.setVisibility(View.GONE);
                        GlideApp.with(getContext())
                                .load(storage.child(GROUPS).child(groupID))
                                .signature(new ObjectKey(System.currentTimeMillis()))
                                .dontAnimate()
                                .placeholder(R.drawable.placeholder_group)
                                .into(img_group);
                        // Delete old Picture from Storage
                        /*if (picPath != null)
                            storage.child(picPath).delete();
                        picPath = newPicPath;*/
                        //updateRefs();
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        imageUri = null;
                        progressBar.setVisibility(View.GONE);
                        Toast.makeText(getContext(), "Fehler beim Upload!", Toast.LENGTH_SHORT).show();
                    }
                });
    }
}
