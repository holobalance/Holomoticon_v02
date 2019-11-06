package com.example.holomoticon_v02;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import android.app.PendingIntent;
import android.bluetooth.BluetoothClass;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.IBinder;
import android.os.Binder;
import android.provider.Telephony;
import android.renderscript.Allocation;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;

import java.net.Socket;
import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Scanner;
import java.util.concurrent.locks.AbstractQueuedSynchronizer;

import de.moticon.insole3_service.Insole3Service;
import de.moticon.insole3_service.Insole3Service.Insole3Binder;
import de.moticon.insole3_service.proto.Service;
import de.moticon.insole3_service.proto.Common;
import de.moticon.insole3_service.proto.Service.StartInsoleScan;

import static de.moticon.insole3_service.proto.Service.*;


import java.io.IOException;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.MulticastSocket;

import android.net.wifi.WifiManager;
import android.net.wifi.WifiManager.MulticastLock;





public class MainActivity extends AppCompatActivity {

    private volatile boolean FOUNDRIGHT = false;
    private volatile boolean FOUNDLEFT = false;
    private volatile boolean CONNECTED = false;
    private volatile boolean CONNECTEDRIGHT = false;
    private volatile boolean CONNECTEDLEFT = false;
    private volatile boolean CONESTABLISHED = false;

    private volatile boolean CON_LEFT_ESTABLISHED = false;
    private volatile boolean CON_RIGHT_ESTABLISHED = false;

    private volatile boolean READYTOSTARTSERVICE = false;
    private volatile int insoleserial = 0;
    private volatile int insoleserialLEFT = 0;
    private volatile int insoleserialRIGHT = 0;

    private volatile boolean STARTINCOLESCONNECTION = false;

    private volatile int RIGHTSERVICECOUNTER = 0;
    private volatile int LEFTSERVICECOUNTER = 0;

    private ConnectInsoles myconnectInsoles;
    private InsoleDevice LEFTinsole;
    private InsoleDevice RIGHTinsole;


    private volatile MoticonMessage msg01;

    private EditText editTextInput;

    private static Socket s;

    private static InputStreamReader isr;
    private static BufferedReader br;
    private static PrintWriter printWriter;
    private static Scanner inFromServer;

    String message = "";
    private static String ip = "192.168.0.25";


    //private static ServiceConnection mServiceConnection;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        //editTextInput = findViewById(R.id.edit_text_input);

        //getBaseContext().getSystemService(Context.LOCATION_SERVICE);


        bindService();

        LocalBroadcastManager.getInstance(getApplicationContext()).registerReceiver(createServiceMsgReceiver(), new IntentFilter(Insole3Service.BROADCAST_SERVICE_MSG));

