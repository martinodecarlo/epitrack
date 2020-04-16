package com.martinodecarlo.epitrack;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.Bundle;
import android.os.IBinder;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.nearby.Nearby;
import com.google.android.gms.nearby.messages.Message;
import com.google.android.gms.nearby.messages.MessageListener;
import com.google.android.gms.nearby.messages.MessagesOptions;
import com.google.android.gms.nearby.messages.NearbyPermissions;
import com.google.android.gms.nearby.messages.PublishOptions;
import com.google.android.gms.nearby.messages.Strategy;
import com.google.android.gms.nearby.messages.SubscribeOptions;

import static com.google.android.gms.nearby.messages.Strategy.TTL_SECONDS_DEFAULT;

public class BackgroundTrackingService extends Service implements GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener {
    private final String TAG = "MessageService";
    public static final String CHANNEL_ID = "ForegroundServiceChannel";
    private MessageListener messageListener;
    private Message myMessage;
    private String myUniqueID;

    GoogleApiClient mGoogleApiClient;

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onCreate() {
        super.onCreate();

        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .addApi(Nearby.MESSAGES_API, new MessagesOptions.Builder()
                        .setPermissions(NearbyPermissions.BLE)
                        .build())
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .build();

        Log.d("QUI","SONO QUI");
        SharedPreferences prefs = this.getSharedPreferences(getString(R.string.prefName), Context.MODE_PRIVATE);
        myUniqueID = prefs.getString(getString(R.string.UID), Utils.getUniqueKey(this));

        myMessage = new Message(myUniqueID.getBytes());
        messageListener = new MessageListener() {
            @Override
            public void onFound(Message message) {
                String metUserUID = new String(message.getContent());
                Log.d("TAG",metUserUID);

                Utils.addContact(metUserUID,BackgroundTrackingService.this);

            }


            @Override
            public void onLost(Message message) {
                Log.d(TAG, "Closed application");
                String metUserUID = new String(message.getContent());
                Log.d(TAG, "Lost sight of user: " + metUserUID);
                //QUI SI PUO' PRENDERE LA DURATA
            }
        };

    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {

        Toast.makeText(this, "Starting tracking", Toast.LENGTH_LONG).show();
        //mGoogleApiClient.connect();

        SubscribeOptions options = new SubscribeOptions.Builder()
                .setStrategy(Strategy.BLE_ONLY)
                .build();





        Nearby.getMessagesClient(this).publish(myMessage,publishOptions());
        Nearby.getMessagesClient(this).subscribe(messageListener,options);

        createNotificationChannel();
        Intent notificationIntent = new Intent(this, MainActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(this,
                0, notificationIntent, 0);
        Notification notification = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle("Sto registrando i tuoi contatti. Non verranno condivisi.")
                .setSmallIcon(R.drawable.ic_launcher_background)
                .setContentIntent(pendingIntent)
                .build();
        startForeground(1, notification);

        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        mGoogleApiClient.disconnect();
        Nearby.getMessagesClient(this).unpublish(myMessage);
        Nearby.getMessagesClient(this).unsubscribe(messageListener);
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel serviceChannel = new NotificationChannel(
                    CHANNEL_ID,
                    "Foreground Service Channel",
                    NotificationManager.IMPORTANCE_DEFAULT
            );
            NotificationManager manager = getSystemService(NotificationManager.class);
            manager.createNotificationChannel(serviceChannel);
        }
    }


    private PublishOptions publishOptions() {
        return new PublishOptions.Builder()
                .setStrategy(strategy())
                .build();
    }

    private Strategy strategy() {
        return new Strategy.Builder()
                .zze(2)
                .setTtlSeconds(TTL_SECONDS_DEFAULT)
                .build();
    }

    @Override
    public void onConnected(@Nullable Bundle bundle) {

    }

    @Override
    public void onConnectionSuspended(int i) {

    }

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {

    }
}