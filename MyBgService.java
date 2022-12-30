package com.demo.Services;

import android.Manifest;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.BitmapFactory;
import android.location.Location;
import android.os.Build;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;
import android.os.Looper;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.core.app.JobIntentService;
import androidx.core.app.NotificationCompat;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.LocationSettingsRequest;
import com.google.android.gms.location.SettingsClient;

import com.demo.R;


public class MyBgService extends JobIntentService {

    private static final String TAG = "MY_SERVICE";

    /**
     * The desired interval for location updates. Inexact. Updates may be more or less frequent.
     */
    private static final long UPDATE_INTERVAL = 1 * 60000;
    private static final long FASTEST_INTERVAL = UPDATE_INTERVAL / 2;
    private static final int SMALLEST_DISPLACEMENT = 1 * 15;
    private static final int SERVICE_NOTIFICATION_ID = 111;


    private Handler mServiceHandler;
    private NotificationManager mNotificationManager;
    private FusedLocationProviderClient mLocationClient;


    private final LocationCallback  mLocationCallback = new LocationCallback() {

        @Override
        public void onLocationResult(LocationResult locationResult) {
            super.onLocationResult(locationResult);

            Location location = locationResult.getLastLocation();
            if (location == null) {
                return;
            }
            //Write your code here to use location
        }
    };


    /**
     * Constructor of this class
     */
    public MyBgService() {
        Log.i(TAG, "Constructor Called");
    }


    /**
     * This method is ued to bind the services with the activity
     * */
    @Override
    public IBinder onBind(@NonNull Intent intent) {
        return null;
    }


    /**
     * Calling onCreate method of service class and initiate objects
     */
    @Override
    public void onCreate() {
        super.onCreate();

        HandlerThread handlerThread = new HandlerThread(TAG);
        handlerThread.start();
        mServiceHandler = new Handler(handlerThread.getLooper());
        mNotificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            CharSequence name = "DEMO";
            NotificationChannel mChannelPrev = mNotificationManager.getNotificationChannel("SERVICE_CHANNEL_ID");
            if (mChannelPrev  != null){
                mChannelPrev.setImportance(NotificationManager.IMPORTANCE_NONE);
                mChannelPrev.setShowBadge(false);
                try {
                    mNotificationManager.deleteNotificationChannel("SERVICE_CHANNEL_ID");
                }catch (Exception e){
                    e.printStackTrace();
                }
            }
            NotificationChannel mChannel = new NotificationChannel("SERVICE_CHANNEL_ID", name, NotificationManager.IMPORTANCE_DEFAULT);
            mChannel.setSound(null, null);
            mNotificationManager.createNotificationChannel(mChannel);
        }

    }


    /**
     * Calling onStart method of service class
     * @param intent: The requirement for the action is shown
     * @param flags: The actions to be taken
     * @param startId: Initial Id
     * @return Sticky Flag
     */
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.d("ClearFromRecentService", "Service Started");

        startMyOwnForeground();
        startLocationUpdates();

        return START_STICKY;
    }


    /**
     * calling onDestroy of service
     * unregister Broadcast Receivers
     * stop location service
     * stop foreground service
     */
    @Override
    public void onDestroy() {
        super.onDestroy();
        mServiceHandler.removeCallbacksAndMessages(null);
        locationStop();
    }

    @Override
    protected void onHandleWork(@NonNull Intent intent) {

    }


    /**
     * This function is used for stop location update
     */
    private void locationStop() {
        try {
            Log.e(TAG, "Location Updates are stopped");
            mLocationClient.removeLocationUpdates(mLocationCallback);
        }catch (Exception e){
            e.printStackTrace();
        }

    }


    /**
     * This function is creating notification for foreground service
     */
    private void startMyOwnForeground() {

        String title = "Demo Title";
        String message = "Demo Msg";
        String ticker = "Demo ticker";

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationCompat.Builder builder = new NotificationCompat.Builder(this, "SERVICE_CHANNEL_ID")
                    .setContentTitle(title)
                    .setContentText(message)
                    .setSound(null)
                    .setOngoing(true)
                    .setDefaults(0)
                    .setPriority(NotificationCompat.PRIORITY_LOW)
                    .setSmallIcon(R.drawable.logo)
                    .setLargeIcon(BitmapFactory.decodeResource(getResources(), R.drawable.logo_small))
                    .setTicker(ticker)
                    .setWhen(System.currentTimeMillis());

            builder.setChannelId("SERVICE_CHANNEL_ID");

            try {
                startForeground(SERVICE_NOTIFICATION_ID, builder.build());
            }catch (Exception e){
                e.printStackTrace();
            }

        }
    }

    /**
     * This function is used for start location update when
     * mGoogleApiClient is connected and app has permission to access location
     */
    private void startLocationUpdates() {
        LocationRequest locationRequest = LocationRequest.create()
                .setInterval(UPDATE_INTERVAL)
                .setFastestInterval(FASTEST_INTERVAL)
                .setPriority(LocationRequest.PRIORITY_BALANCED_POWER_ACCURACY)
                .setSmallestDisplacement(SMALLEST_DISPLACEMENT);

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED
                && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            Toast.makeText(this, "You need to enable permissions to display location !", Toast.LENGTH_SHORT).show();
        }


        LocationSettingsRequest.Builder builder = new LocationSettingsRequest.Builder();
        builder.addLocationRequest(locationRequest);
        LocationSettingsRequest locationSettingsRequest = builder.build();

        SettingsClient settingsClient = LocationServices.getSettingsClient(this);
        settingsClient.checkLocationSettings(locationSettingsRequest);
        mLocationClient.requestLocationUpdates(locationRequest, mLocationCallback, Looper.myLooper());
    }
    
}
