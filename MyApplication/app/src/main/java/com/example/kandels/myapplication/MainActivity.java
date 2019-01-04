package com.example.kandels.myapplication;

import android.app.FragmentTransaction;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattService;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.graphics.Color;
import android.net.Uri;

import android.os.Bundle;
import android.os.IBinder;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.view.animation.RotateAnimation;
import android.widget.Button;
import android.widget.ExpandableListView;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.SimpleExpandableListAdapter;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;

public class MainActivity extends AppCompatActivity implements ManualFragment.OnFragmentInteractionListener, AutomaticFragment.OnFragmentInteractionListener{

    private boolean mode;
    public boolean mRunning;
    private final static String TAG = MainActivity.class.getSimpleName();

    //BLE
    public static final String EXTRAS_DEVICE_NAME = "DEVICE_NAME";
    public static final String EXTRAS_DEVICE_ADDRESS = "DEVICE_ADDRESS";
    private String mDeviceName;
    private String mDeviceAddress;
    private BluetoothLeService mBluetoothLeService;
    private boolean mConnected = false;
    private ExpandableListView mGattServicesList;
    private ArrayList<ArrayList<BluetoothGattCharacteristic>> mGattCharacteristics =
            new ArrayList<ArrayList<BluetoothGattCharacteristic>>();
    private BluetoothGattCharacteristic mNotifyCharacteristic;
    private TextView mDataField;
    private TextView mConnectionState;

    private final String LIST_NAME = "NAME";
    private final String LIST_UUID = "UUID";


    public BluetoothGattCharacteristic JackiModeCharacteristic;   // 0000fff1-0000-1000-8000-00805f9b34fb
    public BluetoothGattCharacteristic JackiSwitchCharacteristic; // 0000fff2-0000-1000-8000-00805f9b34fb
    public BluetoothGattCharacteristic JackiPowerCharacteristic;  // 0000fff3-0000-1000-8000-00805f9b34fb
    public BluetoothGattCharacteristic JackiSensorCharacteristic; // 0000fff6-0000-1000-8000-00805f9b34fb

    public final static UUID UUID_ROBOT_SENSOR =
            UUID.fromString(SampleGattAttributes.ROBOT_SENSOR);


    // MAP
    private FrameLayout map;
    private int width = 750;
    private int height = 800;
    private int size_one_element = 5;
    private int nb_el_width = width/size_one_element;
    private int number_square = height * width / (size_one_element * size_one_element);
    List<View> square_el = new ArrayList<View>();


    ImageView robot;
    int orientation_robot = LEFT;
    int position_robot = 1576;

    static final int STATE_UNKNOWN = 0;
    static final int STATE_OBSTACLE = 1;
    static final int STATE_FREE = 2;

    static final int UP = 0;
    static final int RIGHT = 1;
    static final int DOWN = 2;
    static final int LEFT = 3;


    // Code to manage Service lifecycle.
    private final ServiceConnection mServiceConnection = new
            ServiceConnection() {

                @Override
                public void onServiceConnected(ComponentName componentName,
                                               IBinder
                                                       service) {
                    mBluetoothLeService = ((BluetoothLeService.LocalBinder) service).getService();
                    if (!mBluetoothLeService.initialize()) {
                        Log.e(TAG, "Unable to initialize Bluetooth");
                        finish();
                    }
                    // Automatically connects to the device upon successful
                    // start-up
                    // initialization.
                    mBluetoothLeService.connect(mDeviceAddress);
                }

                @Override
                public void onServiceDisconnected(ComponentName componentName) {
                    mBluetoothLeService = null;
                }
            };

