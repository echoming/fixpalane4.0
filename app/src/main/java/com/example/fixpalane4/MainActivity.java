package com.example.fixpalane4;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Context;
import android.content.SharedPreferences;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.text.method.ScrollingMovementMethod;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.SeekBar;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Calendar;

public class MainActivity extends AppCompatActivity {

    private static boolean serverStatus =false;  //连接状态显示
    private static boolean buttonStatus=false;  //连接状态显示
    private String IP = ""; //地址
    public static ServerSocket serverSocket = null; // 服务器类

    private Handler mhandler;    //UI更新

    private Handler TmimerHandler = new Handler();

    private StringBuilder addressBuilder = new StringBuilder();

    private class SeekBarControl{
        public boolean thresStatus=false;
        public boolean rollStatus=false;
        public boolean yawStatus=false;

        public int thresProgress=0;
        public int rollProgress=0;
        public int yawProgress=0;
    }

    private SeekBarControl seekBarControl = new SeekBarControl();

    private boolean beatflag =false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        final Button btn = (Button) findViewById(R.id.button);
        final Switch status_connect =  findViewById(R.id.switch1);
        final TextView text_msg = findViewById(R.id.textView2);
        final EditText text_ip = findViewById(R.id.editText);

        final SeekBar seekBar_Thres = findViewById(R.id.seekBarThres);
        final SeekBar seekBar_ROLL = findViewById(R.id.seekBarRoll);
        final SeekBar seekBar_YAW = findViewById(R.id.seekBarYaw);

        final TextView text_thres = findViewById(R.id.textView_Threshold);
        final TextView text_roll = findViewById(R.id.textView_ROLL);
        final TextView text_pitch = findViewById(R.id.textView_PITCH);

        text_msg.setMovementMethod(ScrollingMovementMethod.getInstance());
        status_connect.setChecked(false);

        SharedPreferences sp = getSharedPreferences("data", Context.MODE_PRIVATE);
        //直接通过键获取数据，如果通过这个键找不到，就返回第二个参数中的值
        if( sp.getString("name", null) != null){
            IP=sp.getString("name", null);
            text_ip.setText(IP);
        };

        final Runnable r = new Runnable() {
            @Override
            public void run() {
                //do something
                //每隔1s循环执行run方法
                TmimerHandler.postDelayed(this, 1000);
                beatflag=true;
            }
        };

        //使用匿名内部类
        btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(!buttonStatus){
                    //Toast.makeText(MainActivity.this, "连接中", Toast.LENGTH_SHORT).show();

                    IP=text_ip.getText().toString();
                    Toast.makeText(MainActivity.this, IP, Toast.LENGTH_SHORT).show();
                    SharedPreferences sp = getSharedPreferences("data", Context.MODE_PRIVATE);
                    //获取Editor对象
                    SharedPreferences.Editor editor = sp.edit();
                    //用Editor对象储存数据，传入键和值
                    editor.putString("name", IP);
                    //最后调用apply()方法
                    editor.apply();


                    buttonStatus=true;



                    server_setup();

                    TmimerHandler.postDelayed(r, 100);



                    status_connect.setChecked(true);
                    btn.setText("断开");


                }else{
                    Toast.makeText(MainActivity.this, "断开", Toast.LENGTH_SHORT).show();
                    status_connect.setChecked(false);
                    btn.setText("连接");
                    buttonStatus=false;
                }





            }
        });







        seekBar_Thres.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                seekBarControl.thresStatus=true;
                seekBarControl.thresProgress=seekBar_Thres.getProgress();
                text_thres.setText("Threshold:"+seekBarControl.thresProgress);
            }
            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {

            }
        });

        seekBar_ROLL.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                seekBarControl.rollStatus=true;
                seekBarControl.rollProgress=seekBar_ROLL.getProgress();
                text_roll.setText("ROLL:"+seekBarControl.rollProgress);
            }
            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {

            }
        });

        seekBar_YAW.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                seekBarControl.yawStatus=true;
                seekBarControl.yawProgress=seekBar_YAW.getProgress();
                text_pitch.setText("PITCH:"+seekBarControl.yawProgress);
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {

            }
        });



        mhandler=new Handler(){
            @Override
            public void handleMessage(Message msg) {
                super.handleMessage(msg);

                switch (msg.what) {
                    case 1:
                        //addressBuilder.append(msg.getData().getString("msg1"));
                        addressBuilder.insert(0,msg.getData().getString("msg1"));
                        text_msg.setText(addressBuilder);

                        break;

                }

            }
        };


    }




    private void server_setup() {
        new Thread(new Runnable() {

            Socket socket;

            @Override
            public void run() {
                try{


                    socket = new Socket();
                    socket.connect(new InetSocketAddress(IP, 5678), 1000);
                    serverStatus =true;
                    InputStream is = socket.getInputStream();
                    OutputStream os = socket.getOutputStream();

                    BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(os));

                    BufferedReader br;
                    String mess;
                    Message msg;
                    Bundle bundle = new Bundle();


                    while (serverStatus){

                        if(seekBarControl.thresStatus){
                            seekBarControl.thresStatus=false;

                            bw.write("T"+0x01+seekBarControl.thresProgress+"end\r\n");
                            bw.flush();
                            //读取服务器返回的消息
                            br = new BufferedReader(new InputStreamReader(is));
                            mess = br.readLine();
                            msg = new Message();
                            msg.what=1;
                            bundle.putString("msg1",mess+"\r\n");
                            msg.setData(bundle);//mes利用Bundle传递数据
                            mhandler.sendMessage(msg);
                        }

                        if(seekBarControl.rollStatus){
                            seekBarControl.rollStatus=false;

                            bw.write("R"+0x02+seekBarControl.rollProgress+"end\r\n");
                            bw.flush();
                            //读取服务器返回的消息
                            br = new BufferedReader(new InputStreamReader(is));
                            mess = br.readLine();
                            msg = new Message();
                            msg.what=1;
                            bundle.putString("msg1",mess+"\r\n");
                            msg.setData(bundle);//mes利用Bundle传递数据
                            mhandler.sendMessage(msg);
                        }

                        if(seekBarControl.yawStatus){
                            seekBarControl.yawStatus=false;

                            bw.write("P"+0x03+seekBarControl.yawProgress+"end\r\n");
                            bw.flush();
                            //读取服务器返回的消息
                            br = new BufferedReader(new InputStreamReader(is));
                            mess = br.readLine();
                            msg = new Message();
                            msg.what=1;
                            bundle.putString("msg1",mess+"\r\n");
                            msg.setData(bundle);//mes利用Bundle传递数据
                            mhandler.sendMessage(msg);
                        }

                        if(beatflag){
                            beatflag=false;

                            bw.write("H"+0x04+"heart"+"end\r\n");
                            bw.flush();
                            //读取服务器返回的消息
                            br = new BufferedReader(new InputStreamReader(is));
                            mess = br.readLine();
                            msg = new Message();
                            msg.what=1;
                            bundle.putString("msg1",mess+"\r\n");
                            msg.setData(bundle);//mes利用Bundle传递数据
                            mhandler.sendMessage(msg);
                        }


                        if(!buttonStatus){
                            serverStatus=false;
                        }
                    }

                    os.close();
                    socket.close();

                }catch (Exception e){
                    e.printStackTrace();
                }
            }
        }).start();
    }



}

