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
import android.text.Spannable;
import android.text.SpannableString;
import android.text.TextUtils;
import android.text.style.StyleSpan;

import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;

import java.util.HashSet;
import java.util.Set;

import christian.eilers.flibber.Home.HomeActivity;
import christian.eilers.flibber.R;

import static christian.eilers.flibber.Utils.Strings.*;

public class MyFirebaseMessagingService extends FirebaseMessagingService {

    // private final int CHANNEL_SHOPPING = 123;
    // private final int CHANNEL_FINANCES = 321;
    private final int CHANNEL_ID = 12021997;

    @Override
    public void onMessageReceived(RemoteMessage remoteMessage) {
        super.onMessageReceived(remoteMessage);
        if (remoteMessage.getData().size() == 0) return;

        // Get the type of Notification
        String type = remoteMessage.getData().get(TYPE);
        String groupID = remoteMessage.getData().get("groupID");

        String title, description, title_short, description_short;
        switch (type) {
            case SHOPPING:
                if(!getSharedPreferences(groupID, Context.MODE_PRIVATE).getBoolean(SHOPPING, true)) break;
                String articleName = remoteMessage.getData().get(NAME);
                title = articleName;
                description = "wurde zur Einkaufsliste hinzugefügt";
                showNotification(title, description, title, description);
                break;
            case TASKS:
                if(!getSharedPreferences(groupID, Context.MODE_PRIVATE).getBoolean(TASKS, true)) break;
                String taskName = remoteMessage.getData().get(NAME);
                title = "Aufgabe: " + taskName;
                description = "ist fällig";
                showNotification(title, description, title, description);
                break;
            case TASK_SKIPPED:
                if(!getSharedPreferences(groupID, Context.MODE_PRIVATE).getBoolean(TASKS, true)) break;
                String taskName_skipped = remoteMessage.getData().get(NAME);
                String fromUser = remoteMessage.getData().get(FROMUSER);
                title = "Aufgabe: " + taskName_skipped;
                description = "hat " + fromUser + " an dich weitergegeben";
                showNotification(title, description, title, description);
                break;
            case NOTES:
                if(!getSharedPreferences(groupID, Context.MODE_PRIVATE).getBoolean(NOTES, true)) break;
                String username_notes = remoteMessage.getData().get(USER);
                String title_notes = remoteMessage.getData().get(TITLE);
                String description_notes = remoteMessage.getData().get(DESCRIPTION);
                // TODO: If - Bedingungen falls title/description empty
                title = username_notes + ": " + title_notes;
                description = description_notes;
                showNotification(title, description, title, description);
                break;
            case FINANCES:
                if(!getSharedPreferences(groupID, Context.MODE_PRIVATE).getBoolean(FINANCES, false)) break;
                String name_payment = remoteMessage.getData().get(NAME);
                title = "Finanzeintrag: " + name_payment;
                description = "wurde hinzugefügt";
                showNotification(title, description, title, description);
                break;
        }

    }

    private void showNotification(String title, String description, String title_short, String description_short) {
        // Read out old Notifications from the Shared Preferences
        Set<String> set_titles = getSharedPreferences(NOTIFICATIONS, Context.MODE_PRIVATE)
                .getStringSet("TITLES", null);
        Set<String> set_descriptions = getSharedPreferences(NOTIFICATIONS, Context.MODE_PRIVATE)
                .getStringSet("DESCRIPTIONS", null);

        // TODO: Problem -> Keine Dupilkate erlaubt in Sets
        // Möglichkeit: Current Time davor setzen und mit Split wieder entfernen

        if (set_titles != null && set_descriptions != null) {
            set_titles.add(title_short);
            set_descriptions.add(description_short);
        }
        else {
            set_titles = new HashSet<>();
            set_titles.add(title_short);
            set_descriptions = new HashSet<>();
            set_descriptions.add(description_short);
        }

        // Add the new notification to the SharedPreference
        SharedPreferences.Editor editor = getSharedPreferences(NOTIFICATIONS, Context.MODE_PRIVATE).edit();
        editor.putStringSet("DESCRIPTIONS", set_descriptions);
        editor.putStringSet("TITLES", set_titles);
        editor.apply();

        // Configure the Inbox-Style to display all recently notifications
        NotificationCompat.InboxStyle inboxStyle = new NotificationCompat.InboxStyle();
        inboxStyle.setBigContentTitle("Headquarter");
        inboxStyle.setSummaryText(set_titles.size() + " neue Benachrichtigungen");
        String[] title_array = set_titles.toArray(new String[set_titles.size()]);
        String[] desc_array = set_descriptions.toArray(new String[set_descriptions.size()]);
        for (int i = 0; i < desc_array.length; i++) {
            Spannable line_spannable = new SpannableString(title_array[i] + " " + desc_array[i]);
            line_spannable.setSpan(new StyleSpan(android.graphics.Typeface.BOLD), 0, title_array[i].length(),
                    Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);

            inboxStyle.addLine(line_spannable);
        }

        // Intent for onClick-Event
        Intent clickIntent = new Intent(this, HomeActivity.class);
        clickIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        PendingIntent clickPendingIntent = PendingIntent.getActivity(this, 0 , clickIntent, PendingIntent.FLAG_ONE_SHOT);

        // Default Notification Sound
        Uri defaultSoundUri= RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);

