package christian.eilers.flibber.FirestoreAdapter;

import android.app.Dialog;
import android.content.Context;
import android.graphics.Typeface;
import android.support.annotation.NonNull;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.TextView;

import com.bumptech.glide.Glide;
import com.firebase.ui.firestore.FirestoreRecyclerAdapter;
import com.firebase.ui.firestore.FirestoreRecyclerOptions;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import org.fabiomsr.moneytextview.MoneyTextView;

import christian.eilers.flibber.Home.Finance.QuickTransactionDialog;
import christian.eilers.flibber.Models.User;
import christian.eilers.flibber.R;
import christian.eilers.flibber.Utils.GlideApp;
import christian.eilers.flibber.Utils.LocalStorage;
import de.hdodenhof.circleimageview.CircleImageView;

import static christian.eilers.flibber.Utils.Strings.PROFILE;

public class BalanceAdapter extends FirestoreRecyclerAdapter<User, BalanceAdapter.UserHolder> {

    private String userID;
    private StorageReference storage;
    private ViewGroup parent;

    public BalanceAdapter(@NonNull FirestoreRecyclerOptions<User> options) {
        super(options);
        storage = FirebaseStorage.getInstance().getReference();
    }

    @Override
    protected void onBindViewHolder(@NonNull UserHolder holder, int position, @NonNull final User model) {
        final Context c = holder.itemView.getContext();
        // USERNAME & MONEY
        holder.tv_username.setText(model.getName());
        if (model.getUserID().equals(userID)) holder.tv_username.setTypeface(null, Typeface.BOLD);
        else holder.tv_username.setTypeface(null, Typeface.NORMAL);
        holder.tv_money.setAmount(model.getMoney());

        // PROFILE PICTURE
        if(model.getPicPath() != null)
            GlideApp.with(c)
                    .load(storage.child(PROFILE).child(model.getPicPath()))
                    .dontAnimate()
                    .placeholder(R.drawable.profile_placeholder)
                    .into(holder.img_profile);
        else Glide.with(c).clear(holder.img_profile);

        // Open Dialog for "Quick-Transaction" with the clicked-User
        holder.itemView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (userID.equals(model.getUserID())) return; // don't allow transaction with oneself

                final Dialog dialog = new QuickTransactionDialog(c,
                        R.style.TransactionDialog, model);
                // Show the Keyboard
                dialog.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE);
                dialog.show();
            }
        });

        // show bigger picture of profile picture
        holder.img_profile.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                AlertDialog.Builder builder = new AlertDialog.Builder(v.getContext());
                View v_dialog = LayoutInflater.from(parent.getContext()).inflate(R.layout.dialog_profile_image, null);
                ImageView img_profile = v_dialog.findViewById(R.id.profile_image);
                // PROFILE PICTURE
                if(model.getPicPath() != null)
                    GlideApp.with(v.getContext())
                            .load(storage.child(PROFILE).child(model.getPicPath()))
                            .dontAnimate()
                            .placeholder(R.drawable.profile_placeholder)
                            .into(img_profile);
                builder.setView(v_dialog);
                AlertDialog dialog = builder.create();
                dialog.show();
                final float scale = parent.getContext().getResources().getDisplayMetrics().density;
                final int dps = 250;
                int pixels = (int) (dps * scale + 0.5f);
                dialog.getWindow().setLayout(pixels, pixels);
            }
        });
    }

    @NonNull
    @Override
    public UserHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        this.parent = parent;
        this.userID = LocalStorage.getUserID(parent.getContext());
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_finanzen_user, parent, false);
        return new UserHolder(view);
    }

    // Custom Viewholder for users
    public class UserHolder extends RecyclerView.ViewHolder {

        CircleImageView img_profile;
        TextView tv_username;
        MoneyTextView tv_money;

        public UserHolder(View itemView) {
            super(itemView);
            img_profile = itemView.findViewById(R.id.profile_image);
            tv_username = itemView.findViewById(R.id.username);
            tv_money = itemView.findViewById(R.id.money);
        }
    }
}
