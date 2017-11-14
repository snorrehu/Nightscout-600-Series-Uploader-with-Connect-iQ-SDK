package info.nightscout.connectiq;

import android.graphics.Color;
import android.os.Bundle;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.widget.TextView;


import com.garmin.android.connectiq.ConnectIQ;
import com.garmin.android.connectiq.IQApp;
import com.garmin.android.connectiq.IQDevice;
import com.garmin.android.connectiq.exception.InvalidStateException;
import com.garmin.android.connectiq.exception.ServiceUnavailableException;

import java.util.List;

import info.nightscout.android.R;
import info.nightscout.android.medtronic.MainActivity;

/**
 * Created by snorr on 06.10.2017.
 */

public class Connect_IQ_Activity extends AppCompatActivity {

    private static final String LOGGING = MainActivity.class.getSimpleName();

    //Object that handles communication with Connect IQ app
    ConnectIQ mConnectIQ;

    private IQDevice mDevice;
    private IQApp mApp;
    private boolean mSdkReady = false;

    //View connection status:
    private TextView connectStatusTextView;

    //Event listener for the Connect IQ Device (fitness watch):
    private ConnectIQ.IQDeviceEventListener mDeviceEventListener = new ConnectIQ.IQDeviceEventListener() {
        @Override
        public void onDeviceStatusChanged(IQDevice device, IQDevice.IQDeviceStatus status) {
            updateStatus(status);
        }
    };

    //Event listener for the Garmin mobile application:
    ConnectIQ.IQApplicationEventListener mAppEventListener = new ConnectIQ.IQApplicationEventListener() {
        @Override
        public void onMessageReceived(IQDevice iqDevice, IQApp iqApp, List<Object> message, ConnectIQ.IQMessageStatus iqMessageStatus) {
        }
    };

    //Listener to handle the states of the SDK:
    private ConnectIQ.ConnectIQListener mListener = new ConnectIQ.ConnectIQListener() {

        //Call when initialization fails:
        @Override
        public void onInitializeError(ConnectIQ.IQSdkErrorStatus errStatus) {
            if( null != connectStatusTextView)
                connectStatusTextView.setText(R.string.initialization_error + errStatus.name());
            mSdkReady = false;
        }

        //Do any post initialization setup:
        @Override
        public void onSdkReady() {
            loadDevices();
            mSdkReady = true;
        }

        @Override
        public void onSdkShutDown() {
            mSdkReady = false;
        }

    };


    @Override
    public void onCreate(Bundle savedInstanceState){
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_connectiq);

        // Device Connect IQ Application ID (manifest file).
        mApp = new IQApp("b36cddb3d31040b6ba72ca00d7cb2d85");

        //Set our text view:
        connectStatusTextView = (TextView)findViewById(R.id.connect_status);

        //Create our Connect IQ object. Change to wireless for BLE communication
        mConnectIQ = ConnectIQ.getInstance(this, ConnectIQ.IQConnectType.TETHERED);

        //Initialize the SDK:
        mConnectIQ.initialize(this,true, mListener);


    }

    //Method to update connection status:
    private void updateStatus(IQDevice.IQDeviceStatus status) {
        Log.d(LOGGING, "Updating Connect Status");
        switch(status) {
            case CONNECTED:
                connectStatusTextView.setText(R.string.status_connected);
                connectStatusTextView.setTextColor(Color.GREEN);
                break;
            case NOT_CONNECTED:
                connectStatusTextView.setText(R.string.status_not_connected);
                connectStatusTextView.setTextColor(Color.RED);
                break;
            case NOT_PAIRED:
                connectStatusTextView.setText(R.string.status_not_paired);
                connectStatusTextView.setTextColor(Color.RED);
                break;
            case UNKNOWN:
                connectStatusTextView.setText(R.string.status_unknown);
                connectStatusTextView.setTextColor(Color.RED);
                break;
        }
    }

    // Method for creating a list of known devices
    public void loadDevices() {
        try {
            //List paired devices:
            List<IQDevice> pairedDevices = mConnectIQ.getKnownDevices();

            if (pairedDevices != null && pairedDevices.size() > 0) {
                //Get the status of the devices:
                for (IQDevice device : pairedDevices) {
                    mConnectIQ.registerForEvents(device, mDeviceEventListener, mApp, mAppEventListener);
                    mDevice = device;
                    IQDevice.IQDeviceStatus status = mConnectIQ.getDeviceStatus(device);
                    updateStatus(status);
                }
            }
        } catch (InvalidStateException e) {
        } catch (ServiceUnavailableException e) {
            // Garmin Connect Mobile is not installed or needs to be upgraded.
            if (null != connectStatusTextView) {
                connectStatusTextView.setText(R.string.service_unavailable);
            }
        }
    }


}
