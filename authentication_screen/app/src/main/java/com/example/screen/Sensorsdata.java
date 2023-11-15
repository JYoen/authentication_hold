package com.example.biometric;

import android.app.Service;
import android.content.Intent;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.os.IBinder;
import android.os.Message;
import android.util.Log;



public class Sensorsdata extends Service implements SensorEventListener {
    private SensorManager sensorManager;
    //private SimpleRate SR= new SimpleRate();
    private String data = "default";
    private CallBack callback=null;
    private boolean running = false;
    private float[] laccValues;
    private long time;
    private int sensor_delay_normal = 5000;
    private static final String TAG = "Sensorsdata";

    /**
     * 数据预处理所需
     */
    private float[] gyro = new float[3];
    private float[] gyroMatrix = new float[9];
    private float[] gyroOrientation = new float[3];
    private float[] magnet = new float[3];
    private float[] accel = new float[3];
    private float[] accMagOrientation = new float[3];
    private float[] fusedOrientation = new float[3];     // the angles we need
    private float[] rotationMatrix = new float[9];
    public static final float EPSILON = 0.000000001f;
    private static final float NS2S = 1.0f / 1000000000.0f;
    private float timestamp;
    private boolean initState = true;
    public static final int TIME_CONSTANT = 30;    // 30ms
    public static final float FILTER_COEFFICIENT = 0.98f;


    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {

    }
    @Override
    public IBinder onBind(Intent intent) {
        Log.e(TAG, "onBind: ");
        sensorManager=(SensorManager)getSystemService(SENSOR_SERVICE);
        sensorManager.registerListener(this,
                sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER),sensor_delay_normal);

        sensorManager.registerListener(this,
                sensorManager.getDefaultSensor(Sensor.TYPE_LINEAR_ACCELERATION),sensor_delay_normal);

