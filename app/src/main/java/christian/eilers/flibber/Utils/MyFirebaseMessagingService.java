package christian.eilers.flibber.Utils;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Build;
import android.support.v4.app.NotificationCompat;

import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;

import java.util.HashSet;
import java.util.Set;

import christian.eilers.flibber.Home.HomeActivity;
import christian.eilers.flibber.R;

import static christian.eilers.flibber.Utils.Strings.*;

public class MyFirebaseMessagingService extends FirebaseMessagingService {

    private final int CHANNEL_SHOPPING = 123;

    @Override
    public void onMessageReceived(RemoteMessage remoteMessage) {
        super.onMessageReceived(remoteMessage);
        if (remoteMessage.getData().size() == 0) return;

        // Get the type of Notification
        String type = remoteMessage.getData().get(TYPE);
        switch (type) {
            case SHOPPING:
                String articleName = remoteMessage.getData().get(NAME);
                shoppingNotification(articleName);
                break;
            case TASKS:
                String taskName = remoteMessage.getData().get(NAME);
                taskNotification(taskName);
                break;
        }
    }

    private void taskNotification(String taskName) {
        // Intent for onClick-Event
        Intent clickIntent = new Intent(this, HomeActivity.class);
        clickIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        PendingIntent clickPendingIntent = PendingIntent.getActivity(this, 0 , clickIntent, PendingIntent.FLAG_ONE_SHOT);

        // Default Notification Sound
        Uri defaultSoundUri= RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);

        // Build the Notification
        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, CHANNEL_ID_TASKS)
                .setSmallIcon(R.drawable.ic_notification)
                .setContentTitle("Aufgaben-Erinnerung:")
                .setContentText(taskName)
                .setAutoCancel(true)
                .setSound(defaultSoundUri)
                .setContentIntent(clickPendingIntent);

        NotificationManager notificationManager =
                (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);

        // Since android Oreo notification channel is needed.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(CHANNEL_ID_TASKS, "Aufgaben",
                    NotificationManager.IMPORTANCE_DEFAULT);
            notificationManager.createNotificationChannel(channel);
        }

        notificationManager.notify((int) System.currentTimeMillis(), builder.build());
    }

    private void shoppingNotification(String articleName) {
        // Read out Articles from the Shared Preferences
        Set<String> currentArticles = getSharedPreferences(NOTIFICATIONS, Context.MODE_PRIVATE).getStringSet(SHOPPING, null);
        if (currentArticles != null) currentArticles.add(articleName);
        else {
            currentArticles = new HashSet<>();
            currentArticles.add(articleName);
        }

        // Add the new Article to the SharedPreference
        SharedPreferences.Editor editor = getSharedPreferences(NOTIFICATIONS, Context.MODE_PRIVATE).edit();
        editor.putStringSet(SHOPPING, currentArticles);
        editor.apply();

        // Configure the Inbox-Style to display all recently added articles
        NotificationCompat.InboxStyle inboxStyle = new NotificationCompat.InboxStyle();
        inboxStyle.setBigContentTitle("Neu auf der Einkaufsliste:");
        for (String article : currentArticles) inboxStyle.addLine(article);

        // Intent for onClick-Event
        Intent clickIntent = new Intent(this, HomeActivity.class);
        clickIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        PendingIntent clickPendingIntent = PendingIntent.getActivity(this, 0 , clickIntent, PendingIntent.FLAG_ONE_SHOT);

        // Default Notification Sound
        Uri defaultSoundUri= RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);

        // Build the Notification
        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, CHANNEL_ID_SHOPPING)
                .setSmallIcon(R.drawable.ic_notification)
                .setContentTitle(articleName)
                .setContentText("wurde zur Einkaufsliste hinzugefügt")
                .setStyle(inboxStyle)
                .setAutoCancel(true)
                .setSound(defaultSoundUri)
                .setContentIntent(clickPendingIntent);

        NotificationManager notificationManager =
                (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);

        // Since android Oreo notification channel is needed.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(CHANNEL_ID_SHOPPING, "Einkaufsliste",
                    NotificationManager.IMPORTANCE_DEFAULT);
            notificationManager.createNotificationChannel(channel);
        }

        notificationManager.notify(CHANNEL_SHOPPING, builder.build());
    }
}
