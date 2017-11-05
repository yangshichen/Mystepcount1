package com.example.jibuqi;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Build;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import java.util.Timer;
import java.util.TimerTask;

public class MainActivity extends AppCompatActivity  implements SensorEventListener {
    private static final int WRITE_PHONE_STATE =100;
    private float gravityOld=0;
    private SensorManager sm;
    private float lowX=0,lowY=0,lowZ=0;
    private Button bt0,bt1;
    private final float FILTERING_VALAUE=0.1f;
    private boolean lastStatus=false,doWrite=false,isDirectionUp=false;
    private EditText ET0,ET1,ET2;
    private long timeOfLastPeak=0,timeOfThisPeak=0,timeOfNow=0;
    public float peakOfWave=0,valleyOfWave=0;
    final float initialValue = (float) 1.7;
    private int continueUpCount=0,continueUpFormerCount=0;
    float ThreadValue = (float) 2.0;
    float minValue = 11f;
    float maxValue = 19.6f;
    private final String TAG = "StepInAcceleration";
    public  int CURRENT_STEP=0,tempCount=0,valueNum=5;
    private TimeCount time;
    public  float[] tempValue = new float[valueNum];
    private int CountTimeState = 0;
    public static int TEMP_STEP = 0;
    public int lastStep = -1;
    //用x、y、z轴三个维度算出的平均值
    public static float average = 0;
    private Timer timer;
    // 倒计时3.5秒，3.5秒内不会显示计步，用于屏蔽细微波动
    private long duration = 3500;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main_layout);
        ET0 = (EditText) findViewById(R.id.editText);
        ET1 = (EditText) findViewById(R.id.editText2);
        ET2 = (EditText) findViewById(R.id.editText3);
        bt0 = (Button) findViewById(R.id.button1);
        bt1 = (Button) findViewById(R.id.button2);
        sm = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        if (Build.VERSION.SDK_INT >= 23) {
            showContacts();
        }
        bt0.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                    doWrite=true;
            }
        });
        bt1.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                doWrite=false;
            }
        });

    }


    @Override
    protected void onResume()
    {
        super.onResume();
        //为系统的加速度传感器注册监听器
        sm.registerListener(this, sm.getDefaultSensor(Sensor.TYPE_ACCELEROMETER),
                SensorManager.SENSOR_DELAY_NORMAL);
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        //得到线性加速度的值
        String message = new String();
        Context context = getApplicationContext();
        if(event.sensor.getType()== Sensor.TYPE_ACCELEROMETER){
            float X=event.values[0];
            float Y=event.values[1];
            float Z=event.values[2];
            lowX = X*FILTERING_VALAUE+lowX*(1.0f-FILTERING_VALAUE);
            lowY= Y*FILTERING_VALAUE+lowY*(1.0f-FILTERING_VALAUE);
            lowZ= Z*FILTERING_VALAUE+lowZ*(1.0f-FILTERING_VALAUE);

            float highX = X-lowX;
            float highY = Y-lowY;
            float highZ = Z-lowZ;

            average = (float)Math.sqrt(highX*highX + highY*highY + highZ*highZ);
//            DecimalFormat df = new DecimalFormat("#,##0.000");

//            message=df.format(highX)+" ";
//            message+=df.format(highY)+" ";
//            message+=df.format(highZ)+" ";
//            message+=df.format(average)+" ";
            detectorNewStep(average);
//            if (doWrite){
//                writefile(message,context);
//            }
//            ET0.setText(String.valueOf(X));
//            ET1.setText(String.valueOf(Y));
//            ET2.setText(String.valueOf(Z));
        }
    }

    private void detectorNewStep(float values) {
        if(gravityOld==0){
            gravityOld=values;
        }else{
            if(DetectorPeak(values,gravityOld)){
                timeOfLastPeak=timeOfThisPeak;
                timeOfNow=System.currentTimeMillis();

                if(timeOfNow-timeOfLastPeak>=200&&(peakOfWave-valleyOfWave>=ThreadValue)
                        &&(timeOfNow-timeOfLastPeak)<=2000){
                    timeOfThisPeak=timeOfNow;
                    //更新界面的处理，不涉及算法
                    preStep();
                }
                if(timeOfNow-timeOfLastPeak>=200
                        &&(peakOfWave-valleyOfWave>=initialValue)){
                    timeOfThisPeak=timeOfNow;
                    ThreadValue=Peak_Valley_Thread(peakOfWave-valleyOfWave);
                }
            }
        }
        gravityOld=values;
    }

    public boolean DetectorPeak(float newValue,float oldValue){
        lastStatus=isDirectionUp;
        if(newValue>=oldValue){
            isDirectionUp=true;
            continueUpCount++;
        }else{
            continueUpFormerCount=continueUpCount;
            continueUpCount=0;
            isDirectionUp=false;
        }
        if(!isDirectionUp&&lastStatus&&(continueUpFormerCount>=2&&(oldValue>=minValue&&oldValue<maxValue))){
            //满足上面波峰的四个条件，此时为波峰状态
            peakOfWave=oldValue;
            return true;
        }else if(!lastStatus&&isDirectionUp){
            //满足波谷条件，此时为波谷状态
            valleyOfWave=oldValue;
            return false;
        }else{
            return false;
        }
    }

    private void preStep(){
        if(CountTimeState==0){
            //开启计时器(倒计时3.5秒,倒计时时间间隔为0.7秒)  是在3.5秒内每0.7面去监测一次。
            time=new TimeCount(duration,700);
            time.start();
            CountTimeState=1;  //计时中
            Log.v(TAG, "开启计时器");
        }else if(CountTimeState==1){
            TEMP_STEP++;          //如果传感器测得的数据满足走一步的条件则步数加1
            Log.v(TAG,"计步中 TEMP_STEP:"+TEMP_STEP);
        }else if(CountTimeState==2){
            CURRENT_STEP++;
//            if(onSensorChangeListener!=null){
//                //在这里调用onChange()  因此在StepService中会不断更新状态栏的步数
//                onSensorChangeListener.onChange();
//            }
        }
    }

    public float Peak_Valley_Thread(float value){
        float tempThread=ThreadValue;
        if(tempCount<valueNum){
            tempValue[tempCount]=value;
            tempCount++;
        }else{
            //此时tempCount=valueNum=5
            tempThread=averageValue(tempValue,valueNum);
            for(int i=1;i<valueNum;i++){
                tempValue[i-1]=tempValue[i];
            }
            tempValue[valueNum-1]=value;
        }
        return tempThread;
    }

    /**
     * 梯度化阈值
     * 1.计算数组的均值
     * 2.通过均值将阈值梯度化在一个范围里
     */
    public float averageValue(float value[],int n){
        float ave=0;
        for(int i=0;i<n;i++){
            ave+=value[i];
        }
        ave=ave/valueNum;  //计算数组均值
        if(ave>=8){
            Log.v(TAG,"超过8");
            ave=(float)4.3;
        }else if(ave>=7&&ave<8){
            Log.v(TAG,"7-8");
            ave=(float)3.3;
        }else if(ave>=4&&ave<7){
            Log.v(TAG,"4-7");
            ave=(float)2.3;
        }else if(ave>=3&&ave<4){
            Log.v(TAG,"3-4");
            ave=(float)2.0;
        }else{
            Log.v(TAG,"else (ave<3)");
            ave=(float)1.7;
        }
        return ave;
    }
    class TimeCount extends CountDownTimer {

        public TimeCount(long millisInFuture, long countDownInterval) {
            super(millisInFuture, countDownInterval);
        }

        @Override
        public void onFinish() {
            // 如果计时器正常结束，则开始计步
            time.cancel();
            CURRENT_STEP += TEMP_STEP;
            Log.v(TAG, "计时正常结束");

            timer = new Timer(true);
            TimerTask task = new TimerTask() {
                public void run() {
                    if (lastStep == CURRENT_STEP) {
                        timer.cancel();
                        CountTimeState = 0;
                        lastStep = -1;
                        TEMP_STEP = 0;
                        Log.v(TAG, "停止计步：" + CURRENT_STEP);
                    } else {
                        lastStep = CURRENT_STEP;
                    }
                }
            };
            timer.schedule(task, 0, 2000);
            CountTimeState = 2;
        }

        @Override
        public void onTick(long millisUntilFinished) {
            if (lastStep == TEMP_STEP) {
                Log.v(TAG, "onTick 计时停止:" + TEMP_STEP);
                time.cancel();
                CountTimeState = 0;
                lastStep = -1;
                TEMP_STEP = 0;
            } else {
                lastStep = TEMP_STEP;
            }
        }

    }
