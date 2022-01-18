package com.awcoding.zebra_bluetooth;

import android.Manifest;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothSocket;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.util.Log;
import android.os.AsyncTask;
import android.os.Handler;
import android.os.Looper;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import io.flutter.embedding.engine.plugins.FlutterPlugin;
import io.flutter.plugin.common.EventChannel;
import io.flutter.plugin.common.EventChannel.StreamHandler;
import io.flutter.plugin.common.EventChannel.EventSink;
import io.flutter.plugin.common.MethodCall;
import io.flutter.plugin.common.MethodChannel;
import io.flutter.plugin.common.MethodChannel.Result;
import io.flutter.plugin.common.PluginRegistry.Registrar;
import io.flutter.plugin.common.PluginRegistry.RequestPermissionsResultListener;


public class ZebraBluetoothPlugin implements FlutterPlugin, MethodChannel.MethodCallHandler, RequestPermissionsResultListener {

    private static final String TAG = "ZebraBluetoothPlugin";
    private static final String NAMESPACE = "zebra_bluetooth";
    private static final int REQUEST_COARSE_LOCATION_PERMISSIONS = 1451;
    private static final UUID MY_UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");
    private static ConnectedThread THREAD = null;
    private FlutterPluginBinding bindingPlugin = null;
    private BluetoothAdapter mBluetoothAdapter;

    private Result pendingResult;

    private EventSink readSink;
    private EventSink statusSink;

//    public static void registerWith(Registrar registrar) {
//        final ZebraBluetoothPlugin instance = new ZebraBluetoothPlugin(registrar);
//        registrar.addRequestPermissionsResultListener(instance);
//    }

    // MethodChannel.Result wrapper that responds on the platform thread.
    private static class MethodResultWrapper implements Result {
        private Result methodResult;
        private Handler handler;

        MethodResultWrapper(Result result) {
            methodResult = result;
            handler = new Handler(Looper.getMainLooper());
        }

        @Override
        public void success(final Object result) {
            handler.post(new Runnable() {
                @Override
                public void run() {
                    methodResult.success(result);
                }
            });
        }

        @Override
        public void error(final String errorCode, final String errorMessage, final Object errorDetails) {
            handler.post(new Runnable() {
                @Override
                public void run() {
                    methodResult.error(errorCode, errorMessage, errorDetails);
                }
            });
        }

        @Override
        public void notImplemented() {
            handler.post(new Runnable() {
                @Override
                public void run() {
                    methodResult.notImplemented();
                }
            });
        }
    }

    @Override
    public void onAttachedToEngine(@NonNull FlutterPluginBinding binding) {
        bindingPlugin = binding;
        MethodChannel channel = new MethodChannel(bindingPlugin.getBinaryMessenger(), NAMESPACE + "/methods");
        EventChannel stateChannel = new EventChannel(bindingPlugin.getBinaryMessenger(), NAMESPACE + "/state");
        EventChannel readChannel = new EventChannel(bindingPlugin.getBinaryMessenger(), NAMESPACE + "/read");
        BluetoothManager mBluetoothManager = (BluetoothManager) binding.getApplicationContext()
                .getSystemService(Context.BLUETOOTH_SERVICE);
        assert mBluetoothManager != null;
        this.mBluetoothAdapter = mBluetoothManager.getAdapter();
        channel.setMethodCallHandler(this);
        stateChannel.setStreamHandler(stateStreamHandler);
        readChannel.setStreamHandler(readResultsHandler);
    }

    @Override
    public void onDetachedFromEngine(@NonNull FlutterPluginBinding binding) {
        // TODO: your plugin is no longer attached to a Flutter experience.
    }

