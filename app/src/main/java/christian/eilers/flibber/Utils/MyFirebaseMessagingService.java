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
import android.support.v4.app.NotificationManagerCompat;

import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;

import christian.eilers.flibber.Home.HomeActivity;
import christian.eilers.flibber.R;

public class MyFirebaseMessagingService extends FirebaseMessagingService {

    private final int SHOPPING = 123;

    @Override
    public void onMessageReceived(RemoteMessage remoteMessage) {
        super.onMessageReceived(remoteMessage);
        if (remoteMessage.getData().size() == 0) return;

        // Read out the article name from the data-message
        String articleName = remoteMessage.getData().get("name");
        String channelID = getString(R.string.app_name); // set a channelID

        // Configure the Inbox-Style to make multiple lines
        NotificationCompat.InboxStyle inboxStyle = new NotificationCompat.InboxStyle();
        inboxStyle.setBigContentTitle("Einkaufsliste:");
        inboxStyle.addLine(articleName);

        // Intent for onClick-Event
        Intent clickIntent = new Intent(this, HomeActivity.class);
        clickIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        PendingIntent clickPendingIntent = PendingIntent.getActivity(this, 0 , clickIntent, PendingIntent.FLAG_ONE_SHOT);

        // Default Notification Sound
        Uri defaultSoundUri= RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);

        // Build the Notification
        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, channelID)
                .setSmallIcon(R.drawable.recipes_book)
                .setContentTitle(articleName)
                .setContentText("wurde zur Einkaufsliste hinzugefügt")
                .setAutoCancel(true)
                .setSound(defaultSoundUri)
                .setContentIntent(clickPendingIntent)
                .setStyle(inboxStyle);

        NotificationManager notificationManager =
                (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);

        // Since android Oreo notification channel is needed.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(channelID, "Flibber",
                    NotificationManager.IMPORTANCE_DEFAULT);
            notificationManager.createNotificationChannel(channel);
        }

        notificationManager.notify(SHOPPING, builder.build());
    }
}
