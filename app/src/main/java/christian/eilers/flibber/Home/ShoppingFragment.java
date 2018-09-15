package christian.eilers.flibber.Home;


import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v7.widget.DividerItemDecoration;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.TextUtils;
import android.text.format.DateUtils;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.crashlytics.android.Crashlytics;
import com.firebase.ui.firestore.FirestoreRecyclerAdapter;
import com.firebase.ui.firestore.FirestoreRecyclerOptions;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.Transaction;
import com.google.firebase.firestore.WriteBatch;

import java.util.ArrayList;
import java.util.HashMap;

import christian.eilers.flibber.MainActivity;
import christian.eilers.flibber.Models.Article;
import christian.eilers.flibber.Models.StockProduct;
import christian.eilers.flibber.Models.User;
import christian.eilers.flibber.R;
import christian.eilers.flibber.RecyclerAdapter.UserSelectionAdapter;
import christian.eilers.flibber.Utils.LocalStorage;
import static christian.eilers.flibber.Utils.Strings.*;

public class ShoppingFragment extends Fragment implements View.OnClickListener, View.OnFocusChangeListener{

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        mainView = inflater.inflate(R.layout.fragment_shopping, container, false);
        initializeViews();
        initializeVariables();
        if (hasNulls()) {
            Intent main = new Intent(getContext(), MainActivity.class);
            main.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
            startActivity(main);
            getActivity().overridePendingTransition(R.anim.fade_in, R.anim.fade_out);
            getActivity().finish();
            return mainView;
        }
        // clear checkBox-states
        getContext().getSharedPreferences(SHOPPING, Context.MODE_PRIVATE).edit().clear().commit();
        loadData();
        if (getContext().getSharedPreferences(groupID, Context.MODE_PRIVATE).getBoolean(STOCK, false)) {
            loadStock();
            layout_stock.setVisibility(View.VISIBLE);
        } else layout_stock.setVisibility(View.GONE);



