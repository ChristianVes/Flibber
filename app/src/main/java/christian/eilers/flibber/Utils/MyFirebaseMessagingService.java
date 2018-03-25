package christian.eilers.flibber.Utils;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Build;
import android.support.v4.app.NotificationCompat;

import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;

import java.util.ArrayList;

import christian.eilers.flibber.Home.HomeActivity;
import christian.eilers.flibber.R;

public class MyFirebaseMessagingService extends FirebaseMessagingService {

    public ArrayList<String> messages = new ArrayList<>();

    @Override
    public void onMessageReceived(RemoteMessage remoteMessage) {
        super.onMessageReceived(remoteMessage);

        if (remoteMessage.getData().size() == 0) return;
        String articleName = remoteMessage.getData().get("name");
        String channelID = getString(R.string.fcm_fallback_notification_channel_label);

        if (messages != null) messages.add(articleName);

        NotificationCompat.InboxStyle inboxStyle = new NotificationCompat.InboxStyle();
        inboxStyle.setBigContentTitle("Einkaufsliste");
        inboxStyle.setSummaryText("Summary");
        for(int i = 0; i < messages.size(); i++) inboxStyle.addLine(messages.get(i) + " added");

        Intent intent = new Intent(this, HomeActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0 , intent, PendingIntent.FLAG_ONE_SHOT);

        Uri defaultSoundUri= RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);

        NotificationCompat.Builder notiBuilder = new NotificationCompat.Builder(this, channelID)
                .setSmallIcon(R.drawable.recipes_book)
                .setContentTitle("Einkaufsliste")
                .setContentText(articleName + " hinzugefügt")
                .setAutoCancel(true)
                .setStyle(inboxStyle)
                .setSound(defaultSoundUri)
                .setContentIntent(pendingIntent);

        NotificationManager notificationManager =
                (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);

        // Since android Oreo notification channel is needed.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(channelID,
                    "Flibber",
                    NotificationManager.IMPORTANCE_DEFAULT);
            notificationManager.createNotificationChannel(channel);
        }

        notificationManager.notify(123, notiBuilder.build());
    }
}
