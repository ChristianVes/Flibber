package christian.eilers.flibber.Adapter;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import org.fabiomsr.moneytextview.MoneyTextView;

import java.util.ArrayList;
import java.util.HashMap;

import christian.eilers.flibber.Models.Offset;
import christian.eilers.flibber.Models.User;
import christian.eilers.flibber.R;

public class BalanceOffsetAdapter extends RecyclerView.Adapter<BalanceOffsetAdapter.ViewHolder> {

    private HashMap<String, User> map_user;
    private ArrayList<Offset> list_offset;
    private Context context;

    public BalanceOffsetAdapter(HashMap<String, User> map_user, ArrayList<Offset> list_offset) {
        this.map_user = map_user;
        this.list_offset = list_offset;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_offset, parent, false);
        context = parent.getContext();
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull final ViewHolder holder, final int position) {
        final Offset offset = list_offset.get(position);
        final User user_from = map_user.get(offset.getFromID());
        final User user_to = map_user.get(offset.getToID());
        // USERNAMES
        holder.tv_from.setText(user_from.getName());
        holder.tv_to.setText(user_to.getName());
        // AMOUNT
        holder.tv_money.setAmount(offset.getValue());
    }

    @Override
    public int getItemCount() {
        return list_offset.size();
    }

    public class ViewHolder extends RecyclerView.ViewHolder {
        TextView tv_from, tv_to;
        MoneyTextView tv_money;

        public ViewHolder(final View itemView) {
            super(itemView);
            tv_from = itemView.findViewById(R.id.from);
            tv_to = itemView.findViewById(R.id.to);
            tv_money = itemView.findViewById(R.id.money);

        }
    }
}