    // Handles various events fired by the Service.
    // ACTION_GATT_CONNECTED: connected to a GATT server.
    // ACTION_GATT_DISCONNECTED: disconnected from a GATT server.
    // ACTION_GATT_SERVICES_DISCOVERED: discovered GATT services.
    // ACTION_DATA_AVAILABLE: received data from the device.  This can be a
    // result of read
    //                        or notification operations.
    private final BroadcastReceiver mGattUpdateReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            final String action = intent.getAction();
            if (BluetoothLeService.ACTION_GATT_CONNECTED.equals(action)) {
                mConnected = true;
                updateConnectionState(R.string.connected);
                invalidateOptionsMenu();
            } else if (BluetoothLeService.ACTION_GATT_DISCONNECTED.equals(action)) {
                mConnected = false;
                updateConnectionState(R.string.disconnected);
                invalidateOptionsMenu();
            } else if (BluetoothLeService.ACTION_GATT_SERVICES_DISCOVERED.equals(action)) {
                // Show all the supported services and characteristics on the user interface.
                displayGattServices(mBluetoothLeService.getSupportedGattServices());
            } else if (BluetoothLeService.ACTION_DATA_AVAILABLE.equals(action)) {
                displayData(intent.getStringExtra(BluetoothLeService.EXTRA_DATA));
            }
        }
    };

    private void displayData(String data) {
        if (data != null) {
            mDataField.setText(data);
        }
    }

    private final ExpandableListView.OnChildClickListener servicesListClickListner =
            new ExpandableListView.OnChildClickListener() {
                @Override
                public boolean onChildClick(ExpandableListView parent, View v, int groupPosition,
                                            int childPosition, long id) {
                    if (mGattCharacteristics != null) {
                        final BluetoothGattCharacteristic characteristic =
                                mGattCharacteristics.get(groupPosition).get(childPosition);
                        final int charaProp = characteristic.getProperties();
                        if ((charaProp | BluetoothGattCharacteristic.PROPERTY_READ) > 0) {
                            // If there is an active notification on a characteristic, clear
                            // it first so it doesn't update the data field on the user interface.
                            if (mNotifyCharacteristic != null) {
                                mBluetoothLeService.setCharacteristicNotification(
                                        mNotifyCharacteristic, false);
                                mNotifyCharacteristic = null;
                            }
                            mBluetoothLeService.readCharacteristic(characteristic);
                            if(groupPosition==3) {
                                if(childPosition == 3) {
                                    JackiSensorCharacteristic = mGattCharacteristics.get(3).get(3);
                                }
                            }
                        }
                        if ((charaProp | BluetoothGattCharacteristic.PROPERTY_NOTIFY) > 0) {
                            mNotifyCharacteristic = characteristic;
                            mBluetoothLeService.setCharacteristicNotification(
                                    characteristic, true);
                        }
                        return true;
                    }
                    return false;
                }
            };

    private static IntentFilter makeGattUpdateIntentFilter() {
        final IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(BluetoothLeService.ACTION_GATT_CONNECTED);
        intentFilter.addAction(BluetoothLeService.ACTION_GATT_DISCONNECTED);
        intentFilter.addAction(BluetoothLeService
                .ACTION_GATT_SERVICES_DISCOVERED);
        intentFilter.addAction(BluetoothLeService.ACTION_DATA_AVAILABLE);
        return intentFilter;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        boolean bError = true;
        mRunning = false;

        // Start robot control screen in response to button
        // start only if connected to a Jacki RSLK with the correct version of ASEE running
        if(mGattCharacteristics.size() > 3) {
            if (mGattCharacteristics.get(3).size() > 3) {
                JackiSensorCharacteristic = mGattCharacteristics.get(3).get(3);
                if (UUID_ROBOT_SENSOR.equals(JackiSensorCharacteristic.getUuid())) {

                    // BLE
                    final Intent intent = getIntent();
                    mDeviceName = intent.getStringExtra(EXTRAS_DEVICE_NAME);
                    mDeviceAddress = intent.getStringExtra(EXTRAS_DEVICE_ADDRESS);
                    Intent gattServiceIntent = new Intent(this, BluetoothLeService.class);
                    bindService(gattServiceIntent, mServiceConnection, BIND_AUTO_CREATE);

                    // Sets up UI references.
                    ((TextView) findViewById(R.id.device_address)).setText(mDeviceAddress);
                    mGattServicesList = (ExpandableListView) findViewById(R.id.gatt_services_list);
                    mGattServicesList.setOnChildClickListener(servicesListClickListner);
                    mConnectionState = (TextView) findViewById(R.id.connection_state);
                    mDataField = (TextView) findViewById(R.id.data_value);

                    // Bottom Fragment

                    LinearLayout fragContainer = findViewById(R.id.FragmentLinearLayout);
                    FragmentTransaction ft = getFragmentManager().beginTransaction();

                    mode = intent.getBooleanExtra(ManualFragment.MANUAL, true);

                    if(mode) {
                        ManualFragment myManualFragment;
                        myManualFragment = new ManualFragment();
                        ft.add(fragContainer.getId(), myManualFragment, null);
                    }

                    else {
                        AutomaticFragment myAutomaticFragment;
                        myAutomaticFragment = new AutomaticFragment();
                        ft.add(fragContainer.getId(), myAutomaticFragment, null);
                    }

                    ft.commit();


                    bError = false; // ok to Launch RSLK controller
                }
            }
        }

        // No well connected to the robot
        if(bError){
            Toast.makeText(MainActivity.this, "Must be connected to an RSLK to run this app",
                    Toast.LENGTH_SHORT).show();
        }

        map = findViewById(R.id.map);
        robot = findViewById(R.id.robot);

        initialize_map();
        robot.bringToFront();
        rotate(DOWN);

        change_state_square(position_robot, STATE_FREE);




    }

    @Override
    protected void onResume() {
        super.onResume();

        //BLE
        registerReceiver(mGattUpdateReceiver, makeGattUpdateIntentFilter());
        if (mBluetoothLeService != null) {
            final boolean result = mBluetoothLeService.connect(mDeviceAddress);
            Log.d(TAG, "Connect request result=" + result);
        }

    }

    @Override
    protected void onPause() {
        super.onPause();
        //BLE
        unregisterReceiver(mGattUpdateReceiver);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (mBluetoothLeService != null) {
            unbindService(mServiceConnection);
            mBluetoothLeService = null;
        }
    }

    private void displayGattServices(List<BluetoothGattService> gattServices) {
        if (gattServices == null) return;
        String uuid = null;
        String unknownServiceString = getResources().getString(R.string.unknown_service);
        String unknownCharaString = getResources().getString(R.string.unknown_characteristic);
        ArrayList<HashMap<String, String>> gattServiceData = new ArrayList<HashMap<String, String>>();
        ArrayList<ArrayList<HashMap<String, String>>> gattCharacteristicData
                = new ArrayList<ArrayList<HashMap<String, String>>>();
        mGattCharacteristics = new ArrayList<ArrayList<BluetoothGattCharacteristic>>();

        // Loops through available GATT Services.
        for (BluetoothGattService gattService : gattServices) {
            HashMap<String, String> currentServiceData = new HashMap<String, String>();
            uuid = gattService.getUuid().toString();
            currentServiceData.put(
                    LIST_NAME, SampleGattAttributes.lookup(uuid, unknownServiceString));
            currentServiceData.put(LIST_UUID, uuid);
            gattServiceData.add(currentServiceData);

            ArrayList<HashMap<String, String>> gattCharacteristicGroupData =
                    new ArrayList<HashMap<String, String>>();
            List<BluetoothGattCharacteristic> gattCharacteristics =
                    gattService.getCharacteristics();
            ArrayList<BluetoothGattCharacteristic> charas =
                    new ArrayList<BluetoothGattCharacteristic>();

            // Loops through available Characteristics.
            for (BluetoothGattCharacteristic gattCharacteristic : gattCharacteristics) {
                charas.add(gattCharacteristic);
                HashMap<String, String> currentCharaData = new HashMap<String, String>();
                uuid = gattCharacteristic.getUuid().toString();
                currentCharaData.put(
                        LIST_NAME, SampleGattAttributes.lookup(uuid, unknownCharaString));
                currentCharaData.put(LIST_UUID, uuid);
                gattCharacteristicGroupData.add(currentCharaData);
            }
            mGattCharacteristics.add(charas);
            gattCharacteristicData.add(gattCharacteristicGroupData);
        }

        SimpleExpandableListAdapter gattServiceAdapter = new SimpleExpandableListAdapter(
                this,
                gattServiceData,
                android.R.layout.simple_expandable_list_item_2,
                new String[] {LIST_NAME, LIST_UUID},
                new int[] { android.R.id.text1, android.R.id.text2 },
                gattCharacteristicData,
                android.R.layout.simple_expandable_list_item_2,
                new String[] {LIST_NAME, LIST_UUID},
                new int[] { android.R.id.text1, android.R.id.text2 }
        );
        mGattServicesList.setAdapter(gattServiceAdapter);
    }

    private void updateConnectionState(final int resourceId) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                mConnectionState.setText(resourceId);
            }
        });
    }

    // New Robot

    private void VerifyConnection(){
        if(JackiSensorCharacteristic==null) {
            displayGattServices(mBluetoothLeService.getSupportedGattServices());
            JackiModeCharacteristic = mGattCharacteristics.get(3).get(0);   // 0000fff1-0000-1000-8000-00805f9b34fb
            JackiSwitchCharacteristic = mGattCharacteristics.get(3).get(1); // 0000fff2-0000-1000-8000-00805f9b34fb
            JackiPowerCharacteristic = mGattCharacteristics.get(3).get(2);  // 0000fff3-0000-1000-8000-00805f9b34fb
            JackiSensorCharacteristic = mGattCharacteristics.get(3).get(3); // 0000fff6-0000-1000-8000-00805f9b34fb
        }
    }



    // MAP

    void initialize_map(){
        View square;
        FrameLayout.LayoutParams params;
        int left_margin = - size_one_element;
        int top_margin = 0;
        for(int i=0; i < number_square; i++){
            square = new View(this);
            params = new FrameLayout.LayoutParams(size_one_element, size_one_element);
            square.setLayoutParams(new FrameLayout.LayoutParams(size_one_element, size_one_element));
            square.setBackgroundColor(getResources().getColor(android.R.color.darker_gray));
            if(left_margin >= width - size_one_element){
                top_margin += size_one_element;
                left_margin = 0;
            }
            else{
                left_margin += size_one_element;
            }
            params.topMargin = top_margin;
            params.leftMargin = left_margin;
            Log.i("index square", Integer.toString(i));
            square_el.add(i, square);
            map.addView(square_el.get(i), params);
        }

    }

    // 0 = don't know -> gray
    // 1 = obstacle -> red
    // 2 = free -> green
    void change_state_square(int index, int situation){

        if(situation == STATE_UNKNOWN){
            square_el.get(index).setBackgroundColor(getResources().getColor(android.R.color.darker_gray));
        }
        else if(situation == STATE_OBSTACLE){
            square_el.get(index).setBackgroundColor(getResources().getColor(android.R.color.holo_red_light));
        }
        else if(situation == STATE_FREE){
            square_el.get(index).setBackgroundColor(getResources().getColor(android.R.color.holo_green_dark));
        }
    }

    private void rotate(int direction) {
        int degree = (direction - orientation_robot) * 90;
        final RotateAnimation rotateAnim = new RotateAnimation(0.0f, degree,
                RotateAnimation.RELATIVE_TO_SELF, 0.5f,
                RotateAnimation.RELATIVE_TO_SELF, 0.5f);

        rotateAnim.setDuration(0);
        rotateAnim.setFillAfter(true);
        robot.startAnimation(rotateAnim);
        orientation_robot = direction;

        int margin_left = get_marginLeft_from_index(position_robot);
        int margin_top = get_marginTop_from_index(position_robot);
        switch(orientation_robot){
            case UP : margin_left -= 8;
                margin_top -= 0;
                break;
            case RIGHT : margin_left -= 14;
                margin_top -= 7;
                break;
            case DOWN : margin_left -= 7;
                margin_top -= 13;
                break;
            case LEFT : margin_left -= 1;
                margin_top -= 6;
                break;
        }
        FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(FrameLayout.LayoutParams.WRAP_CONTENT, FrameLayout.LayoutParams.WRAP_CONTENT);
        params.setMargins(margin_left, margin_top, 0, 0);
        robot.setLayoutParams(params);

    }

    int get_marginLeft_from_index(int index){
        int marginLeft;
        if(index != 0){
            marginLeft = index%nb_el_width * size_one_element;
        }else{
            marginLeft = 0;
        }

        return marginLeft;
    }

    int get_marginTop_from_index(int index){
        int marginTop;
        if(index != 0){
            marginTop = (int) (index / nb_el_width) * size_one_element;
        }else{
            marginTop = 0;
        }
        return marginTop;
    }

    // MANUAL

    //TODO: copy the readsensors and updatesensors once we have them in the fuckin robot

    public void UpMovement(View view) {
        Button button_down = findViewById(R.id.button_up);
        view.setBackgroundColor(Color.YELLOW);
        button_down.setBackgroundColor(getResources().getColor(R.color.OrangeDark));
        TextView textView = findViewById(R.id.button_start);

        if (!mRunning) {
            textView.setText("Halt");

            //     statusView.setText("Running");
            mRunning = true;
            byte data[] ={1}; // go command
            JackiModeCharacteristic.setValue(data);
            // JackiModeCharacteristic.setWriteType(BluetoothGattCharacteristic.WRITE_TYPE_NO_RESPONSE);
            mBluetoothLeService.writeCharacteristic(JackiModeCharacteristic);
        }else {
            textView.setText("Go");
            //    statusView.setText("Halted");
            mRunning = false;
            byte data[] ={0}; // stop command
            JackiModeCharacteristic.setValue(data);
            // JackiModeCharacteristic.setWriteType(BluetoothGattCharacteristic.WRITE_TYPE_NO_RESPONSE);
            mBluetoothLeService.writeCharacteristic(JackiModeCharacteristic);
        }
        //UpdateSensorStatus();
        //ReadSensors();
    }

    public void DownMovement(View view) {
        Button button_up = findViewById(R.id.button_down);
        view.setBackgroundColor(Color.YELLOW);
        button_up.setBackgroundColor(getResources().getColor(R.color.OrangeDark));
        TextView textView = findViewById(R.id.button_start);
        textView.setText("Halt");
        mRunning = true;
        byte data[] ={2}; // back command
        VerifyConnection();  // back sure Jacki is connected
        JackiModeCharacteristic.setValue(data);
        //       JackiModeCharacteristic.setWriteType(JackiModeCharacteristic.WRITE_TYPE_NO_RESPONSE);
        mBluetoothLeService.writeCharacteristic(JackiModeCharacteristic);
        //UpdateSensorStatus();
    }

    public void RightMovement(View view) {
        Button button_left = findViewById(R.id.button_right);
        view.setBackgroundColor(Color.YELLOW);
        button_left.setBackgroundColor(getResources().getColor(R.color.OrangeDark));
        //   TextView statusView = findViewById(R.id.status);
        //   statusView.setText("Right");
        TextView textView = findViewById(R.id.button_start);
        textView.setText("Halt");
        mRunning = true;
        byte data[] ={3}; // hard right
        VerifyConnection();  // back sure Jacki is connected
        JackiModeCharacteristic.setValue(data);
        //  JackiModeCharacteristic.setWriteType(JackiModeCharacteristic.WRITE_TYPE_NO_RESPONSE);
        mBluetoothLeService.writeCharacteristic(JackiModeCharacteristic);
        //UpdateSensorStatus();
        // ReadSensors();
    }

    public void LeftMovement(View view) {
        Button button_right = findViewById(R.id.button_left);
        view.setBackgroundColor(Color.YELLOW);
        button_right.setBackgroundColor(getResources().getColor(R.color.OrangeDark));
        //  TextView statusView = findViewById(R.id.status);
        //  statusView.setText("Left");
        TextView textView = findViewById(R.id.button_start);
        textView.setText("Halt");
        mRunning = true;
        byte data[] ={4}; // left command
        VerifyConnection();  // back sure Jacki is connected
        JackiModeCharacteristic.setValue(data);
        // JackiModeCharacteristic.setWriteType(BluetoothGattCharacteristic.WRITE_TYPE_NO_RESPONSE);
        mBluetoothLeService.writeCharacteristic(JackiModeCharacteristic);
        //UpdateSensorStatus();
        //     ReadSensors();
    }


    public void StartMovement(View view) {
        Button button_start = findViewById(R.id.button_start);
        if(button_start.getText()==getString(R.string.Start)){
            view.setBackgroundColor(Color.RED);
            button_start.setText(getString(R.string.Stop));
        }
        else{
            Button button_right = findViewById(R.id.button_right);
            button_right.setBackgroundColor(getResources().getColor(R.color.OrangeDark));

            Button button_left = findViewById(R.id.button_left);
            button_left.setBackgroundColor(getResources().getColor(R.color.OrangeDark));

            Button button_up = findViewById(R.id.button_up);
            button_up.setBackgroundColor(getResources().getColor(R.color.OrangeDark));

            Button button_down = findViewById(R.id.button_down);
            button_down.setBackgroundColor(getResources().getColor(R.color.OrangeDark));

            view.setBackgroundColor(getResources().getColor(R.color.Orange));
            button_start.setText(getString(R.string.Start));
        }

    }

    // AUTOMATIC

    public void AutomaticMovement(View view) {
        Button button_auto = findViewById(R.id.button_auto);

        if(button_auto.getText()==getString(R.string.Start)){
            view.setBackgroundColor(Color.RED);
            button_auto.setText(getString(R.string.Stop));
        }
        else {
            view.setBackgroundColor(getResources().getColor(R.color.Orange));
            button_auto.setText(getString(R.string.Start));
        }
    }

    @Override
    public void onFragmentInteraction(Uri uri) {

    }





}
