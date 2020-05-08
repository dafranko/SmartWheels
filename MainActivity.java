package com.example.smartwheels;

    import androidx.appcompat.app.AppCompatActivity;

    import android.os.Handler;
    import android.bluetooth.BluetoothDevice;
    import android.bluetooth.BluetoothAdapter;
    import android.bluetooth.BluetoothSocket;
    import android.content.Intent;
    import android.os.Bundle;
    import android.os.ParcelUuid;
    import android.view.View;
    import android.view.MotionEvent;
    import android.widget.Button;
    import android.widget.TextView;
    import android.widget.Toast;

    import java.io.OutputStream;
    import java.io.IOException;
    import java.util.Set;

    import androidx.core.app.ActivityCompat;
    import android.Manifest;
    import android.annotation.SuppressLint;
    import android.content.Context;
    import android.content.pm.PackageManager;
    import android.location.Location;
    import android.location.LocationManager;
    import android.os.Looper;
    import android.provider.Settings;
    import com.google.android.gms.location.FusedLocationProviderClient;
    import com.google.android.gms.location.LocationCallback;
    import com.google.android.gms.location.LocationRequest;
    import com.google.android.gms.location.LocationResult;
    import com.google.android.gms.location.LocationServices;
    import com.google.android.gms.tasks.OnCompleteListener;
    import com.google.android.gms.tasks.Task;

/**
 * Main class that executes the application boot up actions and
 * the event handlers executed when the button is pressed.
 */
public class MainActivity extends AppCompatActivity {
    /****************************  Global Constants    ****************************/
    // The MAC address of the Bluetooth module that the app will connect to
    final String MAC_ADDR = "00:18:91:D8:38:A0";
    // Below are strings that are used to display messages on screen
    final String DC_STRING = "Disconnected";
    final String C_STRING = "Connected";
    final String AC_STRING = "Already Connected";
    final String ADC_STRING = "Already Disconnected";
    final String BT_OFF_STRING = "Bluetooth is turned off";
    final String STATIONARY_S = "Stationary";
    final String FOLLOWING_S = "Following";
    final String APPLICATION_S = "Application";
    // The amount of time in seconds that the bluetooth notification will appear
    final int SECONDS_30 = 30;
    // The delay between each repeat message sent while holding button in ms
    final int DELAY_MS = 100;

    // Characters used in filtering methods
    final char CHAR_R = 'R';
    final char CHAR_L = 'L';
    final char CHAR_F = 'F';
    final char CHAR_B = 'B';
    final char CHAR_ON = 'C';
    final char CHAR_OFF = 'O';
    final char CHAR_STATIONARY = '1';
    final char CHAR_FOLLOWING = '2';
    final char CHAR_APPLICATION = '3';
    final char CHAR_S = 'S';

    /****************************  Widget Variables    ****************************/
    // Directional buttons
    Button btn_up,btn_down, btn_left, btn_right;
    // Mode buttons
    Button btn_stationary, btn_following, btn_application;
    // Bluetooth connection buttons
    Button btn_bt, btn_dc;
    // Text display on screen displaying if Bluetooth is on
    TextView txt_bluetooth;
    // Text displaying the mode the wagon is in
    TextView txt_mode;


    /****************************  Bluetooth Variables    ****************************/
    // Variable that is used to check if the phone has Bluetooth on
    BluetoothAdapter bluetooth_adapter = BluetoothAdapter.getDefaultAdapter();
    // Variable that is used to collect all the phone's known Bluetooth devices
    Set<BluetoothDevice> paired_devices = bluetooth_adapter.getBondedDevices();
    // Variable used to store the device that has been connected to
    BluetoothDevice bluetooth_device = null;
    // Variables used to create a connection between the phone and the BT module
    BluetoothSocket bluetooth_socket;
    ParcelUuid[] uuids;
    // Variable used to send data to the Bluetooth module
    OutputStream output_data;

    /************************* GPS Variables ***************************************/
    int PERMISSION_ID = 1010;
    TextView latTextView, lonTextView;
    FusedLocationProviderClient mFusedLocationClient;


