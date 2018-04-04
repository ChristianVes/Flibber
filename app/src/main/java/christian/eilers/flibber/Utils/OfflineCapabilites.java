package christian.eilers.flibber.Utils;

import android.app.Application;

import com.crashlytics.android.Crashlytics;
import com.squareup.picasso.OkHttpDownloader;
import com.squareup.picasso.Picasso;

import io.fabric.sdk.android.Fabric;

public class OfflineCapabilites extends Application {

    @Override
    public void onCreate() {
        super.onCreate();
        Fabric.with(this, new Crashlytics());
    }
}
