package info.nightscout.android.connectiq;

import android.app.IntentService;
import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.support.annotation.Nullable;
import android.util.Log;
import android.widget.TextView;
import android.widget.Toast;

import com.garmin.android.connectiq.ConnectIQ;
import com.garmin.android.connectiq.IQApp;
import com.garmin.android.connectiq.IQDevice;
import com.garmin.android.connectiq.exception.InvalidStateException;
import com.garmin.android.connectiq.exception.ServiceUnavailableException;

import java.util.List;

import info.nightscout.android.R;
import info.nightscout.android.medtronic.MainActivity;
import info.nightscout.android.model.medtronicNg.PumpStatusEvent;
import info.nightscout.android.utils.ConfigurationStore;
import info.nightscout.android.utils.DataStore;

/**
 * Created by snorr on 09.10.2017.
 */

public class Connect_IQ_Message_Service extends IntentService  {

    private static final String TAG= Connect_IQ_Message_Service.class.getSimpleName();
    //Default constructor:
    public Connect_IQ_Message_Service() {
        super("DisplayNotification");
    }

    private ConnectIQ mConnectIQ;
    private IQApp mIQApp;
    private boolean mSdkReady;
    List<IQDevice> pairedDevices;
    private String sgvString;
    private DataStore dataStore;


    //Listener to handle the states of the SDK:
    private ConnectIQ.ConnectIQListener mListener = new ConnectIQ.ConnectIQListener() {

        //Call when initialization fails:
        @Override
        public void onInitializeError(ConnectIQ.IQSdkErrorStatus errStatus) {
            mSdkReady = false;
            Log.d(TAG, "ConnectIQListener: sdk initialization error");
        }

        //Called when the sdk is successfully initialized:
        @Override
        public void onSdkReady() {
            loadDevices();
            mSdkReady = true;
            Log.d(TAG, "ConnectIQListener: sdk initialized");
        }

        //Called when sdk is shut down:
        @Override
        public void onSdkShutDown() {
            mSdkReady = false;
            Log.d(TAG, "ConnectIQListener: sdk shut down");
        }

    };

    @Override
    public void onCreate() {
        super.onCreate();
        Log.d(TAG,"Service started");

        // Get the most recently written CGM record for the active pump.
        PumpStatusEvent pumpStatusData = null;
        ConfigurationStore configurationStore = ConfigurationStore.getInstance();
        dataStore = dataStore.getInstance();


        if (dataStore.getLastPumpStatus().getEventDate().getTime() > 0) {
            pumpStatusData = dataStore.getLastPumpStatus();
        }

        if (pumpStatusData != null) {
            if (pumpStatusData.isCgmActive()) {
                sgvString = MainActivity.strFormatSGV(pumpStatusData.getSgv());

                //Reformat the string so that connect iq recognizes it as a floating number:
                sgvString = sgvString.replaceAll(",",".");

                Log.d(TAG,"onCreate: CGM active. SgvValue = " + sgvString);
            } else {
                sgvString = "-";
                Log.d(TAG,"onCreate: CGM not active. SgvValue = " + sgvString);
            }
        }else{
            Log.d(TAG,"onCreate: pumpStatusData = null. SgvValue = " + MainActivity.strFormatSGV(pumpStatusData.getSgv()));
        }

        // Device Connect IQ Application ID (manifest file).
        mIQApp = new IQApp("550399c206ba496195222351d11c256a");

        //Create our Connect IQ object. Change to wireless for BLE communication
        mConnectIQ = ConnectIQ.getInstance(this, ConnectIQ.IQConnectType.WIRELESS);

        //Initialize the SDK:
        mConnectIQ.initialize(this,true, mListener);

    }

    @Override
    public void onDestroy() {

        try {
            mConnectIQ.unregisterAllForEvents();
        } catch (InvalidStateException e) {
            e.printStackTrace();
        }
        super.onDestroy();
    }


    @Override
    protected void onHandleIntent(@Nullable Intent intent) {
        //If the sdk is initialized and there are connected devices in our list, send sgv updates to every device:
        if(mSdkReady) {
            Log.d(TAG,"onHandleIntent: sdk ready");
            if (pairedDevices != null && pairedDevices.size() > 0) {
                Log.d(TAG,"onHandleIntent: Paired devices = " + pairedDevices);
                for (IQDevice device : pairedDevices) {
                    Log.d(TAG,"Device status: " + device.getStatus());
                    sendSgvUpdateToGarmin(device, sgvString);
                }
            }else{
                Log.d(TAG,"onHandleIntent: No connected devices. SgvString = " + sgvString);
            }
        }else{
            Log.d(TAG,"onHandleIntent: sdk not ready");
        }
    }

    //Method that sends messages to the device:
    private void sendSgvUpdateToGarmin(IQDevice device, String sgvString) {
        try {
            mConnectIQ.sendMessage(device, mIQApp, sgvString, new ConnectIQ.IQSendMessageListener() {
                @Override
                public void onMessageStatus(IQDevice iqDevice, IQApp iqApp, ConnectIQ.IQMessageStatus iqMessageStatus) {
                    Log.d(TAG,"sendSGVUpdateToGarmin: " + iqMessageStatus);

                }
            });
        } catch (InvalidStateException e) {
            Log.d(TAG,"sendSGVUpdateToGarmin: No connection");
            e.printStackTrace();
        } catch (ServiceUnavailableException e) {
            Log.d(TAG,"sendSGVUpdateToGarmin: No connection");
            e.printStackTrace();
        }
        Log.d(TAG,"SGV data sendt: " + sgvString);
    }

    public void loadDevices() {
        try {
            pairedDevices = mConnectIQ.getKnownDevices();
            Log.d(TAG,"loadDevices: known devices: " + pairedDevices);
        } catch (InvalidStateException e) {
            e.printStackTrace();
        } catch (ServiceUnavailableException e) {
            e.printStackTrace();
        }
    }

}