    @Override
    /**
     * Method that is called when the application is initially started
     */
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Connect variables to the widget used in the design
        btn_up = findViewById(R.id.btn_up);
        btn_down = findViewById(R.id.btn_down);
        btn_left = findViewById(R.id.btn_left);
        btn_right = findViewById(R.id.btn_right);
        btn_bt = findViewById(R.id.btn_bt);
        btn_dc = findViewById(R.id.btn_dc);
        btn_stationary = findViewById(R.id.btn_stationary);
        btn_following = findViewById(R.id.btn_following);
        btn_application = findViewById(R.id.btn_application);
        txt_bluetooth = findViewById(R.id.txt_bluetooth);
        txt_mode = findViewById(R.id.txt_mode);
        latTextView = findViewById(R.id.latTextView);
        lonTextView = findViewById(R.id.lonTextView);
        mFusedLocationClient = LocationServices.getFusedLocationProviderClient(this);
        getLastLocation();

        // On startup, check if Bluetooth is on
        if(!bluetooth_adapter.isEnabled()){
            // If Bluetooth is not enables, ask the user to enable it
            Intent enable_bt = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enable_bt,SECONDS_30);
        }

        // Below are the 4 button handlers that are executed when the directional
        // buttons are held down. A handler is created to keep repeating an event
        // until the button is not being held down
        btn_right.setOnTouchListener(new View.OnTouchListener(){
            // Handler that is created in the event that button is held
            private Handler handler;

            // Method called when the button is held
            @Override public boolean onTouch(View v, MotionEvent m){
                // If the user is holding down the button
                if(m.getAction()==MotionEvent.ACTION_DOWN){
                    // If the button is held and the handler exist, do nothing
                    if(handler != null) return true;
                    // If the handler doesn't exist, create it and repeat action
                    handler = new Handler();
                    handler.postDelayed(repeated_action, DELAY_MS);
                }
                // Check if the user has let go of the button
                else if(m.getAction()==MotionEvent.ACTION_UP){
                    // If the handler doesn't exist, do nothing
                    if(handler == null) return true;
                    // If the handler exist, delete it and stop repeating action
                    handler.removeCallbacks(repeated_action);
                    handler = null;
                    click_event(true,CHAR_S);
                }
                return false;
            }

            // The event that is to be repeated while the button is held
            Runnable repeated_action = new Runnable() {
                @Override
                public void run() {
                    click_event(false,CHAR_R);
                    handler.postDelayed(repeated_action, DELAY_MS);
                }
            };

        });

        btn_left.setOnTouchListener(new View.OnTouchListener(){
            // Handler that is created in the event that button is held
            private Handler handler;

            // Method called when the button is held
            @Override public boolean onTouch(View v, MotionEvent m){
                // If the user is holding down the button
                if(m.getAction()==MotionEvent.ACTION_DOWN){
                    // If the button is held and the handler exist, do nothing
                    if(handler != null) return true;
                    // If the handler doesn't exist, create it and repeat action
                    handler = new Handler();
                    handler.postDelayed(repeated_action, DELAY_MS);
                }
                // Check if the user has let go of the button
                else if(m.getAction()==MotionEvent.ACTION_UP){
                    // If the handler doesn't exist, do nothing
                    if(handler == null) return true;
                    // If the handler exist, delete it and stop repeating action
                    handler.removeCallbacks(repeated_action);
                    handler = null;
                    click_event(true,CHAR_S);
                }
                return false;
            }

            // The event that is to be repeated while the button is held
            Runnable repeated_action = new Runnable() {
                @Override
                public void run() {
                    click_event(false,CHAR_L);
                    handler.postDelayed(repeated_action, DELAY_MS);
                }
            };

        });

        btn_up.setOnTouchListener(new View.OnTouchListener(){
            // Handler that is created in the event that button is held
            private Handler handler;

            // Method called when the button is held
            @Override public boolean onTouch(View v, MotionEvent m){
                // If the user is holding down the button
                if(m.getAction()==MotionEvent.ACTION_DOWN){
                    // If the button is held and the handler exist, do nothing
                    if(handler != null) return true;
                    // If the handler doesn't exist, create it and repeat action
                    handler = new Handler();
                    handler.postDelayed(repeated_action, DELAY_MS);
                }
                // Check if the user has let go of the button
                else if(m.getAction()==MotionEvent.ACTION_UP){
                    // If the handler doesn't exist, do nothing
                    if(handler == null) return true;
                    // If the handler exist, delete it and stop repeating action
                    handler.removeCallbacks(repeated_action);
                    handler = null;
                    click_event(true,CHAR_S);
                }
                return false;
            }

            // The event that is to be repeated while the button is held
            Runnable repeated_action = new Runnable() {
                @Override
                public void run() {
                    click_event(false,CHAR_F);
                    handler.postDelayed(repeated_action, DELAY_MS);
                }
            };

        });

        btn_down.setOnTouchListener(new View.OnTouchListener(){
            // Handler that is created in the event that button is held
            private Handler handler;

            // Method called when the button is held
            @Override public boolean onTouch(View v, MotionEvent m){
                // If the user is holding down the button
                if(m.getAction()==MotionEvent.ACTION_DOWN){
                    // If the button is held and the handler exist, do nothing
                    if(handler != null) return true;
                    // If the handler doesn't exist, create it and repeat action
                    handler = new Handler();
                    handler.postDelayed(repeated_action, DELAY_MS);
                }
                // Check if the user has let go of the button
                else if(m.getAction()==MotionEvent.ACTION_UP){
                    // If the handler doesn't exist, do nothing
                    if(handler == null) return true;
                    // If the handler exist, delete it and stop repeating action
                    handler.removeCallbacks(repeated_action);
                    handler = null;
                    click_event(true,CHAR_S);
                }
                return false;
            }

            // The event that is to be repeated while the button is held
            Runnable repeated_action = new Runnable() {
                @Override
                public void run() {
                    click_event(false,CHAR_B);
                    handler.postDelayed(repeated_action, DELAY_MS);
                }
            };

        });

    }

    // Method called when the Bluetooth button is pressed
    public void click_btn_bt(View v){
        click_event(true,CHAR_ON);
    }

    // Method called when the Disconnect button is pressed
    public void click_btn_dc(View v) {
        click_event(true,CHAR_OFF);
    }

    // Method called when the Stationary button is pressed
    public void click_btn_stationary(View v){
        click_event(true,CHAR_STATIONARY);
    }

    // Method called when the Following button is pressed
    public void click_btn_following(View v){
        click_event(true,CHAR_FOLLOWING);
    }

    // Method called when the Application button is pressed
    public void click_btn_application(View v){
        click_event(true,CHAR_APPLICATION);
    }

    public void click_event(boolean display, char c){
        // Check if Bluetooth is enabled
        if(bluetooth_adapter.isEnabled()) {
            // If Bluetooth is enabled, get a list of the known paired devices
            bluetooth_adapter = BluetoothAdapter.getDefaultAdapter();
            paired_devices = bluetooth_adapter.getBondedDevices();
            // Check the list of paired devices to find the Bluetooth module you want to connect to
            for (BluetoothDevice device : paired_devices) {
                if(device.getAddress().equals(MAC_ADDR)){
                    // Save the Bluetooth device once you find it
                    bluetooth_device = device;
                    uuids = bluetooth_device.getUuids();
                }
            }
            // If the Bluetooth button was pressed
            if(c == CHAR_ON){
                try {
                    // Try connect to the Bluetooth module
                    bluetooth_socket = bluetooth_device.createRfcommSocketToServiceRecord(uuids[0].getUuid());
                    bluetooth_socket.connect();
                    // If the phone was connected, display it onscreen
                    Toast.makeText(MainActivity.this,C_STRING, Toast.LENGTH_SHORT).show();
                    txt_bluetooth.setText(C_STRING);
                } catch (IOException e) {
                    Toast.makeText(MainActivity.this,AC_STRING, Toast.LENGTH_SHORT).show();
                }
            }
            // Check if the phone is connected to the Bluetooth module
            if(bluetooth_socket != null) {
                try {
                    // Create a connection to the Bluetooth's output data
                    output_data = bluetooth_socket.getOutputStream();
                    // If right button was pressed
                    if(c == CHAR_R){
                        // Output "Sent R" on screen if needs to be displayed
                        if(display)Toast.makeText(MainActivity.this, "Sent R", Toast.LENGTH_SHORT).show();
                        // Output the character "R" via Bluetooth
                        output_data.write("R".getBytes());
                    }
                    // If left button was pressed
                    else if(c == CHAR_L){
                        // Output "Sent L" on screen if needs to be displayed
                        if(display)Toast.makeText(MainActivity.this, "Sent L", Toast.LENGTH_SHORT).show();
                        // Output the character "R" via Bluetooth
                        output_data.write("L".getBytes());
                    }
                    // If Up button was pressed
                    else if(c == CHAR_F){
                        // Output "Sent F" on screen if needs to be displayed
                        if(display)Toast.makeText(MainActivity.this, "Sent F", Toast.LENGTH_SHORT).show();
                        // Output the character "F" via Bluetooth
                        output_data.write("F".getBytes());
                    }
                    // If Down button was pressed
                    else if(c == CHAR_B) {
                        // Output "Sent B" on screen if it needs to be displayed
                        if (display) Toast.makeText(MainActivity.this, "Sent B", Toast.LENGTH_SHORT).show();
                        // Output the character "B" via Bluetooth
                        output_data.write("B".getBytes());
                    }
                    // If Disconnect button was pressed
                    else if(c == CHAR_OFF) {
                        // Close the connection between Bluetooth module and phone
                        bluetooth_socket.close();
                        txt_bluetooth.setText(DC_STRING);
                        // Notify the user on the phone that the phone disconnected from the module
                        Toast.makeText(MainActivity.this, DC_STRING, Toast.LENGTH_SHORT).show();
                        bluetooth_socket = null;
                    }
                    else if(c == CHAR_S) {
                        // Output "Sent S" on screen if it needs to be displayed
                        if(display)Toast.makeText(MainActivity.this, "Sent S", Toast.LENGTH_SHORT).show();
                        // Output the character "S" via Bluetooth
                        output_data.write("S".getBytes());
                    }
                    // If Stationary button was pressed
                    else if(c == CHAR_STATIONARY) {
                        // Update mode onscreen
                        txt_mode.setText(STATIONARY_S);
                        // Output the character "1" via Bluetooth
                        output_data.write("1".getBytes());
                    }
                    // If Following button was pressed
                    else if(c == CHAR_FOLLOWING) {
                        // Update mode onscreen
                        txt_mode.setText(FOLLOWING_S);
                        // Output the character "2" via Bluetooth
                        output_data.write("2".getBytes());
                    }
                    // If Application button was pressed
                    else if(c == CHAR_APPLICATION) {
                        // Update mode onscreen
                        txt_mode.setText(APPLICATION_S);
                        // Output the character "2" via Bluetooth
                        output_data.write("3".getBytes());
                    }
                } catch (IOException e) {
                    // If an error uccurred while sending data via Bluetooth, notify the user
                    if(display && c == CHAR_R){
                        if(display)Toast.makeText(MainActivity.this, "Error sending R", Toast.LENGTH_SHORT).show();
                    }
                    else if(display && c == CHAR_L){
                        if(display)Toast.makeText(MainActivity.this, "Error sending L", Toast.LENGTH_SHORT).show();
                    }
                    else if(display && c == CHAR_F){
                        if(display)Toast.makeText(MainActivity.this, "Error sending F", Toast.LENGTH_SHORT).show();
                    }
                    else if(display && c == CHAR_B) {
                        if(display)Toast.makeText(MainActivity.this, "Error sending B", Toast.LENGTH_SHORT).show();
                    }
                    else if(display && c == CHAR_OFF) {
                        if(display)Toast.makeText(MainActivity.this, "Error Disconnecting", Toast.LENGTH_SHORT).show();
                    }
                    else if(display && c == CHAR_STATIONARY) {
                        if(display)Toast.makeText(MainActivity.this, "Error Stationing M", Toast.LENGTH_SHORT).show();
                    }
                    else if(display && c == CHAR_FOLLOWING) {
                        if(display)Toast.makeText(MainActivity.this, "Error Following M", Toast.LENGTH_SHORT).show();
                    }
                    else if(display && c == CHAR_APPLICATION) {
                        if(display)Toast.makeText(MainActivity.this, "Error Application M", Toast.LENGTH_SHORT).show();
                    }
                }
            }else {
                // If a button was pressed and the phone wasn't connected to the Bluetooth module notify the user
                if(display && c != CHAR_OFF)Toast.makeText(MainActivity.this, "Not connected", Toast.LENGTH_SHORT).show();
                // If the user is disconnected from Bluetooth module and tries to disconnect again, notify them
                else Toast.makeText(MainActivity.this, ADC_STRING, Toast.LENGTH_SHORT).show();
            }
        }else{
            // Notify the user if Bluetooth is off and they attempt to use it
            if(display)Toast.makeText(MainActivity.this, BT_OFF_STRING, Toast.LENGTH_SHORT).show();
        }
    }

    private boolean checkPermissions(){
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED &&
                ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED){
            return true;
        }
        return false;
    }

    private void requestPermissions(){
        ActivityCompat.requestPermissions(
                this,
                new String[]{Manifest.permission.ACCESS_COARSE_LOCATION, Manifest.permission.ACCESS_FINE_LOCATION},
                PERMISSION_ID
        );
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == PERMISSION_ID) {
            if(grantResults.length>0 && grantResults[0] == PackageManager.PERMISSION_GRANTED){
                // Granted. Start getting the location information
            }
        }
    }

    private boolean isLocationEnabled(){
        LocationManager locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        return locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER) || locationManager.isProviderEnabled(
                LocationManager.NETWORK_PROVIDER
        );
    }

    @SuppressLint("MissingPermission")
    private void getLastLocation(){
        if (checkPermissions()) {
            if (isLocationEnabled()) {
                mFusedLocationClient.getLastLocation().addOnCompleteListener(
                        new OnCompleteListener<Location>() {
                            @Override
                            public void onComplete(@androidx.annotation.NonNull Task<Location> task) {
                                Location location = task.getResult();
                                if (location == null) {
                                    requestNewLocationData();
                                } else {
                                    latTextView.setText(location.getLatitude()+"");
                                    lonTextView.setText(location.getLongitude()+"");
                                }
                            }
                        }
                );
            } else {
                Toast.makeText(this, "Turn on location", Toast.LENGTH_LONG).show();
                Intent intent = new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS);
                startActivity(intent);
            }
        } else {
            requestPermissions();
        }
    }

    @SuppressLint("MissingPermission")
    private void requestNewLocationData(){

        LocationRequest mLocationRequest = new LocationRequest();
        mLocationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
        mLocationRequest.setInterval(3000);
        mLocationRequest.setFastestInterval(1000);
        mLocationRequest.setNumUpdates(1);

        mFusedLocationClient = LocationServices.getFusedLocationProviderClient(this);
        mFusedLocationClient.requestLocationUpdates(
                mLocationRequest, mLocationCallback,
                Looper.myLooper()
        );

    }

    private LocationCallback mLocationCallback = new LocationCallback() {
        @Override
        public void onLocationResult(LocationResult locationResult) {
            Location mLastLocation = locationResult.getLastLocation();
            latTextView.setText(mLastLocation.getLatitude()+"");
            lonTextView.setText(mLastLocation.getLongitude()+"");
        }
    };

    @Override
    public void onResume(){
        super.onResume();
        if (checkPermissions()) {
            getLastLocation();
        }

    }

}