        // Build the Notification
        NotificationCompat.Builder builder;
        if (set_titles.size() == 1) {
            builder = new NotificationCompat.Builder(this, CHANNEL_ID_ALL)
                    .setSmallIcon(R.drawable.ic_notification)
                    .setContentTitle(title)
                    .setContentText(description)
                    .setAutoCancel(true)
                    .setSound(defaultSoundUri)
                    .setContentIntent(clickPendingIntent);
        } else {
            builder = new NotificationCompat.Builder(this, CHANNEL_ID_ALL)
                    .setSmallIcon(R.drawable.ic_notification)
                    .setContentTitle("Headquarter")
                    .setContentText(set_titles.size() + " neue Benachrichtigungen")
                    .setStyle(inboxStyle)
                    .setAutoCancel(true)
                    .setSound(defaultSoundUri)
                    .setContentIntent(clickPendingIntent);
        }

        NotificationManager notificationManager =
                (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);

        // Since android Oreo notification channel is needed.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(CHANNEL_ID_ALL, "Headquarter",
                    NotificationManager.IMPORTANCE_DEFAULT);
            notificationManager.createNotificationChannel(channel);
        }

        notificationManager.notify(CHANNEL_ID, builder.build());

    }










    /*private void taskNotification(String taskName) {
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

    private void taskSkippedNotification(String taskName, String fromUser) {
        // Intent for onClick-Event
        Intent clickIntent = new Intent(this, HomeActivity.class);
        clickIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        PendingIntent clickPendingIntent = PendingIntent.getActivity(this, 0 , clickIntent, PendingIntent.FLAG_ONE_SHOT);

        // Default Notification Sound
        Uri defaultSoundUri= RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);

        // Build the Notification
        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, CHANNEL_ID_TASKS)
                .setSmallIcon(R.drawable.ic_notification)
                .setContentTitle("Aufgabe: " + taskName)
                .setContentText("hat " + fromUser + " an dich weitergegeben.")
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

    private void financeNotification(String paymentTitle) {
        // Read out Articles from the Shared Preferences
        Set<String> currentPayments = getSharedPreferences(NOTIFICATIONS, Context.MODE_PRIVATE).getStringSet(FINANCES, null);
        if (currentPayments != null) currentPayments.add(paymentTitle);
        else {
            currentPayments = new HashSet<>();
            currentPayments.add(paymentTitle);
        }

        // Add the new Article to the SharedPreference
        SharedPreferences.Editor editor = getSharedPreferences(NOTIFICATIONS, Context.MODE_PRIVATE).edit();
        editor.putStringSet(FINANCES, currentPayments);
        editor.apply();

        // Configure the Inbox-Style to display all recently added articles
        NotificationCompat.InboxStyle inboxStyle = new NotificationCompat.InboxStyle();
        inboxStyle.setBigContentTitle("Neue Finanzeinträge:");
        for (String payment : currentPayments) inboxStyle.addLine(payment);

        // Intent for onClick-Event
        Intent clickIntent = new Intent(this, HomeActivity.class);
        clickIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        PendingIntent clickPendingIntent = PendingIntent.getActivity(this, 0 , clickIntent, PendingIntent.FLAG_ONE_SHOT);

        // Default Notification Sound
        Uri defaultSoundUri= RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);

        // Build the Notification
        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, CHANNEL_ID_FINANCES)
                .setSmallIcon(R.drawable.ic_notification)
                .setContentTitle(paymentTitle)
                .setContentText("Finanzeintrag wurde hinzugefügt")
                .setStyle(inboxStyle)
                .setAutoCancel(true)
                .setSound(defaultSoundUri)
                .setContentIntent(clickPendingIntent);

        NotificationManager notificationManager =
                (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);

        // Since android Oreo notification channel is needed.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(CHANNEL_ID_FINANCES, "Finanzen",
                    NotificationManager.IMPORTANCE_DEFAULT);
            notificationManager.createNotificationChannel(channel);
        }

        notificationManager.notify(CHANNEL_FINANCES, builder.build());
    }

    private void notesNotification(String username, String title, String description) {
        // Intent for onClick-Event
        Intent clickIntent = new Intent(this, HomeActivity.class);
        clickIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        PendingIntent clickPendingIntent = PendingIntent.getActivity(this, 0 , clickIntent, PendingIntent.FLAG_ONE_SHOT);

        // Default Notification Sound
        Uri defaultSoundUri= RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);

        // Big Notification - Style
        NotificationCompat.BigTextStyle style = new NotificationCompat.BigTextStyle();
        if (!TextUtils.isEmpty(title)) style.setBigContentTitle(title);
        if (!TextUtils.isEmpty(description)) style.bigText(description);
        style.setSummaryText(username);

        // Build the Notification
        if (TextUtils.isEmpty(title)) title = description;
        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, CHANNEL_ID_NOTES)
                .setSmallIcon(R.drawable.ic_notification)
                .setContentTitle(username)
                .setContentText(title)
                .setStyle(style)
                .setAutoCancel(true)
                .setSound(defaultSoundUri)
                .setContentIntent(clickPendingIntent);

        NotificationManager notificationManager =
                (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);

        // Since android Oreo notification channel is needed.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(CHANNEL_ID_NOTES, "Pinnwand",
                    NotificationManager.IMPORTANCE_DEFAULT);
            notificationManager.createNotificationChannel(channel);
        }

        notificationManager.notify((int) System.currentTimeMillis(), builder.build());
    }*/

}
