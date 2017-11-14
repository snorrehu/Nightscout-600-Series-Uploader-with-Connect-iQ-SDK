/*
package info.nightscout.android.connectiq;

import android.content.Context;
import android.graphics.Color;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.BaseAdapter;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;


import com.garmin.android.connectiq.ConnectIQ;
import com.garmin.android.connectiq.IQApp;
import com.garmin.android.connectiq.IQDevice;
import com.garmin.android.connectiq.exception.InvalidStateException;
import com.garmin.android.connectiq.exception.ServiceUnavailableException;
import com.mikepenz.google_material_typeface_library.GoogleMaterial;
import com.mikepenz.iconics.IconicsDrawable;

import org.w3c.dom.Text;

import java.util.List;

import info.nightscout.android.R;
import info.nightscout.android.medtronic.MainActivity;
import io.realm.Realm;

import static android.R.attr.data;
import static android.R.attr.onClick;

*/
/**
 * Created by snorr on 06.10.2017.
 *//*


public class Connect_IQ_Activity extends AppCompatActivity {

    private static final String TAG= MainActivity.class.getSimpleName();
    private Realm mRealm;

    //Object that handles communication with Connect IQ app
    ConnectIQ mConnectIQ;
    private IQDevice mDevice;
    private IQApp mApp;
    private boolean mSdkReady = false;

    List<IQDevice> pairedDevices;
    private ListView mListView;


    //Event listener for the Connect IQ Device (fitness watch):
    private ConnectIQ.IQDeviceEventListener mDeviceEventListener = new ConnectIQ.IQDeviceEventListener() {
        @Override
        public void onDeviceStatusChanged(IQDevice device, IQDevice.IQDeviceStatus status) {
            Log.d(TAG,"onDeviceStatusChanged");
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

        //Create toolbar:
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);

        if (toolbar != null) {
            setSupportActionBar(toolbar);
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setHomeAsUpIndicator(
                    new IconicsDrawable(this)
                            .icon(GoogleMaterial.Icon.gmd_arrow_back)
                            .color(Color.WHITE)
                            .sizeDp(24)
            );
            getSupportActionBar().setElevation(0);
            getSupportActionBar().setTitle("Manage your devices");
        }
        mRealm = Realm.getDefaultInstance();

        // Device Connect IQ Application ID (manifest file).
        mApp = new IQApp("b36cddb3d31040b6ba72ca00d7cb2d85");

        //Set our text view:
        connectStatusTextView = (TextView)findViewById(R.id.connect_status);

        //Create our Connect IQ object. Change to wireless for BLE communication
        mConnectIQ = ConnectIQ.getInstance(this, ConnectIQ.IQConnectType.TETHERED);

        //Initialize the SDK:
        mConnectIQ.initialize(this,true, mListener);

    }

    @Override
    protected void onResume() {
        super.onResume();
        Log.d(TAG, "onResume called");
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                // avoid memory leaks
                mRealm = null;
                finish();
                break;
        }
        return true;
    }

    //Method to update connection status:
    private void updateStatus(IQDevice.IQDeviceStatus status) {
        Log.d(TAG, "Updating Connect Status");
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
        Log.d(TAG,"loadDevices called");
        try {
            //Make list of all paired devices (if there are any):
            pairedDevices = mConnectIQ.getKnownDevices();
            if (pairedDevices != null && pairedDevices.size() > 0) {

                for (IQDevice device : pairedDevices) {
                    Log.d(TAG,"loadDevices: Device added: " + device + " ,Status: " + device.getStatus().toString());
                }
                //Create a clickable listview of all paired devices, and add custom adapter:
                mListView = (ListView)findViewById(R.id.lvAvailableDevices);
                ArrayAdapter<IQDevice> lvAdapter = new ArrayAdapter<IQDevice>(this,android.R.layout.simple_list_item_1,pairedDevices);
                mListView.setAdapter(lvAdapter);

                mListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                    @Override
                    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                        //Delete the existing device
                        mDevice = null;
                        Log.d(TAG, "mListView: item clicked.");
                        Log.d(TAG,"mListView: ststus: " + pairedDevices.get(position).getStatus());
                        IQDevice.IQDeviceStatus status = pairedDevices.get(position).getStatus();
                        mDevice = pairedDevices.get(position);
                        try {
                            mConnectIQ.registerForEvents(mDevice,mDeviceEventListener,mApp,mAppEventListener);
                        } catch (InvalidStateException e) {
                            e.printStackTrace();
                        }
                        updateStatus(status);
                        if (status == IQDevice.IQDeviceStatus.CONNECTED ){
                            Log.d(TAG,"loadDevices: Connected item clicked.");
                            Toast.makeText(getApplicationContext(), "Device connected! CGM service started.",Toast.LENGTH_SHORT).show();
                        }else{
                            Log.d(TAG, "loadDevices: Non connected item clicked.");
                            Toast.makeText(getApplicationContext(), "Device not connected.",Toast.LENGTH_SHORT).show();
                        }
                    }
                });
            }

        }
    }

    public void connectIQCGMService(String sgv){
        try {
            mConnectIQ.sendMessage(mDevice,mApp,sgv, new ConnectIQ.IQSendMessageListener(){
                @Override
                public void onMessageStatus(IQDevice iqDevice, IQApp iqApp, ConnectIQ.IQMessageStatus iqMessageStatus) {
                    //Something....
                }
            });
        } catch (InvalidStateException e) {
            Toast.makeText(this, "ConnectIQ is not in a valid state", Toast.LENGTH_LONG).show();
        } catch (ServiceUnavailableException e) {
            Toast.makeText(this, "ConnectIQ service is unavailable.", Toast.LENGTH_LONG).show();
        }
    }
}
*/
