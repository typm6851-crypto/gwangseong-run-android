package com.gwangseong.run;

import android.app.*;
import android.content.*;
import android.hardware.*;
import android.os.*;

public class RunService extends Service implements SensorEventListener {
    public static final String ACTION_UPDATE = "com.gwangseong.run.UPDATE";
    public static final String ACTION_START = "com.gwangseong.run.START";
    public static final String ACTION_STOP = "com.gwangseong.run.STOP";
    private static final String CHANNEL = "gwangseong_run_tracking";
    private SensorManager sensors;
    private Sensor stepDetector;
    private Sensor accelerometer;
    private int steps;
    private long startedAt;
    private float baseline = 9.8f;
    private long lastStepAt;
    private Handler handler;

    @Override public void onCreate() {
        super.onCreate();
        sensors = (SensorManager) getSystemService(SENSOR_SERVICE);
        stepDetector = sensors.getDefaultSensor(Sensor.TYPE_STEP_DETECTOR);
        accelerometer = sensors.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        handler = new Handler(Looper.getMainLooper());
        createChannel();
    }

    @Override public int onStartCommand(Intent intent, int flags, int startId) {
        String action = intent == null ? ACTION_START : intent.getAction();
        if (ACTION_STOP.equals(action)) { finishRun(); return START_NOT_STICKY; }
        steps = 0;
        startedAt = System.currentTimeMillis();
        getSharedPreferences("run", MODE_PRIVATE).edit().putBoolean("running", true).putInt("steps", 0).putLong("startedAt", startedAt).apply();
        startForeground(1001, notification("측정 시작 · 0걸음"));
        if (stepDetector != null) sensors.registerListener(this, stepDetector, SensorManager.SENSOR_DELAY_NORMAL);
        else if (accelerometer != null) sensors.registerListener(this, accelerometer, SensorManager.SENSOR_DELAY_GAME);
        handler.post(ticker);
        return START_STICKY;
    }

    private final Runnable ticker = new Runnable() {
        @Override public void run() { broadcast(); handler.postDelayed(this, 1000); }
    };

    @Override public void onSensorChanged(SensorEvent event) {
        if (event.sensor.getType() == Sensor.TYPE_STEP_DETECTOR) steps += Math.max(1, Math.round(event.values[0]));
        else {
            float x=event.values[0], y=event.values[1], z=event.values[2];
            float magnitude=(float)Math.sqrt(x*x+y*y+z*z);
            baseline=baseline*0.92f+magnitude*0.08f;
            long now=System.currentTimeMillis();
            if(magnitude-baseline>1.35f && now-lastStepAt>320){ steps++; lastStepAt=now; }
        }
        getSharedPreferences("run", MODE_PRIVATE).edit().putInt("steps", steps).apply();
    }

    private void broadcast() {
        long seconds = Math.max(0, (System.currentTimeMillis()-startedAt)/1000);
        Intent update = new Intent(ACTION_UPDATE).setPackage(getPackageName());
        update.putExtra("steps", steps); update.putExtra("seconds", seconds);
        sendBroadcast(update);
        NotificationManager nm=(NotificationManager)getSystemService(NOTIFICATION_SERVICE);
        nm.notify(1001, notification(String.format("%,d걸음 · %.2fkm", steps, steps*0.0007)));
    }

    private void finishRun() {
        long seconds=Math.max(0,(System.currentTimeMillis()-startedAt)/1000);
        getSharedPreferences("run", MODE_PRIVATE).edit().putBoolean("running",false).putInt("lastSteps",steps).putLong("lastSeconds",seconds).apply();
        broadcast();
        handler.removeCallbacks(ticker); sensors.unregisterListener(this); stopForeground(STOP_FOREGROUND_REMOVE); stopSelf();
    }

    private Notification notification(String text) {
        Intent open=new Intent(this,MainActivity.class);
        PendingIntent pi=PendingIntent.getActivity(this,0,open,PendingIntent.FLAG_UPDATE_CURRENT|PendingIntent.FLAG_IMMUTABLE);
        return new Notification.Builder(this,CHANNEL).setSmallIcon(R.drawable.ic_launcher).setContentTitle("광성런 측정 중").setContentText(text).setContentIntent(pi).setOngoing(true).setOnlyAlertOnce(true).build();
    }
    private void createChannel(){ if(Build.VERSION.SDK_INT>=26){NotificationChannel c=new NotificationChannel(CHANNEL,"광성런 운동 측정",NotificationManager.IMPORTANCE_LOW);c.setDescription("화면이 꺼진 동안에도 러닝을 측정합니다.");getSystemService(NotificationManager.class).createNotificationChannel(c);} }
    @Override public void onAccuracyChanged(Sensor sensor,int accuracy){}
    @Override public android.os.IBinder onBind(Intent intent){return null;}
    @Override public void onDestroy(){handler.removeCallbacks(ticker);sensors.unregisterListener(this);super.onDestroy();}
}