        return mainView;
    }

    // Initialize Layout
    private void initializeViews() {
        layout_stock = mainView.findViewById(R.id.layout_stock);
        recView = mainView.findViewById(R.id.shoppingList);
        recView_stock = mainView.findViewById(R.id.recView_stock);
        et_article = mainView.findViewById(R.id.input_shopping);
        btn_save = mainView.findViewById(R.id.btn_save);
        btn_group = mainView.findViewById(R.id.btn_group);
        btn_shopping = mainView.findViewById(R.id.btn_shopping);
        btn_stock = mainView.findViewById(R.id.btn_stock);
        placeholder_stock = mainView.findViewById(R.id.placeholder_stock);
        placeholder = mainView.findViewById(R.id.placeholder);

        et_article.setOnFocusChangeListener(this);
        et_article.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView textView, int i, KeyEvent keyEvent) {
                if (i == EditorInfo.IME_ACTION_GO) saveArticle();
                return true;
            }
        });

        btn_save.setOnClickListener(this);
        btn_group.setOnClickListener(this);
        btn_shopping.setOnClickListener(this);
        btn_stock.setOnClickListener(this);
    }

    // Initialize Variables
    private void initializeVariables() {
        userID = LocalStorage.getUserID(getContext());
        groupID = LocalStorage.getGroupID(getContext());
        users = ((HomeActivity) getActivity()).getUsers();
        db = FirebaseFirestore.getInstance();
    }

    // check for null pointers
    private boolean hasNulls() {
        if (users == null || userID == null || groupID == null) return true;
        else return false;
    }

    // Add article to the user's database
    private void saveArticle() {
        // Get the Article-Name
        final String articleName = et_article.getText().toString().trim();
        if (TextUtils.isEmpty(articleName)) return;
        et_article.setText("");

        // Create Article and a new document for it
        DocumentReference ref_article = ref_shopping.document();
        final Article article = new Article(ref_article.getId(), articleName, userID, !forAll);

        // Save Article private/for all
        if (!forAll) ref_article.set(article);
        else {
            CollectionReference ref_users = db.collection(GROUPS).document(groupID).collection(USERS);
            WriteBatch batch = db.batch();
            users = ((HomeActivity) getActivity()).getUsers();
            // Save article in each users collection
            for (User u : users.values()) {
                DocumentReference ref = ref_users.document(u.getUserID()).collection(SHOPPING).document(article.getKey());
                batch.set(ref, article);
            }
            batch.commit();
        }
    }

    // Delete article from the database
    private void deleteArticle(final Article article) {
        // If private: only delete from current user's collection
        if (article.isPrivate()) ref_shopping.document(article.getKey()).delete();
        // Otherwise: delete for every user in the group
        else {
            CollectionReference ref_users = db.collection(GROUPS).document(groupID).collection(USERS);
            WriteBatch batch = db.batch();
            users = ((HomeActivity) getActivity()).getUsers();
            // Save article in each users collection
            for (User u : users.values()) {
                DocumentReference ref = ref_users.document(u.getUserID()).collection(SHOPPING).document(article.getKey());
                batch.delete(ref);
            }
            batch.commit();
        }
        // look for a StockProduct with same key and add the user to the purchaser list if it exists
        final CollectionReference ref_stock = db.collection(GROUPS).document(groupID).collection(STOCK);
        db.runTransaction(new Transaction.Function<Void>() {
            @Nullable
            @Override
            public Void apply(@NonNull Transaction transaction) throws FirebaseFirestoreException {
                DocumentSnapshot snapshot = transaction.get(ref_stock.document(article.getKey()));
                if (snapshot.exists()) {
                    StockProduct product = snapshot.toObject(StockProduct.class);
                    if (product.getInvolvedIDs().contains(userID)) {
                        product.getPurchaserIDs().add(userID);
                        transaction.set(ref_stock.document(article.getKey()), product);
                    }
                }
                return null;
            }
        });
    }

    // Find all checked Articles and delete them
    private void findCheckedArticles() {
        for (int i = 0; i < adapter.getItemCount(); i++) {
            try {
                CheckBox checkBox = ((ShoppingHolder) recView.findViewHolderForAdapterPosition(i)).checkBox;
                if (checkBox.isChecked()) {
                    Article article = (Article) adapter.getItem(i);
                    deleteArticle(article);
                }
            }
            // Sometimes throws exceptions e.g. if article was added just now
            catch (Exception e) {
                Crashlytics.logException(e);
            }
        }
    }

    // Load the Shopping-List, update checkedItems and set Listeners
    private void loadData() {
        ref_shopping = db.collection(GROUPS).document(groupID).collection(USERS).document(userID).collection(SHOPPING);

        final Query shoppingQuery = ref_shopping.orderBy(TIMESTAMP, Query.Direction.DESCENDING);

        final FirestoreRecyclerOptions<Article> options = new FirestoreRecyclerOptions.Builder<Article>()
                .setQuery(shoppingQuery, Article.class)
                .build();

        final LinearLayoutManager layoutManager = new LinearLayoutManager(getContext());

        adapter = new FirestoreRecyclerAdapter<Article, ShoppingHolder>(options) {

            @NonNull
            @Override
            public ShoppingHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
                View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_article, parent, false);
                return new ShoppingHolder(view);
            }

            @Override
            protected void onBindViewHolder(@NonNull final ShoppingHolder holder, int position, @NonNull final Article model) {
                // Set Checkbox-State
                boolean checked = holder.itemView.getContext().getSharedPreferences(SHOPPING, Context.MODE_PRIVATE)
                        .getBoolean(model.getKey(), model.isChecked());
                holder.checkBox.setChecked(checked);
                // Artikelname
                holder.tv_article.setText(model.getName());
                // Username
                users = ((HomeActivity) getActivity()).getUsers();
                final User articleUser = users.get(model.getUserID());
                holder.tv_username.setText(articleUser.getName());
                // Timestamp
                if (model.getTimestamp() != null) {
                    holder.tv_datum.setText(
                            DateUtils.getRelativeTimeSpanString(
                                    model.getTimestamp().getTime(),
                                    System.currentTimeMillis() + BUFFER,
                                    DateUtils.MINUTE_IN_MILLIS,
                                    DateUtils.FORMAT_ABBREV_RELATIVE)
                    );
                    holder.tv_datum.setVisibility(View.VISIBLE);
                } else {
                    holder.tv_datum.setVisibility(View.GONE);
                }

                // Group-Icon-Visibility
                if (model.isPrivate()) holder.img_group.setVisibility(View.GONE);
                else holder.img_group.setVisibility(View.VISIBLE);

                // Change Checkbox-State on Click
                holder.itemView.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        model.setChecked(!model.isChecked());
                        holder.itemView.getContext().getSharedPreferences(SHOPPING, Context.MODE_PRIVATE)
                                .edit().putBoolean(model.getKey(), model.isChecked()).apply();
                        if (model.isChecked()) {
                            holder.checkBox.setChecked(true);
                        }
                        else {
                            holder.checkBox.setChecked(false);
                        }

                    }
                });

                holder.itemView.setOnLongClickListener(new View.OnLongClickListener() {
                    @Override
                    public boolean onLongClick(View v) {
                        if (!model.getUserID().equals(userID)) return false;
                        Dialog dialog = new UserSelectionDialog(getContext(),
                                android.R.style.Theme_Translucent_NoTitleBar,
                                model);
                        dialog.show();
                        return true;
                    }
                });
            }

            @Override
            public void onDataChanged() {
                layoutManager.scrollToPositionWithOffset(0,0);
                if (getItemCount() == 0) placeholder.setVisibility(View.VISIBLE);
                else placeholder.setVisibility(View.GONE);
                super.onDataChanged();
            }
        };

        recView.setLayoutManager(layoutManager);
        recView.addItemDecoration(new DividerItemDecoration(recView.getContext(), DividerItemDecoration.VERTICAL));
        recView.setAdapter(adapter);
    }

    // Custom Viewholder for Shopping-Articles
    public class ShoppingHolder extends RecyclerView.ViewHolder {
        TextView tv_username, tv_datum, tv_article;
        CheckBox checkBox;
        ImageView img_group;

        public ShoppingHolder(View itemView) {
            super(itemView);
            tv_username = itemView.findViewById(R.id.username);
            tv_datum = itemView.findViewById(R.id.datum);
            tv_article = itemView.findViewById(R.id.article);
            checkBox = itemView.findViewById(R.id.checkbox);
            img_group = itemView.findViewById(R.id.img_group);
        }
    }

    // Dialog to choose the involved users for the given (shopping) article
    private class UserSelectionDialog extends Dialog {

        private Button btn;
        private RecyclerView recView;
        private Article article;

        public UserSelectionDialog(@NonNull Context context, int themeResId, Article article) {
            super(context, themeResId);
            // set the background to a translucent black
            getWindow().setBackgroundDrawableResource(R.color.translucent_black);
            this.article = article;
        }

        @Override
        protected void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            setContentView(R.layout.dialog_userlist);
            btn = findViewById(R.id.btn_save);
            recView = findViewById(R.id.recView);

            recView.setHasFixedSize(true);
            recView.setLayoutManager(new LinearLayoutManager(getContext()));

            // create adapter for user selection with no user selected
            // cannot select involved users because each user has it's own shopping list
            final UserSelectionAdapter adapter = new UserSelectionAdapter(
                    new ArrayList<>(users.values()),
                    new ArrayList<String>());
            recView.setAdapter(adapter);

            // save article in selected users shopping list and delete the article for everyone else
            btn.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    final ArrayList<String> involvedIDs = adapter.getInvolvedIDs();
                    // check if current user is involved
                    if (!involvedIDs.contains(userID)) {
                        Toast.makeText(getContext(), "Du musst ausgewählt sein...", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    // check if product is private
                    if (involvedIDs.size() == 1) article.setPrivate(true);
                    else article.setPrivate(false);
                    // add/delete article in each users shopping list depending whether user is 'involved'
                    final CollectionReference ref_users = db.collection(GROUPS).document(groupID).collection(USERS);
                    db.runTransaction(new Transaction.Function<Void>() {
                        @Nullable
                        @Override
                        public Void apply(@NonNull Transaction transaction) throws FirebaseFirestoreException {
                            for (String id : users.keySet()) {
                                if (involvedIDs.contains(id))
                                    transaction.set(ref_users.document(id).collection(SHOPPING).document(article.getKey()), article);
                                else
                                    transaction.delete(ref_users.document(id).collection(SHOPPING).document(article.getKey()));
                            }
                            return null;
                        }
                    }).addOnFailureListener(new OnFailureListener() {
                        @Override
                        public void onFailure(@NonNull Exception e) {
                            Toast.makeText(getContext(), "Änderung fehlgeschlagen...", Toast.LENGTH_SHORT).show();
                        }
                    });
                    dismiss();
                }
            });
        }
    }


    private void loadStock() {
        final Query query_stock = db.collection(GROUPS).document(groupID).collection(USERS).document(userID)
                .collection(STOCK).orderBy(NAME, Query.Direction.ASCENDING);

        final FirestoreRecyclerOptions<StockProduct> options = new FirestoreRecyclerOptions.Builder<StockProduct>()
                .setQuery(query_stock, StockProduct.class)
                .build();

        adapter_stock = new FirestoreRecyclerAdapter<StockProduct, StockHolder>(options) {
            @NonNull
            @Override
            public StockHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
                View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_stock_small, parent, false);
                return new StockHolder(view);
            }

            @Override
            protected void onBindViewHolder(@NonNull final StockHolder holder, int position, @NonNull final StockProduct model) {
                holder.tv_name.setText(model.getName());

                if (model.getInvolvedIDs().size() > 1) holder.img_group.setVisibility(View.VISIBLE);
                else holder.img_group.setVisibility(View.GONE);

                holder.itemView.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        final Article article = new Article(model.getKey(), model.getName(), userID, model.getInvolvedIDs().size() == 1);
                        WriteBatch batch = db.batch();
                        for (String id : model.getInvolvedIDs()) {
                            DocumentReference ref = db.collection(GROUPS).document(groupID).collection(USERS).document(id).collection(SHOPPING).document(article.getKey());
                            batch.set(ref, article);
                        }
                        batch.commit();

                    }
                });

                holder.btn_detail.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        Intent i = new Intent(holder.itemView.getContext(), StockProductActivity.class);
                        i.putExtra(STOCKID, model);
                        i.putExtra(USERS, users);
                        holder.itemView.getContext().startActivity(i);
                    }
                });
            }

            @Override
            public void onDataChanged() {
                super.onDataChanged();
                if (getItemCount() == 0) placeholder_stock.setVisibility(View.VISIBLE);
                else placeholder_stock.setVisibility(View.GONE);
            }
        };

        recView_stock.setLayoutManager(new LinearLayoutManager(getContext()));
        recView_stock.addItemDecoration(new DividerItemDecoration(recView_stock.getContext(), DividerItemDecoration.VERTICAL));
        recView_stock.setAdapter(adapter_stock);

    }

    // Custom Viewholder for Stock-Products
    public class StockHolder extends RecyclerView.ViewHolder {
        TextView tv_name;
        ImageView img_group;
        ImageButton btn_detail;

        public StockHolder(View itemView) {
            super(itemView);
            tv_name = itemView.findViewById(R.id.name);
            img_group = itemView.findViewById(R.id.ic_group);
            btn_detail = itemView.findViewById(R.id.btn_detail);
        }
    }

    // Hide Keyboard on click outside
    @Override
    public void onFocusChange(View view, boolean hasFocus) {
        if (!hasFocus) {
            InputMethodManager inputMethodManager = (InputMethodManager)getActivity().getSystemService(Activity.INPUT_METHOD_SERVICE);
            inputMethodManager.hideSoftInputFromWindow(view.getWindowToken(), 0);
        }
    }

    @Override
    public void onClick(View view) {
        int id = view.getId();
        if (id == R.id.btn_save)
            saveArticle();
        else if (id == R.id.btn_shopping)
            findCheckedArticles();
        else if (id == R.id.btn_group) {
            forAll = !forAll;
            if (forAll) btn_group.setColorFilter(getResources().getColor(R.color.colorAccent));
            else btn_group.setColorFilter(getResources().getColor(R.color.colorPrimary));
        }
        else if (id == R.id.btn_stock) {
            Intent intent_stock = new Intent(getContext(), StockAddActivity.class);
            intent_stock.putExtra(USERS, users);
            startActivity(intent_stock);
        }
    }

    @Override
    public void onStart() {
        super.onStart();
        if (adapter != null) adapter.startListening();
        if (!hasNulls() && getContext().getSharedPreferences(groupID, Context.MODE_PRIVATE).getBoolean(STOCK, false)) {
            if (adapter_stock == null) loadStock();
            adapter_stock.startListening();
            layout_stock.setVisibility(View.VISIBLE);
        } else layout_stock.setVisibility(View.GONE);
    }

    @Override
    public void onStop() {
        super.onStop();
        if (adapter != null) adapter.stopListening();
        if (adapter_stock != null) adapter_stock.stopListening();
    }

    private FirebaseFirestore db;
    private CollectionReference ref_shopping;
    private FirestoreRecyclerAdapter adapter, adapter_stock;
    private String groupID, userID;
    private HashMap<String, User> users;
    private boolean forAll = false;

    private View mainView;
    private LinearLayout layout_stock;
    private RecyclerView recView, recView_stock;
    private EditText et_article;
    private ImageButton btn_save, btn_group, btn_shopping, btn_stock;
    private TextView placeholder, placeholder_stock;
}