        sensorManager.registerListener(this,
                sensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE),sensor_delay_normal);

        sensorManager.registerListener(this,
                sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD),sensor_delay_normal);
        return new Binder();
    }

    public class Binder extends android.os.Binder{
        public void setData(String str) {
            Sensorsdata.this.data = str;
        }
        public Sensorsdata getService(){
            return Sensorsdata.this;
        }
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        Message msg  = new Message();
        Bundle b= new Bundle();
        switch(event.sensor.getType()){
            case Sensor.TYPE_ACCELEROMETER:
                accel = event.values.clone();
                calculateAccMagOrientation();
                break;

            case Sensor.TYPE_GYROSCOPE:
                gyroFunction(event);
                calculateFusedOrientation();
                synchronized (data){
                    data = fusedOrientation[0]*180/Math.PI+"  "+fusedOrientation[1]*180/Math.PI+"  "+fusedOrientation[2]*180/Math.PI;
                    b.putString("dataGyr",data);
                }
                break;

            case Sensor.TYPE_MAGNETIC_FIELD:
                magnet = event.values.clone();
                break;

            case Sensor.TYPE_LINEAR_ACCELERATION:
                laccValues = event.values.clone();
                synchronized (data){
                    data = laccValues[0]+" "+laccValues[1]+" "+laccValues[2];
                    b.putString("dataLacc",data);
                }
                break;
        }
        msg.setData(b);
        if (callback!=null)
            callback.onDataChange(msg);
    }

    @Override
    public void onCreate() {
        super.onCreate();
        initData();
        running =true;
    }

    public void initData(){
        gyroOrientation[0] = 0.0f;
        gyroOrientation[1] = 0.0f;
        gyroOrientation[2] = 0.0f;

        gyroMatrix[0] = 1.0f; gyroMatrix[1] = 0.0f; gyroMatrix[2] = 0.0f;
        gyroMatrix[3] = 0.0f; gyroMatrix[4] = 1.0f; gyroMatrix[5] = 0.0f;
        gyroMatrix[6] = 0.0f; gyroMatrix[7] = 0.0f; gyroMatrix[8] = 1.0f;
    }

    public CallBack getCallback() {
        return callback;
    }

    public void setCallback(CallBack callback) {
        this.callback = callback;
    }

    public static interface CallBack{
        void onDataChange(Message data);
    }

    public Sensorsdata() {
    }

    @Override
    public void onDestroy() {
        running = false;
        super.onDestroy();
    }

    /**
     *  methods for the sensorfusion
     */
    public void calculateAccMagOrientation() {
        if (SensorManager.getRotationMatrix(rotationMatrix, null, accel, magnet)) {
            SensorManager.getOrientation(rotationMatrix, accMagOrientation);
        }
    }

    private float[] getRotationMatrixFromOrientation(float[] o) {
        float[] xM = new float[9];
        float[] yM = new float[9];
        float[] zM = new float[9];

        float sinX = (float)Math.sin(o[1]);
        float cosX = (float)Math.cos(o[1]);
        float sinY = (float)Math.sin(o[2]);
        float cosY = (float)Math.cos(o[2]);
        float sinZ = (float)Math.sin(o[0]);
        float cosZ = (float)Math.cos(o[0]);

        // rotation about x-axis (pitch)
        xM[0] = 1.0f; xM[1] = 0.0f; xM[2] = 0.0f;
        xM[3] = 0.0f; xM[4] = cosX; xM[5] = sinX;
        xM[6] = 0.0f; xM[7] = -sinX; xM[8] = cosX;

        // rotation about y-axis (roll)
        yM[0] = cosY; yM[1] = 0.0f; yM[2] = sinY;
        yM[3] = 0.0f; yM[4] = 1.0f; yM[5] = 0.0f;
        yM[6] = -sinY; yM[7] = 0.0f; yM[8] = cosY;

        // rotation about z-axis (yaw)
        zM[0] = cosZ; zM[1] = sinZ; zM[2] = 0.0f;
        zM[3] = -sinZ; zM[4] = cosZ; zM[5] = 0.0f;
        zM[6] = 0.0f; zM[7] = 0.0f; zM[8] = 1.0f;

        // rotation order is y, x, z (roll, pitch, yaw)
        float[] resultMatrix = matrixMultiplication(xM, yM);
        resultMatrix = matrixMultiplication(zM, resultMatrix);
        return resultMatrix;
    }

    public void  gyroFunction(SensorEvent event){
        if (accMagOrientation == null)
            return;

        if (initState){
            float[] initMatrix = getRotationMatrixFromOrientation(accMagOrientation);
            float[] test  = new float[3];
            SensorManager.getOrientation(initMatrix,test);
            gyroMatrix = matrixMultiplication(gyroMatrix,initMatrix);
            initState = false;
        }

        float[] deltaVector = new float[4];

        if( timestamp != 0 ){
            final float dt = (event.timestamp-timestamp)*NS2S;
            System.arraycopy(event.values,0,gyro,0,3);
            getRotationVectorFromGyro(gyro, deltaVector, dt / 2.0f);

        }
        timestamp= event.timestamp;
        float[] deltaMatrix = new float[9];
        SensorManager.getRotationMatrixFromVector(deltaMatrix, deltaVector);

        // apply the new rotation interval on the gyroscope based rotation matrix
        gyroMatrix = matrixMultiplication(gyroMatrix, deltaMatrix);

        // get the gyroscope based orientation from the rotation matrix
        SensorManager.getOrientation(gyroMatrix, gyroOrientation);
    }


    private void getRotationVectorFromGyro(float[] gyroValues,
                                           float[] deltaRotationVector,
                                           float timeFactor)
    {
        float[] normValues = new float[3];

        // Calculate the angular speed of the sample
        float omegaMagnitude =
                (float)Math.sqrt(gyroValues[0] * gyroValues[0] +
                        gyroValues[1] * gyroValues[1] +
                        gyroValues[2] * gyroValues[2]);

        // Normalize the rotation vector if it's big enough
        if(omegaMagnitude > EPSILON) {
            normValues[0] = gyroValues[0] / omegaMagnitude;
            normValues[1] = gyroValues[1] / omegaMagnitude;
            normValues[2] = gyroValues[2] / omegaMagnitude;
        }

        // Integrate around this axis with the angular speed by the timestep and convert it into a quaternion
        float thetaOverTwo = omegaMagnitude * timeFactor;
        float sinThetaOverTwo = (float)Math.sin(thetaOverTwo);
        float cosThetaOverTwo = (float)Math.cos(thetaOverTwo);
        deltaRotationVector[0] = sinThetaOverTwo * normValues[0];
        deltaRotationVector[1] = sinThetaOverTwo * normValues[1];
        deltaRotationVector[2] = sinThetaOverTwo * normValues[2];
        deltaRotationVector[3] = cosThetaOverTwo;
    }

    private float[] matrixMultiplication(float[] A, float[] B) {
        float[] result = new float[9];

        result[0] = A[0] * B[0] + A[1] * B[3] + A[2] * B[6];
        result[1] = A[0] * B[1] + A[1] * B[4] + A[2] * B[7];
        result[2] = A[0] * B[2] + A[1] * B[5] + A[2] * B[8];

        result[3] = A[3] * B[0] + A[4] * B[3] + A[5] * B[6];
        result[4] = A[3] * B[1] + A[4] * B[4] + A[5] * B[7];
        result[5] = A[3] * B[2] + A[4] * B[5] + A[5] * B[8];

        result[6] = A[6] * B[0] + A[7] * B[3] + A[8] * B[6];
        result[7] = A[6] * B[1] + A[7] * B[4] + A[8] * B[7];
        result[8] = A[6] * B[2] + A[7] * B[5] + A[8] * B[8];

        return result;
    }

    public void calculateFusedOrientation() {

        float oneMinusCoeff = 1.0f - FILTER_COEFFICIENT;


        // check and correct angles

        // yaw
        if (gyroOrientation[0] < -0.5 * Math.PI && accMagOrientation[0] > 0.0) {
            fusedOrientation[0] = (float) (FILTER_COEFFICIENT * (gyroOrientation[0] + 2.0 * Math.PI) + oneMinusCoeff * accMagOrientation[0]);
            fusedOrientation[0] -= (fusedOrientation[0] > Math.PI) ? 2.0 * Math.PI : 0;
        }
        else if (accMagOrientation[0] < -0.5 * Math.PI && gyroOrientation[0] > 0.0) {
            fusedOrientation[0] = (float) (FILTER_COEFFICIENT * gyroOrientation[0] + oneMinusCoeff * (accMagOrientation[0] + 2.0 * Math.PI));
            fusedOrientation[0] -= (fusedOrientation[0] > Math.PI)? 2.0 * Math.PI : 0;
        }
        else {
            fusedOrientation[0] = FILTER_COEFFICIENT * gyroOrientation[0] + oneMinusCoeff * accMagOrientation[0];
        }

        // pitch
        if (gyroOrientation[1] < -0.5 * Math.PI && accMagOrientation[1] > 0.0) {
            fusedOrientation[1] = (float) (FILTER_COEFFICIENT * (gyroOrientation[1] + 2.0 * Math.PI) + oneMinusCoeff * accMagOrientation[1]);
            fusedOrientation[1] -= (fusedOrientation[1] > Math.PI) ? 2.0 * Math.PI : 0;
        }
        else if (accMagOrientation[1] < -0.5 * Math.PI && gyroOrientation[1] > 0.0) {
            fusedOrientation[1] = (float) (FILTER_COEFFICIENT * gyroOrientation[1] + oneMinusCoeff * (accMagOrientation[1] + 2.0 * Math.PI));
            fusedOrientation[1] -= (fusedOrientation[1] > Math.PI)? 2.0 * Math.PI : 0;
        }
        else {
            fusedOrientation[1] = FILTER_COEFFICIENT * gyroOrientation[1] + oneMinusCoeff * accMagOrientation[1];
        }

        // roll
        if (gyroOrientation[2] < -0.5 * Math.PI && accMagOrientation[2] > 0.0) {
            fusedOrientation[2] = (float) (FILTER_COEFFICIENT * (gyroOrientation[2] + 2.0 * Math.PI) + oneMinusCoeff * accMagOrientation[2]);
            fusedOrientation[2] -= (fusedOrientation[2] > Math.PI) ? 2.0 * Math.PI : 0;
        }
        else if (accMagOrientation[2] < -0.5 * Math.PI && gyroOrientation[2] > 0.0) {
            fusedOrientation[2] = (float) (FILTER_COEFFICIENT * gyroOrientation[2] + oneMinusCoeff * (accMagOrientation[2] + 2.0 * Math.PI));
            fusedOrientation[2] -= (fusedOrientation[2] > Math.PI)? 2.0 * Math.PI : 0;
        }
        else {
            fusedOrientation[2] = FILTER_COEFFICIENT * gyroOrientation[2] + oneMinusCoeff * accMagOrientation[2];		}
        // compensate the gyro drift
        gyroMatrix = getRotationMatrixFromOrientation(fusedOrientation);
        System.arraycopy(fusedOrientation, 0, gyroOrientation, 0, 3);
    }


}