//    private  void writefile(String a,Context context){
//        try{
//            String cachePath = null;
//            if(Environment.MEDIA_MOUNTED.equals(Environment.getExternalStorageState())||!Environment.isExternalStorageRemovable()){
//                cachePath = context.getExternalCacheDir().getPath();
//            }
//            else cachePath = context.getCacheDir().getPath();
//            File file = new File(cachePath,"acc.txt");
//            if(!file.exists()){
//                file.createNewFile();
//            }
//            RandomAccessFile randomAccessFile = new RandomAccessFile(file,"rw");
//            long fileLength = randomAccessFile.length();
//            randomAccessFile.seek(fileLength);
//            randomAccessFile.writeBytes(a);
//            randomAccessFile.close();
//        }catch (IOException e){
//            e.printStackTrace();
//        }
//
//    }
    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {

    }

    public void showContacts(){
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)
                != PackageManager.PERMISSION_GRANTED
                || ActivityCompat.checkSelfPermission(this, Manifest.permission.BODY_SENSORS)
                != PackageManager.PERMISSION_GRANTED ) {
            Toast.makeText(getApplicationContext(),"没有权限,请手动开启定位权限",Toast.LENGTH_SHORT).show();
            // 申请一个（或多个）权限，并提供用于回调返回的获取码（用户定义）
            ActivityCompat.requestPermissions(MainActivity.this,new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE,Manifest.permission.BODY_SENSORS}, WRITE_PHONE_STATE);
        }
    }
    //Android6.0申请权限的回调方法
    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        switch (requestCode) {
            // requestCode即所声明的权限获取码，在checkSelfPermission时传入
            case WRITE_PHONE_STATE:
                if (grantResults[0] != PackageManager.PERMISSION_GRANTED) {
                    // 没有获取到权限，做特殊处理
                    Toast.makeText(getApplicationContext(), "获取写入权限失败，请手动开启", Toast.LENGTH_SHORT).show();
                }
                break;
            default:
                break;
        }
    }

}
