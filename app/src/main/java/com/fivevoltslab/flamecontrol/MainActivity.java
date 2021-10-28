package com.fivevoltslab.flamecontrol;

import android.Manifest;
import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.view.animation.LinearInterpolator;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.app.ActivityCompat;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import androidx.preference.PreferenceManager;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.snackbar.Snackbar;
import com.skydoves.colorpickerview.ActionMode;
import com.skydoves.colorpickerview.ColorPickerView;
import com.skydoves.colorpickerview.listeners.ColorEnvelopeListener;

import java.nio.charset.StandardCharsets;
import java.util.Objects;

public class MainActivity extends AppCompatActivity {
    String hexColor;
    String hexColorStringForSending = "#FFFFFF";

    String hexColorStringForToast;

    Toolbar mainActivityToolbar;
    private Menu menu;

    private static final int REQUEST_SELECT_DEVICE = 1;
    private static final int REQUEST_ENABLE_BT = 2;
    private static final int REQUEST_ACCESS_FINE_LOCATION = 3;

    public static final String TAG = "MainActivity";
    private static final int UART_PROFILE_CONNECTED = 20;
    private static final int UART_PROFILE_DISCONNECTED = 21;

    private int mState = UART_PROFILE_DISCONNECTED;
    private UartService mService = null;
    private BluetoothDevice mDevice = null;
    private BluetoothAdapter mBtAdapter = null;
    String deviceAddress;

    FloatingActionButton sendButton;
    ImageView blinkImgView;
    Animation animation;