        UDPRECEIVER ur = new UDPRECEIVER();
        ur.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);


        MSGSENDER m = new MSGSENDER();
        m.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);

    }


    private BroadcastReceiver createServiceMsgReceiver() {

        BroadcastReceiver br = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {

                byte[] protoMsg = intent.getByteArrayExtra(Insole3Service.EXTRA_PROTO_MSG);
                MoticonMessage moticonMessage;

                moticonMessage = MoticonMessage.parseFrom(protoMsg);
                Log.d("test", moticonMessage.toString());

                Log.d("activity", moticonMessage.getMsgCase().name());

                if ((moticonMessage.getMsgCase().name() == "INSOLE_ADVERTISEMENT") && (CONNECTEDRIGHT == false || CONNECTEDLEFT == false) ) {
                    Log.d("[Moticon message]", "RECEIVED AN ADV MESSAGE");



                    msg01 = moticonMessage;
                    //Common.Side myside = Common.Side.RIGHT;
                    //InsoleDevice mysole = InsoleDevice.newBuilder().setDeviceAddress("D9:32:C2:93:4E:1B").setSide(myside).build();
                   // MoticonMessage.Builder moticonMessage2 =  MoticonMessage.newBuilder();
                    //ConnectInsoles cis =  ConnectInsoles.newBuilder(moticonMessage.getConnectInsoles()).build();

                    if (moticonMessage.getInsoleAdvertisement().getInsole().getSide().name() == "LEFT" && FOUNDLEFT == false)
                    {
                        LEFTinsole = moticonMessage.getInsoleAdvertisement().getInsole();
                        FOUNDLEFT = true;

                        EditText myEditText2 = (EditText)findViewById(R.id.editText2);
                        myEditText2.setText("Left OK");
                    }

                    if (moticonMessage.getInsoleAdvertisement().getInsole().getSide().name() == "RIGHT" && FOUNDRIGHT == false)
                    {
                        RIGHTinsole = moticonMessage.getInsoleAdvertisement().getInsole();
                        FOUNDRIGHT = true;

                        EditText myEditText = (EditText)findViewById(R.id.editText);
                        myEditText.setText("Right OK");

                    }

                    if (FOUNDLEFT && FOUNDRIGHT) {
                        myconnectInsoles = ConnectInsoles.newBuilder().addInsoles(LEFTinsole).addInsoles(RIGHTinsole).build();
                        MoticonMessage.Builder moticonMessage2 = MoticonMessage.newBuilder();
                        moticonMessage2.setConnectInsoles(myconnectInsoles);
                        Log.d("[Device IDs] ", LEFTinsole.getDeviceAddress() + ' ' + RIGHTinsole.getDeviceAddress());
                        Log.d("[# IDs] ", String.valueOf(myconnectInsoles.getInsolesCount()));

                        //MoticonMessage moticonMessage3 = MoticonMessage.parseFrom(moticonMessage2.build().toByteArray());
                        //Log.d("Constructed frame ", moticonMessage3.toString());

                        sendProtoToService(moticonMessage2.build().toByteArray());
                        CONNECTED = true;

                        //STOP SCANNING
                        MoticonMessage.Builder moticonMessage4 = MoticonMessage.newBuilder();
                        StopInsoleScan sinscan = StopInsoleScan.newBuilder().build();
                        moticonMessage4.setStopInsoleScan(sinscan);
                        sendProtoToService(moticonMessage4.build().toByteArray());
                        Log.d("M o t i c o n] ", moticonMessage4.getMsgCase().name());
                    }

                }


                if ((moticonMessage.getMsgCase().name() == "INSOLE_CONNECTION_STATUS") && moticonMessage.getInsoleConnectionStatus().getStatus().name() == "DISCONNECTED")
                {
                    EditText myEditText2 = (EditText)findViewById(R.id.editText2);
                    myEditText2.setText("LEFT DISCONNECTED");
                    EditText myEditText = (EditText)findViewById(R.id.editText);
                    myEditText.setText("Right DISCONNECTED");
                }
                if ((moticonMessage.getMsgCase().name() == "INSOLE_CONNECTION_STATUS") && moticonMessage.getInsoleConnectionStatus().getStatus().toString() == "READY" && moticonMessage.getInsoleConnectionStatus().getSide().name() == "LEFT") {
                    CON_LEFT_ESTABLISHED = true;
                }

                if ((moticonMessage.getMsgCase().name() == "INSOLE_CONNECTION_STATUS") && moticonMessage.getInsoleConnectionStatus().getStatus().toString() == "READY" && moticonMessage.getInsoleConnectionStatus().getSide().name() == "RIGHT") {
                    CON_RIGHT_ESTABLISHED = true;
                }

                if ( (moticonMessage.getMsgCase().name() == "START_SERVICE_CONF") ) {


                    RIGHTSERVICECOUNTER = moticonMessage.getStartServiceConf().getRightStartServiceConf().getServiceCounter();
                    Log.e("[SERVICE COUNTER] ", String.valueOf(RIGHTSERVICECOUNTER));

                    LEFTSERVICECOUNTER = moticonMessage.getStartServiceConf().getLeftStartServiceConf().getServiceCounter();
                    Log.e("[SERVICE COUNTER] ", String.valueOf(LEFTSERVICECOUNTER));
                }



                if ( (moticonMessage.getMsgCase().name() == "STATUS_INFO") && (CON_LEFT_ESTABLISHED) && (CON_RIGHT_ESTABLISHED) )
                {
                    List<InsoleStatusInfo> mysoles = moticonMessage.getStatusInfo().getInsoleStatusInfoList();

                    for (InsoleStatusInfo insi : mysoles) {
                        if (insi.getInsoleInfo().getSide().name() == "LEFT")
                            insoleserialLEFT = insi.getInsoleInfo().getInsoleSettings().getSerialNumber();
                        else
                            insoleserialRIGHT = insi.getInsoleInfo().getInsoleSettings().getSerialNumber();
                    }
                    //moticonMessage.getStatusInfo().getInsoleStatusInfo(0).getInsoleInfo().getInsoleSettings().getSerialNumber();
                    //Log.d("S E R I A L ", String.valueOf(insoleserial));

                    if (insoleserialLEFT > 0 && insoleserialRIGHT > 0)
                        READYTOSTARTSERVICE = true;
/*
                    Date date = new Date();

                    MoticonMessage.Builder moticonMessage2 = MoticonMessage.newBuilder();
                    Common.ServiceId sid = Common.ServiceId.newBuilder().setRightSerialNumber(insoleserial).build();
                    Common.ServiceType stp = Common.ServiceType.LIVE;



                    Common.ServiceEndpoint sep = Common.ServiceEndpoint.
                            newBuilder().
                            setEndpointType(Common.ServiceEndpoint.EndpointType.APP).
                            build();


                    Common.EndpointSettings eps = Common.EndpointSettings.newBuilder().
                            setIpAddress("192.168.0.107").
                            setPort(9083).
                            build();

                    Common.ServiceEndpoint sep2 = Common.ServiceEndpoint.
                            newBuilder().
                            setEndpointSettings(eps).
                            setEndpointType(Common.ServiceEndpoint.EndpointType.PC).
                            build();

                    Common.ServiceConfig.AccGRange accr = Common.ServiceConfig.AccGRange.ACC_16_G;
                    Common.ServiceConfig.AccOdr accor = Common.ServiceConfig.AccOdr.ACC_104_ODR;
                    Common.ServiceConfig.AngOdr angor = Common.ServiceConfig.AngOdr.ANG_104_ODR;

                    List<Boolean> myPressure = new ArrayList<Boolean>();
                    for (int p = 0; p < 16; p++)
                        myPressure.add(true);

                    List<Boolean> myAngular = new ArrayList<Boolean>();
                    for (int p = 0; p < 3; p++)
                        myAngular.add(true);


                    Common.ServiceConfig tmpserviceconf = Common.ServiceConfig.newBuilder().
                            setServiceStartTime(date.getTime()).
                            setServiceId(sid).
                            setAngDpsRange(Common.ServiceConfig.AngDpsRange.ANG_2000_DPS).
                            addAllEnabledPressure(myPressure).
                            addAllEnabledAngular(myAngular).
                            setEnabledPressure(15, true).
                            setEnabledAngular(2, true).
                            setServiceType(stp).setRate(100).
                            setAccOdr(accor).
                            setAngOdr(angor).
                            setEnabledTotalForce(true).
                            setActivityProfile(Common.ServiceConfig.ActivityProfile.ACTIVITY_PROFILE_ACCELERATION).
                            build();


                    StartService msgStartService = StartService.
                            newBuilder().
                            setServiceConfig(tmpserviceconf).
                            setServiceEndpoint(sep2).
                            build();

                    moticonMessage2.setStartService(msgStartService);


                    MoticonMessage tmp;
                    tmp = MoticonMessage.parseFrom(moticonMessage2.build().toByteArray());
                    Log.d("test", tmp.toString());


                    sendProtoToService(moticonMessage2.build().toByteArray());
*/
                }

            }
        };
        return br;
    }

    public void startService(View v) {
        String input = editTextInput.getText().toString();

        Intent serviceIntent = new Intent(this, ExampleService.class);
        serviceIntent.putExtra("inputExtra", input);
        //startService(serviceIntent);
        ContextCompat.startForegroundService(this, serviceIntent);

        //commManager cm = new commManager();
        //cm.execute();


        //MSGSENDER m = new MSGSENDER();
        //m.execute();

            //public void run() {}

/*
        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        MoticonMessage.Builder moticonMessage1 = MoticonMessage.newBuilder();
        StartInsoleScan sis = StartInsoleScan.newBuilder().setIncludeDFU(false).build();
        moticonMessage1.setStartInsoleScan(sis);

        MoticonMessage moticonMessage3 = MoticonMessage.parseFrom(moticonMessage1.build().toByteArray());
        Log.d("Constructed frame ADV", moticonMessage3.toString());

        Log.e("hello", "wait...");
//
        //while (!CONNECTED) {
        for (int i = 0; i<10; i++) {

            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }

            sendProtoToService(moticonMessage1.build().toByteArray());
        }
*/

        Toast.makeText(getApplicationContext(), "Data send to server", Toast.LENGTH_LONG).show();
    }

    public void stopService(View v) {
        String input = editTextInput.getText().toString();

        Intent serviceIntent = new Intent(this, ExampleService.class);
        serviceIntent.putExtra("inputExtra", input);
        stopService(serviceIntent);
    }


    private ServiceConnection mServiceConnection = new ServiceConnection() {

        boolean mBound = false;
        private Insole3Binder mInsole3Service;

        @Override
        public void onServiceConnected(ComponentName i, IBinder service) {
            Log.e("MOTICON", "on Service Connected");
            mInsole3Service = (Insole3Service.Insole3Binder) service;
        }

        @Override
        public void onServiceDisconnected(ComponentName arg0) {
            mInsole3Service = null;
        }
    };


    private void bindService() {
        boolean mBound = false;
        Intent mIntentInsole3 = new Intent(this, Insole3Service.class);


        try {
            if (mBound) {
                //Already running
            } else {
                //ContextCompat.startForegroundService(this, mIntentInsole3);


                getApplicationContext().startService(mIntentInsole3);
                //startService(mIntentInsole3);
                getApplicationContext().bindService(mIntentInsole3, mServiceConnection, 0);
                mBound = true;
                Log.e("MOTICON", "Start the service");
            }
        } catch (IllegalStateException e) {
            Log.e("ERROR", e.toString());
        }
    }

    private void unbindService(View v) {
        boolean mBound = false;

        try {
            unbindService(mServiceConnection);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void sendProtoToService(byte[] message) {

        Intent broadcast = new Intent(Insole3Service.BROADCAST_CONTROLLER_MSG);
        broadcast.putExtra(Insole3Service.EXTRA_PROTO_MSG, message);
        boolean b = LocalBroadcastManager.getInstance(getApplicationContext()).sendBroadcast(broadcast);
        Log.d("MESSAGEOUT", String.valueOf(b) + " MSG = " + message.toString());

        //getInstance(getApplicationContext()).sendBroadcast(broadcast);


    }


    public void send_text(View v) {

        message = "Hello World";
        //toServer mt = new toServer();
        //mt.execute();

        Toast.makeText(getApplicationContext(), "Data send", Toast.LENGTH_LONG).show();

    }


    class commManager extends AsyncTask<Void, Void, Void> {

        @Override
        protected Void doInBackground(Void... params) {
            try {
                Socket s;
                boolean connected = false;
                message = "Hello World";
                while (true) {
                    try {
                        if (!connected) {
                            s = new Socket(ip, 5556);
                            connected = true;
                            toServer tS = new toServer(s);
                            tS.start();

                            tS.join();

                            //mServiceConnection.mInsole3Service;
                            break;
                        }
                    } catch (IOException e) {
                        e.printStackTrace();
                        connected = false;
                    }
                    try {
                        Thread.sleep(5000);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }

            } catch (Exception e) {
                e.printStackTrace();
            }

            return null;
        }
    }


    class toServer extends Thread {
        Socket soc;

        public toServer(Socket s) {
            this.soc = s;
        }

        public void run() {
            try {
                printWriter = new PrintWriter(soc.getOutputStream());
                for (int i = 0; i < 3; i++) {
                    printWriter.write(message);
                    try {
                        Thread.sleep(5000);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                    printWriter.flush();
                }

                printWriter.close();

            } catch (IOException e) {
                e.printStackTrace();

            }

        }
    }


    class MSGSENDER extends AsyncTask<Void, Void, Void> {
        @Override
        protected Void doInBackground(Void... params) {
            try {

                while (true) {

                    try {
                        Thread.sleep(1000);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }


                    while (!STARTINCOLESCONNECTION) {

                        try {
                            Thread.sleep(500);
                            Log.d("[I N F O]", "Waiting for EDGE to start...");
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                    }


                    MoticonMessage.Builder moticonMessage1 = MoticonMessage.newBuilder();
                    StartInsoleScan sis = StartInsoleScan.newBuilder().setIncludeDFU(false).build();
                    moticonMessage1.setStartInsoleScan(sis);

                    MoticonMessage moticonMessage3 = MoticonMessage.parseFrom(moticonMessage1.build().toByteArray());
                    Log.d("Constructed frame ADV", moticonMessage3.toString());


                    while (!CONNECTED) {

                        try {
                            Thread.sleep(1000);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }

                        sendProtoToService(moticonMessage1.build().toByteArray());
                    }

                    while (!READYTOSTARTSERVICE) {

                        try {
                            Thread.sleep(500);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                    }


                    //Log.d("S E R I A L ", String.valueOf(insoleserial));

                    //READYTOSTARTSERVICE = true;

                    Date date = new Date();
                    MoticonMessage.Builder moticonMessage2 = MoticonMessage.newBuilder();
                    Common.ServiceId sid = Common.ServiceId.newBuilder().setLeftSerialNumber(insoleserialLEFT).setRightSerialNumber(insoleserialRIGHT).build();
                    Common.ServiceType stp = Common.ServiceType.LIVE;


                    Common.ServiceEndpoint sep = Common.ServiceEndpoint.
                            newBuilder().
                            setEndpointType(Common.ServiceEndpoint.EndpointType.APP).
                            build();


                    Common.EndpointSettings eps = Common.EndpointSettings.newBuilder().
                            setIpAddress(ip).
                            setPort(9083).
                            build();

                    Common.ServiceEndpoint sep2 = Common.ServiceEndpoint.
                            newBuilder().
                            setEndpointSettings(eps).
                            setEndpointType(Common.ServiceEndpoint.EndpointType.PC).
                            build();

                    Common.ServiceConfig.AccGRange accr = Common.ServiceConfig.AccGRange.ACC_16_G;
                    Common.ServiceConfig.AccOdr accor = Common.ServiceConfig.AccOdr.ACC_104_ODR;
                    Common.ServiceConfig.AngOdr angor = Common.ServiceConfig.AngOdr.ANG_104_ODR;

                    List<Boolean> myPressure = new ArrayList<Boolean>();
                    for (int p = 0; p < 16; p++)
                        myPressure.add(true);

                    List<Boolean> myAngular = new ArrayList<Boolean>();
                    for (int p = 0; p < 3; p++)
                        myAngular.add(true);

                    List<Boolean> myAcceler = new ArrayList<Boolean>();
                    for (int p = 0; p < 3; p++)
                        myAcceler.add(true);

                    List<Boolean> myCop = new ArrayList<Boolean>();
                    for (int p = 0; p < 2; p++)
                        myCop.add(true);


                Common.ServiceConfig tmpserviceconf = Common.ServiceConfig.newBuilder().
                            setServiceStartTime(date.getTime()).
                            setServiceId(sid).
                            setAccGRange(accr).
                            setAngDpsRange(Common.ServiceConfig.AngDpsRange.ANG_2000_DPS).
                            addAllEnabledAcceleration(myAcceler).
                            addAllEnabledCop(myCop).
                            addAllEnabledPressure(myPressure).
                            addAllEnabledAngular(myAngular).
                            setEnabledPressure(15, true).
                            setEnabledAcceleration(2, true).
                            setEnabledAngular(2, true).
                            setEnabledCop(1, true).
                            setEnabledTotalForce(true).
                            setServiceType(stp).
                            setRate(100).
                            setAccOdr(accor).
                            setAngOdr(angor).
                            setEnabledTotalForce(true).
                            setActivityProfile(Common.ServiceConfig.ActivityProfile.ACTIVITY_PROFILE_CONTINUOUS).
                            build();


                    StartService msgStartService = StartService.
                            newBuilder().
                            setServiceConfig(tmpserviceconf).
                            setServiceEndpoint(sep2).
                            build();

                    moticonMessage2.setStartService(msgStartService);


                    MoticonMessage tmp;
                    tmp = MoticonMessage.parseFrom(moticonMessage2.build().toByteArray());
                    Log.d("test", tmp.toString());

                    sendProtoToService(moticonMessage2.build().toByteArray());


                    STARTINCOLESCONNECTION = false;
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
            return null;
        }

    }


    class UDPRECEIVER extends AsyncTask<Void, Void, Void> {

        protected MulticastSocket socket = null;
        protected byte[] buf = new byte[256];
        @Override
        protected Void doInBackground(Void... params) {
            try {

                Log.e("MSG", "in the UDPreceiver");

                socket = new MulticastSocket(10000);
                InetAddress group = InetAddress.getByName("224.3.29.71");
                socket.joinGroup(group);

                //while(true) {

                    while (true) {

                        //STARTINCOLESCONNECTION = true;

                        WifiManager wifi = (WifiManager) getSystemService(Context.WIFI_SERVICE);
                        MulticastLock multicastLock = wifi.createMulticastLock("multicastLock");
                        multicastLock.setReferenceCounted(true);
                        multicastLock.acquire();

                        DatagramPacket packet = new DatagramPacket(buf, buf.length);
                        socket.receive(packet);

                        if (multicastLock != null) {
                            multicastLock.release();
                            multicastLock = null;
                        }
                        String received = new String(
                                packet.getData(), 0, packet.getLength());
                        Log.e("MSG", received);

                        if ("end".equals(received)) {
                            break;
                        }

                        String[] dr = received.split(",");
                        Log.e("udp message", dr[0]);
                        if ("startConnection".equals(dr[0])) {
                            STARTINCOLESCONNECTION = true;
                            ip = dr[1];
                        }


                        if ("endService".equals(dr[0])) {
                            StopServiceConf ssc = StopServiceConf.newBuilder().build();

                            StopService stopmyservice = StopService.newBuilder().setRightServiceCounter(RIGHTSERVICECOUNTER).setLeftServiceCounter(LEFTSERVICECOUNTER).build();
                            MoticonMessage.Builder moticonMessage5 = MoticonMessage.newBuilder();
                            moticonMessage5.setStopService(stopmyservice);
                            sendProtoToService(moticonMessage5.build().toByteArray());
                            Log.d("M o t i c o n] ", "Ending service");

                            DisconnectInsoles din = DisconnectInsoles.newBuilder().build();
                            MoticonMessage.Builder moticonMessage6 = MoticonMessage.newBuilder();
                            moticonMessage6.setDisconnectInsoles(din);
                            sendProtoToService(moticonMessage6.build().toByteArray());
                            STARTINCOLESCONNECTION = false;
                            CONESTABLISHED = false;
                            CONNECTED = false;
                            READYTOSTARTSERVICE = false;
                            RIGHTSERVICECOUNTER = 0;
                            FOUNDLEFT = false;
                            FOUNDRIGHT = false;
                            CON_LEFT_ESTABLISHED = false;
                            CON_RIGHT_ESTABLISHED = false;


                        }
                    }
                //}
                socket.leaveGroup(group);
                socket.close();


/*
                ServerSocket ss = null;

                // Initialize Server Socket to listen to its opened port
                ss = new ServerSocket(5555);

                // Accept any client connection
                Socket s = ss.accept();
                Log.e("MSG", "connection accepted");
                // Initialize Buffered reader to read the message from the client
                BufferedReader in = new BufferedReader(new InputStreamReader(s.getInputStream()));

                // Get incoming message
                String incomingMessage = in.readLine() + System.getProperty("line.separator");
                Log.e("MSG", incomingMessage);


                // Use incomingMessage as required
                System.out.print(incomingMessage);

                // Close input stream
                in.close();

                // Close Socket
                s.close();

                // Close server socket
                ss.close();

*/

            } catch (IOException e) {
                e.printStackTrace();
            }
            return null;
        }

    }
}
