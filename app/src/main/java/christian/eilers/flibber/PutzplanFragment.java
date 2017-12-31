package christian.eilers.flibber;


import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

public class PutzplanFragment extends Fragment {

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {

        View view = inflater.inflate(R.layout.fragment_putzplan, container, false);
        return view;
    }

    // newInstance constructor for creating fragment with arguments
    public static PutzplanFragment newInstance() {
        return new PutzplanFragment();
    }


}