    SharedPreferences sharedPreferences;
    private Button brightnessButton;
    private static final String BRIGHTNESS_PREF_KEY = "brightness_key";
    private int brightnessValue;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);


        mBtAdapter = BluetoothAdapter.getDefaultAdapter();
        if (mBtAdapter == null) {
            Toast.makeText(this, "Bluetooth is not available", Toast.LENGTH_LONG).show();
            finish();
            return;
        }

        service_init();

        mainActivityToolbar = findViewById(R.id.mainActivityToolbar);
        setSupportActionBar(mainActivityToolbar);

        sendButton = findViewById(R.id.fab);
        sendButton.setOnClickListener(view -> {
            if (mState == UART_PROFILE_DISCONNECTED) {
                Snackbar.make(view, "Please connect to a device", Snackbar.LENGTH_SHORT)
                        .setAction("Action", null).show();

            } else {
                sendColorCode();

            }


        });

        blinkImgView = findViewById(R.id.blinkImgView);


        ColorPickerView colorPickerView = findViewById(R.id.colorPick);
        colorPickerView.setPaletteDrawable(Objects.requireNonNull(getDrawable(R.drawable.colorful_flower)));
        colorPickerView.setActionMode(ActionMode.LAST);
        colorPickerView.setColorListener((ColorEnvelopeListener) (envelope, fromUser) -> {

            hexColor = String.format("#%06X", (0xFFFFFF & envelope.getColor()));

            hexColorStringForToast = String.format("#%06X", (0xFFFFFF & envelope.getColor()));

            mainActivityToolbar.setBackgroundColor(envelope.getColor());
        });

        startBlinkingImgView(); //Start blinking Red Circle in ImageView

        brightnessButton = findViewById(R.id.brightnessButton);

        if (sharedPreferences.getInt(BRIGHTNESS_PREF_KEY, -1) != -1) {
            switch (sharedPreferences.getInt(BRIGHTNESS_PREF_KEY, -1)) {
                case 1:
                    brightnessButton.setText(R.string.brightness_low);
                    break;
                case 2:
                    brightnessButton.setText(R.string.brightness_medium);
                    break;
                case 3:
                    brightnessButton.setText(R.string.brightness_high);
                    break;
            }
        } else {
            brightnessButton.setText(R.string.brightness_high);
            sharedPreferences.edit().putInt(BRIGHTNESS_PREF_KEY, 3).apply();

        }

        brightnessButton.setOnClickListener(v -> {
            if (sharedPreferences.getInt(BRIGHTNESS_PREF_KEY, -1) == 3) {
                brightnessValue = 2;
                brightnessButton.setText(R.string.brightness_medium);
            } else if (sharedPreferences.getInt(BRIGHTNESS_PREF_KEY, -1) == 2) {
                brightnessValue = 1;
                brightnessButton.setText(R.string.brightness_low);
            } else if (sharedPreferences.getInt(BRIGHTNESS_PREF_KEY, -1) == 1) {
                brightnessValue = 3;
                brightnessButton.setText(R.string.brightness_high);
            }
            sharedPreferences.edit().putInt(BRIGHTNESS_PREF_KEY, brightnessValue).apply();
        });

        /***
         1 = LOW
         2 = MEDIUM
         3 = HIGH
         */

    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_main, menu);
        this.menu = menu;
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        /********************************************************************/
        if (id == R.id.menu_flame) {

            if (mState == UART_PROFILE_DISCONNECTED) {
                showMessage("Please connect to a device", Toast.LENGTH_SHORT);

            } else {
                Log.d(TAG, "Color: " + hexColorStringForSending);
                sendColorCode();
            }

            return true;
        }
        /********************************************************************/
        else if (id == R.id.menu_settings) {
            Intent settingsIntent = new Intent(MainActivity.this, SettingsActivity.class);
            startActivity(settingsIntent);

            return true;
        }

        /********************************************************************/
        else if (id == R.id.menu_connect) {

            if (!mBtAdapter.isEnabled()) {
                Intent enableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                startActivityForResult(enableIntent, REQUEST_ENABLE_BT);
            } else {

                if (ActivityCompat.checkSelfPermission(MainActivity.this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(MainActivity.this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                    ActivityCompat.requestPermissions(MainActivity.this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, REQUEST_ACCESS_FINE_LOCATION);
                } else {
                    if (mState != UART_PROFILE_CONNECTED) {
                        Intent newIntent = new Intent(MainActivity.this, ScanDeviceActivity.class);
                        startActivityForResult(newIntent, REQUEST_SELECT_DEVICE);
                    } else {
                        if (mDevice != null) {
                            mService.disconnect();
                            showMessage("Disconnected from " + deviceAddress + ".", Toast.LENGTH_SHORT);
                        }
                    }
                }

            }

            return true;
        }

        return super.onOptionsItemSelected(item);

    }

    private void showMessage(String msg, int toastLength) {
        Toast.makeText(this, msg, toastLength).show();

    }

    private ServiceConnection mServiceConnection = new ServiceConnection() {
        public void onServiceConnected(ComponentName className, IBinder rawBinder) {
            mService = ((UartService.LocalBinder) rawBinder).getService();

            Log.d(TAG, "onServiceConnected mService= " + mService);
            if (!mService.initialize()) {
                Log.e(TAG, "Unable to initialize Bluetooth");
                finish();
            }


        }

        public void onServiceDisconnected(ComponentName classname) {
            ////     mService.disconnect(mDevice);
            mService = null;
        }
    };

    private void sendColorCode() {
        int brightnessValue = 3;
        if (sharedPreferences.getInt(BRIGHTNESS_PREF_KEY, -1) != -1) {
            brightnessValue = sharedPreferences.getInt(BRIGHTNESS_PREF_KEY, 3);

        }
        hexColorStringForSending = "{\"hex_color\":\"" + hexColor + "\",\"b\":\"" + brightnessValue + "\" }";

        byte[] hexColorByte;
        hexColorByte = hexColorStringForSending.getBytes(StandardCharsets.UTF_8);
        mService.writeRXCharacteristic(hexColorByte);
        Toast.makeText(MainActivity.this, "Color: \"" + hexColorStringForToast + "\" sent!", Toast.LENGTH_SHORT).show();


    }

    private void startBlinkingImgView() {
        animation = new AlphaAnimation(1, 0); //to change visibility from visible to invisible
        animation.setDuration(500); //0.5 second duration for each animation cycle
        animation.setInterpolator(new LinearInterpolator());
        animation.setRepeatCount(Animation.INFINITE); //repeating indefinitely
        animation.setRepeatMode(Animation.REVERSE); //animation will start from end point once ended.
        blinkImgView.startAnimation(animation);

    }

    private void stopBlinkingImgView() {
        animation.cancel();
    }

    private Handler mHandler = new Handler() {
        @Override

        //Handler events that received from UART service
        public void handleMessage(Message msg) {

        }
    };

    private final BroadcastReceiver UARTStatusChangeReceiver = new BroadcastReceiver() {

        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();

            final Intent mIntent = intent;
            //*********************//
            if (action.equals(UartService.ACTION_GATT_CONNECTED)) {
                runOnUiThread(() -> {
                    Log.d(TAG, "UART_CONNECT_MSG");
                    mState = UART_PROFILE_CONNECTED;
                    menu.getItem(1).setIcon(getDrawable(R.drawable.connect_icon_red));
                    stopBlinkingImgView(); //Stop blinking Red Circle in ImageView
                    showMessage("Connected to " + deviceAddress + ".", Toast.LENGTH_SHORT);
                });
            }

            //*********************//
            if (action.equals(UartService.ACTION_GATT_DISCONNECTED)) {
                runOnUiThread(() -> {
                    Log.d(TAG, "UART_DISCONNECT_MSG");
                    mState = UART_PROFILE_DISCONNECTED;
                    mService.close();
                    menu.getItem(1).setIcon(getDrawable(R.drawable.connect_icon_white));
                    startBlinkingImgView(); //Start blinking Red Circle in ImageView
                });
            }


            //*********************//
            if (action.equals(UartService.ACTION_GATT_SERVICES_DISCOVERED)) {
                mService.enableTXNotification();
            }
            //*********************//
            if (action.equals(UartService.ACTION_DATA_AVAILABLE)) {

                final byte[] txValue = intent.getByteArrayExtra(UartService.EXTRA_DATA);
                runOnUiThread(() -> {
                    try {


                        String text = new String(txValue, StandardCharsets.UTF_8);

                        Log.d(TAG, "" + text);


                    } catch (Exception e) {
                        Log.e(TAG, e.toString());
                        e.printStackTrace();

                    }
                });
            }
            //*********************//
            if (action.equals(UartService.DEVICE_DOES_NOT_SUPPORT_UART)) {
                showMessage("Device doesn't support UART. Disconnecting", Toast.LENGTH_SHORT);
                mService.disconnect();
            }


        }
    };


    private void service_init() {
        Intent bindIntent = new Intent(this, UartService.class);
        bindService(bindIntent, mServiceConnection, Context.BIND_AUTO_CREATE);

        LocalBroadcastManager.getInstance(this).registerReceiver(UARTStatusChangeReceiver, makeGattUpdateIntentFilter());
    }

    private static IntentFilter makeGattUpdateIntentFilter() {
        final IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(UartService.ACTION_GATT_CONNECTED);
        intentFilter.addAction(UartService.ACTION_GATT_DISCONNECTED);
        intentFilter.addAction(UartService.ACTION_GATT_SERVICES_DISCOVERED);
        intentFilter.addAction(UartService.ACTION_DATA_AVAILABLE);
        intentFilter.addAction(UartService.DEVICE_DOES_NOT_SUPPORT_UART);
        return intentFilter;
    }

    @Override
    public void onStart() {
        super.onStart();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.d(TAG, "onDestroy()");

        try {
            LocalBroadcastManager.getInstance(this).unregisterReceiver(UARTStatusChangeReceiver);
        } catch (Exception ignore) {
            Log.e(TAG, ignore.toString());
        }
        unbindService(mServiceConnection);
        mService.stopSelf();
        mService = null;

    }

    @Override
    protected void onStop() {
        Log.d(TAG, "onStop");
        super.onStop();
    }


    @Override
    protected void onRestart() {
        super.onRestart();


        Log.d(TAG, "onRestart");
    }


    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
    }


    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        switch (requestCode) {

            case REQUEST_SELECT_DEVICE:
                //When the DeviceListActivity return, with the selected device address
                if (resultCode == Activity.RESULT_OK && data != null) {
                    deviceAddress = data.getStringExtra(BluetoothDevice.EXTRA_DEVICE);
                    mDevice = BluetoothAdapter.getDefaultAdapter().getRemoteDevice(deviceAddress);

                    mService.connect(deviceAddress);
                }
                break;
            case REQUEST_ENABLE_BT:
                // When the request to enable Bluetooth returns
                if (resultCode == Activity.RESULT_OK) {
                    Toast.makeText(this, "Bluetooth has turned on ", Toast.LENGTH_SHORT).show();

                } else {
                    // User did not enable Bluetooth or an error occurred
                    Log.d(TAG, "BT not enabled");
                    Toast.makeText(this, "Problem in BT Turning ON ", Toast.LENGTH_SHORT).show();
                }
                break;
            default:
                Log.e(TAG, "wrong request code");
                break;
        }
    }


}

