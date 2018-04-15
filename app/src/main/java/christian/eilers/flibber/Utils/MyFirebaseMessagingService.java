package christian.eilers.flibber.Utils;

import android.app.Notification;
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
import android.support.v4.app.NotificationManagerCompat;
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

    private final int CHANNEL_SHOPPING = 123;
    private final int CHANNEL_FINANCES = 321;
    private final int CHANNEL_TASKS = 3214;
    private final int CHANNEL_ID = 12021997;
    private final int SUMMARY_ID = 0;

    @Override
    public void onMessageReceived(RemoteMessage remoteMessage) {
        super.onMessageReceived(remoteMessage);
        if (remoteMessage.getData().size() == 0) return;

        // Get the type of Notification
        String type = remoteMessage.getData().get(TYPE);
        String groupID = remoteMessage.getData().get("groupID");

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            switch (type) {
                case SHOPPING:
                    if(!getSharedPreferences(groupID, Context.MODE_PRIVATE).getBoolean(SHOPPING, true)) break;
                    String articleName = remoteMessage.getData().get(NAME);
                    shoppingNotification(articleName);
                    break;
                case TASKS:
                    if(!getSharedPreferences(groupID, Context.MODE_PRIVATE).getBoolean(TASKS, true)) break;
                    String taskName = remoteMessage.getData().get(NAME);
                    taskNotification(taskName);
                    break;
                case TASK_SKIPPED:
                    if(!getSharedPreferences(groupID, Context.MODE_PRIVATE).getBoolean(TASKS, true)) break;
                    String taskName_skipped = remoteMessage.getData().get(NAME);
                    String fromUser = remoteMessage.getData().get(FROMUSER);
                    taskSkippedNotification(taskName_skipped, fromUser);
                    break;
                case NOTES:
                    if(!getSharedPreferences(groupID, Context.MODE_PRIVATE).getBoolean(NOTES, true)) break;
                    String username_notes = remoteMessage.getData().get(USER);
                    String title_notes = remoteMessage.getData().get(TITLE);
                    String description_notes = remoteMessage.getData().get(DESCRIPTION);
                    notesNotification(username_notes, title_notes, description_notes);
                    break;
                case FINANCES:
                    if(!getSharedPreferences(groupID, Context.MODE_PRIVATE).getBoolean(FINANCES, false)) break;
                    String name_payment = remoteMessage.getData().get(NAME);
                    paymentNotification(name_payment);
                    break;
            }
        } else {
            String title, description, title_short, description_short;
            switch (type) {
                case SHOPPING:
                    if (!getSharedPreferences(groupID, Context.MODE_PRIVATE).getBoolean(SHOPPING, true))
                        break;
                    String articleName = remoteMessage.getData().get(NAME);
                    title = articleName;
                    description = "wurde zur Einkaufsliste hinzugefügt";
                    showNotification(title, description, title, description);
                    break;
                case TASKS:
                    if (!getSharedPreferences(groupID, Context.MODE_PRIVATE).getBoolean(TASKS, true))
                        break;
                    String taskName = remoteMessage.getData().get(NAME);
                    title = taskName;
                    description = "Anstehende Aufgabe: " + taskName;
                    title_short = "Reminder";
                    description_short = "für anstehende Aufgabe " + taskName;
                    showNotification(title, description, title_short, description_short);
                    break;
                case TASK_SKIPPED:
                    if (!getSharedPreferences(groupID, Context.MODE_PRIVATE).getBoolean(TASKS, true))
                        break;
                    String taskName_skipped = remoteMessage.getData().get(NAME);
                    String fromUser = remoteMessage.getData().get(FROMUSER);
                    title = "Aufgabe: " + taskName_skipped;
                    description = fromUser + "hat " + taskName_skipped + " an dich weitergegeben";
                    title_short = taskName_skipped;
                    description_short = " wurde von " + fromUser + "an dich weitergegeben";
                    showNotification(title, description, title_short, description_short);
                    break;
                case NOTES:
                    if (!getSharedPreferences(groupID, Context.MODE_PRIVATE).getBoolean(NOTES, true))
                        break;
                    String username_notes = remoteMessage.getData().get(USER);
                    String title_notes = remoteMessage.getData().get(TITLE);
                    String description_notes = remoteMessage.getData().get(DESCRIPTION);
                    if (TextUtils.isEmpty(title_notes)) {
                        title = username_notes;
                        description = description_notes;
                        title_short = username_notes;
                        description_short = description_notes;
                    } else if (TextUtils.isEmpty(description_notes)) {
                        title = username_notes;
                        description = title_notes;
                        title_short = username_notes;
                        description_short = title_notes;
                    } else {
                        title = title_notes;
                        description = description_notes;
                        title_short = username_notes;
                        description_short = title_notes;
                    }
                    showNotification(title, description, title_short, description_short);
                    break;
                case FINANCES:
                    if (!getSharedPreferences(groupID, Context.MODE_PRIVATE).getBoolean(FINANCES, false))
                        break;
                    String name_payment = remoteMessage.getData().get(NAME);
                    title = "Neuer Finanzeintrag";
                    description = name_payment;
                    showNotification(title, description, title, description);
                    break;
            }
        }
    }

    private void showNotification(String title, String description, String title_short, String description_short) {
        // Read out old Notifications from the Shared Preferences
        Set<String> set_titles = getSharedPreferences(NOTIFICATIONS, Context.MODE_PRIVATE)
                .getStringSet("TITLES", null);
        Set<String> set_descriptions = getSharedPreferences(NOTIFICATIONS, Context.MODE_PRIVATE)
                .getStringSet("DESCRIPTIONS", null);

        int time = (int) System.currentTimeMillis();

        // Check if old notifications exists
        // Add the time to each String to avoid duplicates
        if (set_titles != null && set_descriptions != null) {
            set_titles.add(time + ":" + title_short);
            set_descriptions.add(time + ":" + description_short);
        } else {
            set_titles = new HashSet<>();
            set_titles.add(time + ":" + title_short);
            set_descriptions = new HashSet<>();
            set_descriptions.add(time + ":" + description_short);
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
        // Cast sets to arrays
        String[] title_array = set_titles.toArray(new String[set_titles.size()]);
        String[] desc_array = set_descriptions.toArray(new String[set_descriptions.size()]);
        // iterate over all notifications to display
        for (int i = 0; i < title_array.length; i++) {
            // remove time from the string
            String boldText = title_array[i].split(":", 2)[1];
            String normalText = desc_array[i].split(":", 2)[1];
            // Make the title bold
            Spannable line_spannable = new SpannableString(boldText + " " + normalText);
            line_spannable.setSpan(new StyleSpan(android.graphics.Typeface.BOLD), 0, boldText.length(),
                    Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
            // add the notification
            inboxStyle.addLine(line_spannable);
        }

        // Intent for onClick-Event
        Intent clickIntent = new Intent(this, HomeActivity.class);
        clickIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        PendingIntent clickPendingIntent = PendingIntent.getActivity(this, 0, clickIntent, PendingIntent.FLAG_ONE_SHOT);

        // Default Notification Sound
        Uri defaultSoundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);

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
        inboxStyle.setBigContentTitle("Kürzlich hinzugefügte Einkaufsartikel:");
        for (String article : currentArticles) inboxStyle.addLine(article);

        // Intent for onClick-Event
        Intent clickIntent = new Intent(this, HomeActivity.class);
        clickIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        PendingIntent clickPendingIntent = PendingIntent.getActivity(this, 0, clickIntent, PendingIntent.FLAG_ONE_SHOT);

        // Default Notification Sound
        Uri defaultSoundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);

        // Build the Notification
        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, CHANNEL_ID_SHOPPING)
                .setSmallIcon(R.drawable.ic_notification)
                .setContentTitle(articleName)
                .setContentText("wurde zur Einkaufsliste hinzugefügt")
                .setAutoCancel(true)
                .setSound(defaultSoundUri)
                .setContentIntent(clickPendingIntent)
                .setGroup(CHANNEL_ID_ALL);
        if (currentArticles.size() > 1) builder.setStyle(inboxStyle);


        NotificationManager notificationManager =
                (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);

        // Since android Oreo notification channel is needed.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(CHANNEL_ID_SHOPPING, "Einkaufsliste",
                    NotificationManager.IMPORTANCE_DEFAULT);
            notificationManager.createNotificationChannel(channel);
        }

        notificationManager.notify(CHANNEL_SHOPPING, builder.build());
        summaryNotification();
    }

    private void taskNotification(String taskName) {
        // Read out Articles from the Shared Preferences
        Set<String> currentTasks = getSharedPreferences(NOTIFICATIONS, Context.MODE_PRIVATE).getStringSet(TASKS, null);
        if (currentTasks != null) currentTasks.add(taskName);
        else {
            currentTasks = new HashSet<>();
            currentTasks.add(taskName);
        }

        // Add the new Article to the SharedPreference
        SharedPreferences.Editor editor = getSharedPreferences(NOTIFICATIONS, Context.MODE_PRIVATE).edit();
        editor.putStringSet(TASKS, currentTasks);
        editor.apply();

        // Configure the Inbox-Style to display all recently added articles
        NotificationCompat.InboxStyle inboxStyle = new NotificationCompat.InboxStyle();
        inboxStyle.setBigContentTitle("Aufgaben Erinnerungen:");
        for (String task : currentTasks) inboxStyle.addLine(task);

        // Intent for onClick-Event
        Intent clickIntent = new Intent(this, HomeActivity.class);
        clickIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        PendingIntent clickPendingIntent = PendingIntent.getActivity(this, 0, clickIntent, PendingIntent.FLAG_ONE_SHOT);

        // Default Notification Sound
        Uri defaultSoundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);

        // Build the Notification
        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, CHANNEL_ID_TASKS)
                .setSmallIcon(R.drawable.ic_notification)
                .setContentTitle(taskName)
                .setContentText("???")
                .setAutoCancel(true)
                .setSound(defaultSoundUri)
                .setContentIntent(clickPendingIntent)
                .setGroup(CHANNEL_ID_ALL);
        if (currentTasks.size() > 1) builder.setStyle(inboxStyle);


        NotificationManager notificationManager =
                (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);

        // Since android Oreo notification channel is needed.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(CHANNEL_ID_TASKS, "Aufgaben",
                    NotificationManager.IMPORTANCE_DEFAULT);
            notificationManager.createNotificationChannel(channel);
        }

        notificationManager.notify(CHANNEL_TASKS, builder.build());
        summaryNotification();
    }

    private void taskSkippedNotification(String taskName, String fromUser) {
        // Intent for onClick-Event
        Intent clickIntent = new Intent(this, HomeActivity.class);
        clickIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        PendingIntent clickPendingIntent = PendingIntent.getActivity(this, 0, clickIntent, PendingIntent.FLAG_ONE_SHOT);

        // Default Notification Sound
        Uri defaultSoundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);

        // Build the Notification
        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, CHANNEL_ID_TASKS)
                .setSmallIcon(R.drawable.ic_notification)
                .setContentTitle(taskName)
                .setContentText("hat " + fromUser + " an dich weitergegeben.")
                .setAutoCancel(true)
                .setSound(defaultSoundUri)
                .setContentIntent(clickPendingIntent)
                .setGroup(CHANNEL_ID_ALL);

        NotificationManager notificationManager =
                (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);

        // Since android Oreo notification channel is needed.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(CHANNEL_ID_TASKS, "Aufgaben",
                    NotificationManager.IMPORTANCE_DEFAULT);
            notificationManager.createNotificationChannel(channel);
        }

        notificationManager.notify((int) System.currentTimeMillis(), builder.build());
        summaryNotification();
    }

    private void paymentNotification(String paymentTitle) {
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
        inboxStyle.setBigContentTitle("Kürzlich hinzugefügte Finanzeinträge");
        for (String payment : currentPayments) inboxStyle.addLine(payment);

        // Intent for onClick-Event
        Intent clickIntent = new Intent(this, HomeActivity.class);
        clickIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        PendingIntent clickPendingIntent = PendingIntent.getActivity(this, 0, clickIntent, PendingIntent.FLAG_ONE_SHOT);

        // Default Notification Sound
        Uri defaultSoundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);

        // Build the Notification
        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, CHANNEL_ID_TASKS)
                .setSmallIcon(R.drawable.ic_notification)
                .setContentTitle("Neuer Finanzeintrag")
                .setContentText(paymentTitle)
                .setAutoCancel(true)
                .setSound(defaultSoundUri)
                .setContentIntent(clickPendingIntent)
                .setGroup(CHANNEL_ID_ALL);
        if (currentPayments.size() > 1) builder.setStyle(inboxStyle);


        NotificationManager notificationManager =
                (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);

        // Since android Oreo notification channel is needed.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(CHANNEL_ID_FINANCES, "Finanzen",
                    NotificationManager.IMPORTANCE_DEFAULT);
            notificationManager.createNotificationChannel(channel);
        }

        notificationManager.notify(CHANNEL_FINANCES, builder.build());
        summaryNotification();
    }

    private void notesNotification(String username, String title, String description) {
        // Intent for onClick-Event
        Intent clickIntent = new Intent(this, HomeActivity.class);
        clickIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        PendingIntent clickPendingIntent = PendingIntent.getActivity(this, 0, clickIntent, PendingIntent.FLAG_ONE_SHOT);

        // Default Notification Sound
        Uri defaultSoundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);

        // Big Notification - Style
        NotificationCompat.BigTextStyle style = new NotificationCompat.BigTextStyle();
        if (!TextUtils.isEmpty(title)) {
            style.setBigContentTitle(title);
            style.setSummaryText(username);
        } else style.setBigContentTitle(username);
        style.bigText(description);

        // Build the Notification
        if (TextUtils.isEmpty(title)) title = description;
        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, CHANNEL_ID_NOTES)
                .setSmallIcon(R.drawable.ic_notification)
                .setContentTitle(username)
                .setContentText(title)
                .setAutoCancel(true)
                .setSound(defaultSoundUri)
                .setContentIntent(clickPendingIntent)
                .setGroup(CHANNEL_ID_ALL);
        if (!TextUtils.isEmpty(description)) builder.setStyle(style);

        NotificationManager notificationManager =
                (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);

        // Since android Oreo notification channel is needed.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(CHANNEL_ID_NOTES, "Pinnwand",
                    NotificationManager.IMPORTANCE_DEFAULT);
            notificationManager.createNotificationChannel(channel);
        }

        notificationManager.notify((int) System.currentTimeMillis(), builder.build());
        summaryNotification();
    }

    private void summaryNotification() {
        Notification summaryNotification =
                new NotificationCompat.Builder(this, CHANNEL_ID_ALL)
                        .setContentTitle("Headquarter")
                        //set content text to support devices running API level < 24
                        .setContentText("???")
                        .setSmallIcon(R.drawable.ic_notification)
                        //specify which group this notification belongs to
                        .setGroup(CHANNEL_ID_ALL)
                        //set this notification as the summary for the group
                        .setGroupSummary(true)
                        .build();

        NotificationManagerCompat notificationManager = NotificationManagerCompat.from(this);
        notificationManager.notify(SUMMARY_ID, summaryNotification);
    }

}
