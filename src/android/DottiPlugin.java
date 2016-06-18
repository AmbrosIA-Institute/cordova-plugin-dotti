
package institute.ambrosia.plugins;

import org.apache.cordova.CordovaWebView;
import org.apache.cordova.CallbackContext;
import org.apache.cordova.CordovaPlugin;
import org.apache.cordova.CordovaInterface;
import org.apache.cordova.PluginResult;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.Service;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothProfile;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanResult;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.graphics.Color;
import android.os.Binder;
import android.os.IBinder;
import android.provider.ContactsContract;
import android.util.Log;
import android.os.Handler;


import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.UUID;
import java.util.concurrent.Callable;



class ActionFilterGatt {
    public final static String ACTION_GATT_CONNECTED           = "Dotti-ACTION_GATT_CONNECTED";
    public final static String ACTION_GATT_DISCONNECTED        = "Dotti-ACTION_GATT_DISCONNECTED";
    public final static String ACTION_GATT_SERVICES_DISCOVERED = "Dotti-ACTION_GATT_SERVICES_DISCOVERED";
    public final static String ACTION_DATA_AVAILABLE           = "Dotti-ACTION_DATA_AVAILABLE";

    static IntentFilter makeFilter() {
        final IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(ACTION_GATT_CONNECTED);
        intentFilter.addAction(ACTION_GATT_DISCONNECTED);
        intentFilter.addAction(ACTION_GATT_SERVICES_DISCOVERED);
        intentFilter.addAction(ACTION_DATA_AVAILABLE);
        return intentFilter;
    }
}



public class DottiPlugin extends CordovaPlugin {

    public static final String TAG = "Dotti Plugin";
    private BluetoothAdapter mBluetoothAdapter = null;
    private final static int REQUEST_ENABLE_BT = 1;
    private Handler mHandler = new Handler();
    //private ArrayList<BluetoothDevice> devices = new ArrayList<>();
    private HashMap<String, BluetoothDevice> devices = new HashMap<>();
    private HashMap<String, BluetoothGatt> gatts = new HashMap<>();
    private Activity activity;


    public DottiPlugin() {
    }


    public void initialize(CordovaInterface cordova, CordovaWebView webView) {
        super.initialize(cordova, webView);
        Log.v(TAG, "Init DottiPlugin");
        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        activity = cordova.getActivity();
        activity.registerReceiver(mGattUpdateReceiver, ActionFilterGatt.makeFilter());
        refresh();
    }


    private void refresh() {
        if (!mBluetoothAdapter.isEnabled()) {
            try_enable_bt();
        }
        if (mBluetoothAdapter.isEnabled()) {
            scanLeDevice(true);
        }
    }