    @Override
    public void onMethodCall(@NonNull MethodCall call, @NonNull Result rawResult) {
        Result result = new MethodResultWrapper(rawResult);

        if (mBluetoothAdapter == null && !"isAvailable".equals(call.method)) {
            result.error("bluetooth_unavailable", "the device does not have bluetooth", null);
            return;
        }

        final Map<String, Object> arguments = call.arguments();

        switch (call.method) {

            case "isAvailable":
                result.success(mBluetoothAdapter != null);
                break;

            case "isOn":
                try {
                    result.success(mBluetoothAdapter.isEnabled());
                } catch (Exception ex) {
                    result.error("Error", ex.getMessage(), exceptionToString(ex));
                }
                break;

            case "isConnected":
                result.success(THREAD != null);
                break;

            case "openSettings":
                ContextCompat.startActivity(bindingPlugin.getApplicationContext(), new Intent(android.provider.Settings.ACTION_BLUETOOTH_SETTINGS),
                        null);
                result.success(true);
                break;

            case "getBondedDevices":
                try {

//                    if (ContextCompat.checkSelfPermission(bindingPlugin.getApplicationContext(),
//                            Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
//
//                        ActivityCompat.requestPermissions(bindingPlugin,
//                                new String[]{Manifest.permission.ACCESS_COARSE_LOCATION}, REQUEST_COARSE_LOCATION_PERMISSIONS);
//
//                        pendingResult = result;
//                        break;
//                    }

                    getBondedDevices(result);

                } catch (Exception ex) {
                    result.error("Error", ex.getMessage(), exceptionToString(ex));
                }

                break;

            case "connect":
                if (arguments.containsKey("address")) {
                    String address = (String) arguments.get("address");
                    connect(result, address);
                } else {
                    result.error("invalid_argument", "argument 'address' not found", null);
                }
                break;

            case "disconnect":
                disconnect(result);
                break;

            case "write":
                if (arguments.containsKey("message")) {
                    String message = (String) arguments.get("message");
                    write(result, message);
                } else {
                    result.error("invalid_argument", "argument 'message' not found", null);
                }
                break;

            case "writeBytes":
                if (arguments.containsKey("message")) {
                    byte[] message = (byte[]) arguments.get("message");
                    writeBytes(result, message);
                } else {
                    result.error("invalid_argument", "argument 'message' not found", null);
                }
                break;

            default:
                result.notImplemented();
                break;
        }
    }

