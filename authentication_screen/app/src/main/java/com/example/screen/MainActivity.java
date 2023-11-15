package com.example.screen;



import androidx.appcompat.app.AppCompatActivity;


import android.annotation.SuppressLint;
import android.os.Bundle;
import android.util.Log;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.widget.FrameLayout;
import android.widget.TextView;

import java.io.File;
import java.time.Instant;
import java.util.Arrays;

public class MainActivity extends AppCompatActivity {
    private static final String TAG = "Activity";
    public static String SDFile;
    public static MyDraw myDraw;
    float[] positionRegY = new float[5]; //save the X,Y for register
    float[] positionRegX = new float[5]; //save the X,Y for register
    float tempX = 0;
    float tempY = 0;
    float[] pressure = new float[5];
    float[] positionX = new float[5];
    float[] positionY = new float[5];
    public static int count;
    private int countReg = 0;
    public boolean flagPowerOn;
    boolean flagThree = false;
    public boolean[] pointerID = new boolean[5];
    private TextView textView;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        textView = findViewById(R.id.textView);
        flagPowerOn = false;
        count = 0;
        SDFile = MainActivity.this.getFilesDir().getPath();
        Log.i(TAG, "onCreate: " + SDFile);
        File file = new File(SDFile + "/Data.txt");
        boolean isDeleted = file.delete();
        if (!isDeleted) {
            Log.e(TAG, "onCreate: Delete file failed.");
        }
        new SDFileUtils(SDFile + "/Data.txt").writeToSDFile("####" + "\n");
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        //屏蔽音量升高按键，作为启动认证的要素
        //与电源键靠近且易于实现
        if (keyCode == KeyEvent.KEYCODE_VOLUME_UP) {
            //Log.i(TAG, "onKeyDown: " + count);
            flagPowerOn = true;
            if (!flagThree) {
                textView.setText("您在触点少于3个时开启了采集，本次采集无效，请重新开始采集");
                flagPowerOn = false;
            }
//            getSystemService(VIBRATOR_SERVICE);
            return true;
        } else if (keyCode == KeyEvent.KEYCODE_BACK) {
            return true;
        }
        return super.onKeyDown(keyCode, event);
    }

    @SuppressLint("DefaultLocale")
    @Override
    public boolean dispatchTouchEvent(MotionEvent event) {
        int pointerIndex = event.getActionIndex();
        int pointerId = event.getPointerId(pointerIndex);

        switch (event.getActionMasked()) {
            case MotionEvent.ACTION_POINTER_UP:
                pointerID[pointerId] = false;
                //Log.w(TAG, "dispatchTouchEvent: " + "up\t" + pointerId);

                break;
            case MotionEvent.ACTION_POINTER_DOWN:
                pointerID[pointerId] = true;
                //Log.w(TAG, "dispatchTouchEvent: " + "down\t" + pointerId);

                break;
            case MotionEvent.ACTION_MOVE://MOVE事件仅能由index为0的指针触发

                int count_PointerID = 0;
                for (int i = 0; i < pointerID.length; i++) {
                    boolean isPointAlive = pointerID[i];
                    if (isPointAlive) {
                        int tempIndex = event.findPointerIndex(i);
                        pressure[i] = event.getPressure(tempIndex);
                        positionX[i] = event.getX(tempIndex);
                        positionY[i] = event.getY(tempIndex);
                        count_PointerID++;
                    } else {

                        pressure[i] = 0.0f;
                        positionX[i] = 0.0f;
                        positionY[i] = 0.0f;
                        //Log.i(TAG, "dispatchTouchEvent: \t" + i +"dead");
                    }
                }
                if (!flagPowerOn && count_PointerID < 3) {
                    textView.setText("您目前检测到" + count_PointerID + "个触点，请调整手指位置再开始采集");
                    flagThree = false;
                } else if (!flagPowerOn && count_PointerID == 3) {
                    textView.setText("All the point can be sampled.\n Begin your collection.");
                    flagThree = true;
                }

                if (flagPowerOn) {
                    StringBuilder str = new StringBuilder();
                    for (int i = 0; i < 5; i++) {
                        str.append(pressure[i]).append(" ");
                    }

                    for (int i = 0; i < 5; i++) {
                        str.append(positionX[i]).append(" ").append(positionY[i]).append(" ");
                    }

                    Instant now = Instant.now();

                    str.append(now.toEpochMilli()).append("\n");

                    Log.d(TAG, "dispatchTouchEvent: " + str.toString());
                    // textView.setText(str.toString());

                    //对前五次的每次采集过程的前十个数据中的中指位置求平均并记录
                    if (countReg < 20 && count < 5) {
                        countReg++;
                        float[] temp = new float[5];
                        System.arraycopy(positionY, 0, temp, 0, 5);
                        Arrays.sort(temp);
                        int tempindex = 0;

                        for (int i = 0; i < 5; i++) {
                            if (positionY[i] == temp[3]) {
                                tempindex = i;
                                break;
                            }
                        }

                        tempY += positionY[tempindex];
                        tempX += positionX[tempindex];

                    } else if (countReg == 20) {
                        countReg++;
                        positionRegX[count] = tempX / 20;
                        positionRegY[count] = tempY / 20;
                        tempX = 0;
                        tempY = 0;
                    }

                    new SDFileUtils(SDFile + "/Data.txt").writeToSDFile(str.toString());
                }
                break;
            case MotionEvent.ACTION_DOWN:
                pointerID[pointerId] = true;
                //Log.w(TAG, "dispatchTouchEvent: " + "down\t" + pointerId);
                break;
            case MotionEvent.ACTION_UP:
                pointerID[pointerId] = false;
                flagThree = false;
                if (flagPowerOn) {
                    count++;
                    Log.w(TAG, "dispatchTouchEvent: " + "You behavior have been collected:\t" + count);
                    textView.setText("目前一共采集了\t" + count + "\t次，一共需要采集 50 次");
                    new SDFileUtils(SDFile + "/Data.txt").writeToSDFile("####\n");
                    flagPowerOn = false;
                    countReg = 0;

                    if (count == 5) {
                        float x = 0;
                        float y = 0;
                        for (int i = 0; i < 5; i++) {
                            x += positionRegX[i];
                            y += positionRegY[i];
                        }
                        Log.d(TAG, "dispatchTouchEvent: " + x + "###" + y);
                        myDraw = new MyDraw(this, (int) x / 5, (int) y / 5);
                        FrameLayout.LayoutParams params = new FrameLayout.LayoutParams
                                (FrameLayout.LayoutParams.WRAP_CONTENT, FrameLayout.LayoutParams.WRAP_CONTENT);
                        addContentView(myDraw, params);
                        textView.setText(R.string.tip_test);
                    }
                } else {
                    textView.setText("目前没有触点，请将手指放置在屏幕上");
                }

                //Log.w(TAG, "dispatchTouchEvent: " + "up\t" + pointerId);

                break;
        }
        return super.dispatchTouchEvent(event);

    }

    @Override
    protected void onPause() {
        super.onPause();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }


}