    private void scanLeDevice(final boolean enable) {
        final BluetoothAdapter.LeScanCallback mLeScanCallback =
                new BluetoothAdapter.LeScanCallback() {
                    @Override
                    public void onLeScan(final BluetoothDevice device, int rssi,
                                         byte[] scanRecord) {
                        String name = device.getName();
                        String address = device.getAddress();
                        Log.i(TAG, "SCAN CALLBACK");
                        Log.i(TAG, "Address: " + device.getAddress());
                        Log.i(TAG, "Name: " + device.getName());
                        if (name != null) {
                            if (name.matches("Dotti")) {
                                devices.put(address, device);
                                Context context = activity.getApplicationContext();
                                gatts.put(address, device.connectGatt(context, false, gattCallback));
                            }
                        }
                    }
                };
        if (enable) {
            // Stops scanning after a pre-defined scan period.
            mHandler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    mBluetoothAdapter.stopLeScan(mLeScanCallback);
                }
            }, 10000);
            //devices.clear();
            mBluetoothAdapter.startLeScan(mLeScanCallback);
        } else {
            mBluetoothAdapter.stopLeScan(mLeScanCallback);
        }
    }


    private void try_enable_bt() {
        Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
        cordova.setActivityResultCallback(this);
        cordova.getActivity().startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
    }


    private final BluetoothGattCallback gattCallback = new BluetoothGattCallback() {
        @Override
        public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
            Log.i("onConnectionStateChange", "Status: " + status);
            switch (newState) {
                case BluetoothProfile.STATE_CONNECTED:
                    Log.i("gattCallback", "STATE_CONNECTED");
                    gatt.discoverServices();
                    break;
                case BluetoothProfile.STATE_DISCONNECTED:
                    Log.e("gattCallback", "STATE_DISCONNECTED");
                    break;
                default:
                    Log.e("gattCallback", "STATE_OTHER");
            }

        }

        @Override
        public void onServicesDiscovered(BluetoothGatt gatt, int status) {
            List<BluetoothGattService> services = gatt.getServices();
            Log.i("onServicesDiscovered", services.toString());
            gatt.readCharacteristic(services.get(1).getCharacteristics().get
                    (0));
        }

        @Override
        public void onCharacteristicRead(BluetoothGatt gatt,
                                         BluetoothGattCharacteristic
                                                 characteristic, int status) {
            Log.i("onCharacteristicRead", characteristic.toString());
            gatt.disconnect();
        }
    };


    private void doListDevices(final CallbackContext ctx) {
        JSONArray packet = new JSONArray();
        Iterator iterator = devices.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry mentry = (Map.Entry) iterator.next();
            BluetoothDevice device = (BluetoothDevice) mentry.getValue();
            String address = (String) mentry.getKey();
            Log.i(TAG, "ADD DEVICE TO LIST");
            JSONObject item = new JSONObject();
            try {
                item.put("address", "");
                item.put("name", "");
                item.put("address", device.getAddress());
                item.put("name", device.getName());
                Log.i(TAG, "Address: " + device.getAddress());
                Log.i(TAG, "Name: " + device.getName());
                packet.put(item);
            } catch (JSONException e) {
            }

        }
        ctx.sendPluginResult(new PluginResult(PluginResult.Status.OK, packet));
    }


    private final BroadcastReceiver mGattUpdateReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            final String action = intent.getAction();
            if (ActionFilterGatt.ACTION_GATT_CONNECTED.equals(action)) {
                Log.i(TAG, "Device connected");
            } else if (ActionFilterGatt.ACTION_GATT_DISCONNECTED.equals(action)) {
                Log.i(TAG, "Device disconnected");
            } else if (ActionFilterGatt.ACTION_GATT_SERVICES_DISCOVERED.equals(action)) {
                Log.i(TAG, "Device connected && service discovered");
                //device_list_view.getChildAt(list_item_position).setBackgroundColor(Color.BLUE);
                //invalidateOptionsMenu();
                //service has been discovered on device => you can address directly the device
                ArrayList<String> actionsStr = intent.getStringArrayListExtra("");
                if (actionsStr.size() > 0) {
                    try {
                        JSONObject mainObject = new JSONObject(actionsStr.get(0));
                        if (mainObject.has("address") && mainObject.has("deviceName") && mainObject.has("deviceName")) {
                            Log.i(TAG, "Setting for device = > " + mainObject.getString("address") + " - " + mainObject.getString("deviceName") + " - " + mainObject.getString("deviceName"));
                            // Intent intentDevice = new Intent(DottiActivity.this, DottiDeviceActivity.class);
                            //intentDevice.putExtra("deviceAddr", mainObject.getString("address"));
                            //intentDevice.putExtra("deviceName", mainObject.getString("deviceName"));
                            //toSecondLevel=true;
                            //startActivity(intentDevice);
                        }
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                }
            } else if (ActionFilterGatt.ACTION_DATA_AVAILABLE.equals(action)) {
                if (intent.getStringArrayListExtra("STATUS") != null) {
                    ArrayList<String> values = intent.getStringArrayListExtra("STATUS");
                }
            }
        }
    };


    public static BluetoothGattCharacteristic getCharacteristic(List<BluetoothGattService> serviceList, String characteristicUid) {
        for (int i = 0; i < serviceList.size(); i++) {
            for (int j = 0; j < serviceList.get(i).getCharacteristics().size(); j++) {
                if (serviceList.get(i).getCharacteristics().get(j).getUuid().toString().equals(characteristicUid)) {
                    return serviceList.get(i).getCharacteristics().get(j);
                }
            }
        }
        return null;
    }


    private void testMethod() {
        BluetoothDevice device = getDeviceByIndex(0);
        String address = device.getAddress();
        try {
            BluetoothGatt gatt = getGattByMacAddress(address);
            String dotti_charac = "0000fff3-0000-1000-8000-00805f9b34fb";
            BluetoothGattCharacteristic characteristic = getCharacteristic(gatt.getServices(), dotti_charac);
            int pixelId = 5;
            int red = 120;
            int green = 200;
            int blue = 50;
            byte[] value = new byte[]{(byte) 7, (byte) 2, (byte) (pixelId + 1), (byte) red, (byte) green, (byte) blue};
            characteristic.setValue(value);
            gatt.writeCharacteristic(characteristic);
        } catch (Exception e) {
        }
    }


    private void writeMainCharacteristic(String address, byte[] value) {
        BluetoothGatt gatt = getGattByMacAddress(address);
        String dotti_charac = "0000fff3-0000-1000-8000-00805f9b34fb";
        BluetoothGattCharacteristic characteristic = getCharacteristic(gatt.getServices(), dotti_charac);
        characteristic.setValue(value);
        gatt.writeCharacteristic(characteristic);
    }


    public void setPixelRGB(String address, int pixelId, int red, int green, int blue) {
        byte[] value = new byte[]{(byte) 7, (byte) 2, (byte) (pixelId + 1), (byte) red, (byte) green, (byte) blue};
        writeMainCharacteristic(address, value);
    }


    public void saveCurrentIcon(String address, int iconId) {
        byte[] value;
        if (iconId >= 0 && iconId <= 7) {

            if (iconId == 0) {
                value = new byte[]{(byte) 6, (byte) 7, (byte) 0, (byte) 0};
                writeMainCharacteristic(address, value);
            } else {
                int id = 0b10000000 + (iconId << 4);
                System.out.println("ID => " + id);
                value = new byte[]{(byte) 6, (byte) 7, (byte) 2, (byte) id};
                writeMainCharacteristic(address, value);
            }
        } else {
            Log.e(TAG, "Error animation id must be between 0 and 7");
        }
    }


    public void showIcon(String address, int iconId) {
        byte[] value;
        if (iconId >= 0 && iconId <= 7) {
            if (iconId == 0) {
                value = new byte[]{(byte) 6, (byte) 8, (byte) 0, (byte) 0};
                writeMainCharacteristic(address, value);
            } else {
                int id = 0b10000000 + (iconId << 4);
                System.out.println("ID => " + id);
                value = new byte[]{(byte) 6, (byte) 8, (byte) 2, (byte) id};
                writeMainCharacteristic(address, value);
            }
        } else {
            Log.e(TAG, "Error animation id must be between 0 and 7");
        }
    }


    public void initIcons(String address) {
        int[] icons = new int[]{0, 15, 55, 95, 135, 175, 215, 255};
        for (int iconIdx = 0; iconIdx < 8; iconIdx++) {
            int value = icons[iconIdx];
            for (int i = 0; i <= 63; i++) {
                setPixelRGB(address, i, value, value, value);
            }
        }
    }


    private BluetoothGatt getGattByMacAddress(String macAddress) throws IndexOutOfBoundsException, NoSuchElementException {
        if (gatts.size() < 1) {
            throw new IndexOutOfBoundsException();
        }
        Iterator iterator = gatts.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry mentry = (Map.Entry) iterator.next();
            BluetoothGatt gatt = (BluetoothGatt) mentry.getValue();
            String address = (String) mentry.getKey();
            if (address.equals(macAddress)) {
                return gatt;
            }
        }
        throw new NoSuchElementException();
    }

    private BluetoothDevice getDeviceByMacAddress(String macAddress) throws IndexOutOfBoundsException, NoSuchElementException {
        if (devices.size() < 1) {
            throw new IndexOutOfBoundsException();
        }
        Iterator iterator = devices.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry mentry = (Map.Entry) iterator.next();
            BluetoothDevice device = (BluetoothDevice) mentry.getValue();
            if (device.getAddress().equals(macAddress)) {
                return device;
            }
        }
        throw new NoSuchElementException();
    }

    private BluetoothDevice getDeviceByName(String name) throws IndexOutOfBoundsException, NoSuchElementException {
        if (devices.size() < 1) {
            throw new IndexOutOfBoundsException();
        }
        Iterator iterator = devices.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry mentry = (Map.Entry) iterator.next();
            BluetoothDevice device = (BluetoothDevice) mentry.getValue();
            if (device.getName().equals(name)) {
                return device;
            }
        }
        throw new NoSuchElementException();
    }


    private BluetoothDevice getDeviceByIndex(Integer index) throws IndexOutOfBoundsException, NoSuchElementException {
        if (devices.size() < 1) {
            throw new IndexOutOfBoundsException();
        } else if (devices.size() <= index) {
            throw new NoSuchElementException();
        }
        Iterator iterator = devices.entrySet().iterator();
        int i = 0;
        while (iterator.hasNext()) { // TODO - this seems retarded
            Map.Entry mentry = (Map.Entry) iterator.next();
            BluetoothDevice device = (BluetoothDevice) mentry.getValue();
            if (i == index) {
                return device;
            }
            i++;
        }
        return null;
    }


    private Integer getArgNInteger(int N, JSONArray args) {
        Integer value = 0;
        if (args.length() > N) {
            try {
                value = args.getInt(N);
            } catch (JSONException e) {
            }
        }
        return value;
    }


    private String getArgNString(int N, JSONArray args) {
        String value = "";
        if (args.length() > N) {
            try {
                value = args.getString(N);
            } catch (JSONException e) {
            }
        }
        return value;
    }


    public boolean execute(final String action, final JSONArray args, final CallbackContext callbackContext) throws JSONException {
        cordova.getThreadPool().execute(new Runnable() {
            @Override
            public void run() {
                if ("list".equals(action)) {
                    doListDevices(callbackContext);
                } else if ("refresh".equals(action)) {
                    Log.i(TAG, "Refresh..");
                    refresh();
                    callbackContext.success();
                } else if ("test".equals(action)) {
                    Log.i(TAG, "Test method");
                    testMethod();
                    callbackContext.success();
                } else if ("init_icons".equals(action)) {
                    Log.i(TAG, "Test method");
                    String mac = getArgNString(0, args);
                    initIcons(mac);
                    callbackContext.success();
                } else if ("set_pixel".equals(action)) {
                    Log.i(TAG, "Test method");
                    String mac = getArgNString(0, args);
                    int pixelId = getArgNInteger(1, args);
                    int red = getArgNInteger(2, args);
                    int green = getArgNInteger(3, args);
                    int blue = getArgNInteger(4, args);
                    setPixelRGB(mac, pixelId, red, green, blue);
                    callbackContext.success();
                } else if ("save_icon".equals(action)) {
                    Log.i(TAG, "Test method");
                    String mac = getArgNString(0, args);
                    int iconId = getArgNInteger(1, args);
                    saveCurrentIcon(mac, iconId);
                    callbackContext.success();
                } else if ("show_icon".equals(action)) {
                    Log.i(TAG, "Test method");
                    String mac = getArgNString(0, args);
                    int iconId = getArgNInteger(1, args);
                    showIcon(mac, iconId);
                    callbackContext.success();
                }
            }
        });
        return true;
    }

}