    /**
     * @param requestCode  requestCode
     * @param permissions  permissions
     * @param grantResults grantResults
     * @return boolean
     */
    @Override
    public boolean onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {

        if (requestCode == REQUEST_COARSE_LOCATION_PERMISSIONS) {
            if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                getBondedDevices(pendingResult);
            } else {
                pendingResult.error("no_permissions", "this plugin requires location permissions for scanning", null);
                pendingResult = null;
            }
            return true;
        }
        return false;
    }

    /**
     * @param result result
     */
    private void getBondedDevices(Result result) {

        List<Map<String, Object>> list = new ArrayList<>();

        for (BluetoothDevice device : mBluetoothAdapter.getBondedDevices()) {
            Map<String, Object> ret = new HashMap<>();
            ret.put("address", device.getAddress());
            ret.put("name", device.getName());
            ret.put("type", device.getType());
            list.add(ret);
        }

        result.success(list);
    }

    private String exceptionToString(Exception ex) {
        StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter(sw);
        ex.printStackTrace(pw);
        return sw.toString();
    }

    /**
     * @param result  result
     * @param address address
     */
    private void connect(Result result, String address) {

        if (THREAD != null) {
            result.error("connect_error", "already connected", null);
            return;
        }
        AsyncTask.execute(() -> {
            try {
                BluetoothDevice device = mBluetoothAdapter.getRemoteDevice(address);

                if (device == null) {
                    result.error("connect_error", "device not found", null);
                    return;
                }

                BluetoothSocket socket = device.createRfcommSocketToServiceRecord(MY_UUID);

                if (socket == null) {
                    result.error("connect_error", "socket connection not established", null);
                    return;
                }

                // Cancel bt discovery, even though we didn't start it
                mBluetoothAdapter.cancelDiscovery();

                try {
                    socket.connect();
                    THREAD = new ConnectedThread(socket);
                    THREAD.start();
                    result.success(true);
                } catch (Exception ex) {
                    Log.e(TAG, ex.getMessage(), ex);
                    result.error("connect_error", ex.getMessage(), exceptionToString(ex));
                }
            } catch (Exception ex) {
                Log.e(TAG, ex.getMessage(), ex);
                result.error("connect_error", ex.getMessage(), exceptionToString(ex));
            }
        });
    }

    /**
     * @param result result
     */
    private void disconnect(Result result) {

        if (THREAD == null) {
            result.error("disconnection_error", "not connected", null);
            return;
        }
        AsyncTask.execute(() -> {
            try {
                THREAD.cancel();
                THREAD = null;
                result.success(true);
            } catch (Exception ex) {
                Log.e(TAG, ex.getMessage(), ex);
                result.error("disconnection_error", ex.getMessage(), exceptionToString(ex));
            }
        });
    }

    /**
     * @param result  result
     * @param message message
     */
    private void write(Result result, String message) {
        if (THREAD == null) {
            result.error("write_error", "not connected", null);
            return;
        }

        try {
            THREAD.write(message.getBytes());
            result.success(true);
        } catch (Exception ex) {
            Log.e(TAG, ex.getMessage(), ex);
            result.error("write_error", ex.getMessage(), exceptionToString(ex));
        }
    }

    private void writeBytes(Result result, byte[] message) {
        if (THREAD == null) {
            result.error("write_error", "not connected", null);
            return;
        }

        try {
            THREAD.write(message);
            result.success(true);
        } catch (Exception ex) {
            Log.e(TAG, ex.getMessage(), ex);
            result.error("write_error", ex.getMessage(), exceptionToString(ex));
        }
    }

    private class ConnectedThread extends Thread {
        private final BluetoothSocket mmSocket;
        private final InputStream inputStream;
        private final OutputStream outputStream;

        ConnectedThread(BluetoothSocket socket) {
            mmSocket = socket;
            InputStream tmpIn = null;
            OutputStream tmpOut = null;

            try {
                tmpIn = socket.getInputStream();
                tmpOut = socket.getOutputStream();
            } catch (IOException e) {
                e.printStackTrace();
            }
            inputStream = tmpIn;
            outputStream = tmpOut;
        }

        public void run() {
            byte[] buffer = new byte[1024];
            int bytes;
            while (true) {
                try {
                    bytes = inputStream.read(buffer);
                    readSink.success(new String(buffer, 0, bytes));
                } catch (NullPointerException e) {
                    break;
                } catch (IOException e) {
                    break;
                }
            }
        }

        public void write(byte[] bytes) {
            try {
                outputStream.write(bytes);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        public void cancel() {
            try {
                outputStream.flush();
                outputStream.close();

                inputStream.close();

                mmSocket.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private final StreamHandler stateStreamHandler = new StreamHandler() {

        private final BroadcastReceiver mReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                final String action = intent.getAction();

                Log.d(TAG, action);

                if (BluetoothAdapter.ACTION_STATE_CHANGED.equals(action)) {
                    THREAD = null;
                    statusSink.success(intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, -1));
                } else if (BluetoothDevice.ACTION_ACL_CONNECTED.equals(action)) {
                    statusSink.success(1);
                } else if (BluetoothDevice.ACTION_ACL_DISCONNECTED.equals(action)) {
                    THREAD = null;
                    statusSink.success(0);
                }
            }
        };

        @Override
        public void onListen(Object o, EventSink eventSink) {
            statusSink = eventSink;
            bindingPlugin.getApplicationContext().registerReceiver(mReceiver, new IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED));

            bindingPlugin.getApplicationContext().registerReceiver(mReceiver, new IntentFilter(BluetoothDevice.ACTION_ACL_CONNECTED));

            bindingPlugin.getApplicationContext().registerReceiver(mReceiver, new IntentFilter(BluetoothDevice.ACTION_ACL_DISCONNECTED));
        }

        @Override
        public void onCancel(Object o) {
            statusSink = null;
            bindingPlugin.getApplicationContext().unregisterReceiver(mReceiver);
        }
    };

    private final StreamHandler readResultsHandler = new StreamHandler() {
        @Override
        public void onListen(Object o, EventSink eventSink) {
            readSink = eventSink;
        }

        @Override
        public void onCancel(Object o) {
            readSink = null;
        }
    };
}
