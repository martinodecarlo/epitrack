package com.martinodecarlo.epitrack;

import android.Manifest;
import android.app.Activity;
import android.app.PendingIntent;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.content.Context;
import android.content.Intent;
import android.content.IntentSender;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.common.api.Status;
import com.google.android.gms.iid.InstanceID;
import com.google.android.gms.nearby.Nearby;
import com.google.android.gms.nearby.messages.Message;
import com.google.android.gms.nearby.messages.MessageListener;
import com.google.android.gms.nearby.messages.MessagesOptions;
import com.google.android.gms.nearby.messages.NearbyPermissions;
import com.google.android.gms.nearby.messages.PublishOptions;
import com.google.android.gms.nearby.messages.Strategy;
import com.google.android.gms.nearby.messages.SubscribeOptions;
import com.google.firebase.FirebaseApp;
import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import static android.content.ContentValues.TAG;

public class MainActivity extends Activity implements GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener {


    Message mMessage;
    private boolean mResolvingError = false;
    private static final int REQUEST_RESOLVE_ERROR = 1001;

    TextView receivedMessage;
    Button btnSend;
    List<String> infectedList;
    String [] contactList;
    boolean amIatRisk=false;
    int counter = 0;
    static String fileContacts= "logContacts.txt";
    static String lastDate="lastDate.txt";
    GoogleApiClient mGoogleApiClient;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);


       String  myUserUID = InstanceID.getInstance(getApplicationContext()).getId();

        receivedMessage = (TextView) findViewById(R.id.receivedMessage);
        final Intent intentForService=new Intent(MainActivity.this, BackgroundTrackingService.class);

        ImageButton register=(ImageButton) findViewById(R.id.registerBackground);
        register.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                startService(intentForService);

                Log.d("QUI","SONO QUI");
            }
        });


        Button unregister=(Button) findViewById(R.id.unregisterBackground);
        unregister.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                stopService(intentForService);
            }
        });



        Button checkList=(Button) findViewById(R.id.checkList);
        checkList.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                // Read contacts from my contactsList
                contactList=Utils.readContacts(getApplicationContext()).split(";");

                FirebaseDatabase database = FirebaseDatabase.getInstance();
                DatabaseReference myRef = database.getReference("Contagiati");
                // Read from the database
                myRef.orderByKey().startAt(Utils.getLastDate(getApplicationContext())).addListenerForSingleValueEvent(
                        new ValueEventListener() {
                            @Override
                            public void onDataChange(DataSnapshot dataSnapshot) {
                                //Get map of users in datasnapshot
                                collectCodeAndDate(dataSnapshot);
                                /*String value = dataSnapshot.getKey();
                                Contagiato contagiato=dataSnapshot.getValue(Contagiato.class);
                                for (int i=0; i<contactList.length;i++) {
                                    if (value.equals(contactList[i])) {
                                        amIatRisk=true;
                                        break;
                                    }
                                }*/



                            }

                            @Override
                            public void onCancelled(DatabaseError databaseError) {
                                //handle databaseError
                            }
                        });





               // DatabaseReference usersRef = myRef.child("Utenti");
              //  usersRef.child("alanisawesome").setValue("{June 23, 1912", "Alan Turing"));
                //new checkList(getApplicationContext()).execute();
            }
        });






        btnSend=(Button) findViewById((R.id.send));
        btnSend.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                FirebaseDatabase database = FirebaseDatabase.getInstance();
                DatabaseReference myRef = database.getReference("Contagiati");
                DatabaseReference newChildRef = myRef.push();
                String key = newChildRef.getKey();
                myRef.child(key).setValue(Utils.getUniqueKey(getApplicationContext()));

            }
        });



        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .addApi(Nearby.MESSAGES_API)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .build();


    }






    private void collectCodeAndDate(DataSnapshot dataSnapshot) {

        ArrayList<String> codes = new ArrayList<>();
        ArrayList<String> dates = new ArrayList<>();
        String maxDate="";


        //iterate through each user, ignoring their UID
        for (DataSnapshot data: dataSnapshot.getChildren()){
            //Get user map
            //Map singleUser = (Map) entry.getValue();
            //Get phone field and append to list
            dates.add(data.getKey());
            codes.add((String) data.getValue());
            Log.d("Data",data.getKey());
       //  if(maxDate.compareTo((String) data.getKey())>0) {maxDate=(String) data.getKey();}
            for (int i=0; i<contactList.length;i++) {
                if (data.getValue().equals(contactList[i])) {
                    amIatRisk=true;
                    receivedMessage.setText("SEI A RISCHIO!! Contatta il 1300 e comunica questo codice: "+Utils.getUniqueKey(getApplicationContext()));
                    receivedMessage.setTextColor(getResources().getColor(R.color.colorAccent));
                    break;
                }
            }
        }
        Log.d("SONO A RISCHIO?",""+amIatRisk);

        if(dates.size()>0) {
            maxDate = dates.get(dates.size() - 1);
        }
         if(maxDate.compareTo(Utils.getLastDate(getApplicationContext()))>0) {
             Utils.saveLastDate(maxDate, getApplicationContext());
         }


       // System.out.println(phoneNumbers.toString());
    }



    @Override
    protected void onStart() {
        super.onStart();
        mGoogleApiClient.connect();
    }


    @Override
    public void onDestroy() {


        super.onDestroy();
    }

    @Override
    public void onConnected(Bundle bundle) {
        Log.d("TAG","Google API Connected");
      /*  Nearby.Messages.getPermissionStatus(mGoogleApiClient).setResultCallback(
                new ErrorCheckingCallback("getPermissionStatus", new Runnable() {
                    @Override
                    public void run() {

                    }
                })
        );*/
    }




    @Override
    public void onConnectionFailed(ConnectionResult connectionResult) {
        if (mResolvingError) {
            // Already attempting to resolve an error.
            return;
        } else if (connectionResult.hasResolution()) {
            try {
                mResolvingError = true;
                connectionResult.startResolutionForResult(this, REQUEST_RESOLVE_ERROR);
            } catch (IntentSender.SendIntentException e) {
                // There was an error with the resolution intent. Try again.
                mGoogleApiClient.connect();
            }
        } else {
            // Show dialog using GoogleApiAvailability.getErrorDialog()
//            showErrorDialog(connectionResult.getErrorCode());
            mResolvingError = true;
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == REQUEST_RESOLVE_ERROR) {
            mResolvingError = false;
            if (resultCode == RESULT_OK) {
                // Make sure the app is not already connected or attempting to connect
                if (!mGoogleApiClient.isConnecting() &&
                        !mGoogleApiClient.isConnected()) {
                    mGoogleApiClient.connect();
                }
            }
        }
    }


    @Override
    public void onConnectionSuspended(int i) {

    }




    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           String[] permissions, int[] grantResults) {

        if (requestCode == Utils.MULTIPLE_PERMISSIONS) {
            if (grantResults.length > 0) {
                Log.d("TAG", "3");

                boolean internetPermission = grantResults[0] == PackageManager.PERMISSION_GRANTED;
                Log.d("TAG", String.valueOf(internetPermission));
                boolean accesFineLocationPermission = grantResults[1] == PackageManager.PERMISSION_GRANTED;
                Log.d("TAG", String.valueOf(accesFineLocationPermission));
                boolean bluetoothPermission = grantResults[2] == PackageManager.PERMISSION_GRANTED;
            } else {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    requestPermissions(
                            new String[]{Manifest.permission
                                    .INTERNET, Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.BLUETOOTH},
                            Utils.MULTIPLE_PERMISSIONS);
                }

        }
    }

}






}


