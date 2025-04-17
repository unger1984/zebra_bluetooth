package com.awcoding.zebra_bluetooth;

import android.app.Activity;

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
import android.os.AsyncTask;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

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
import io.flutter.embedding.engine.plugins.activity.ActivityAware;
import io.flutter.embedding.engine.plugins.activity.ActivityPluginBinding;
import io.flutter.plugin.common.EventChannel;
import io.flutter.plugin.common.MethodCall;
import io.flutter.plugin.common.MethodChannel;
import io.flutter.plugin.common.MethodChannel.Result;
import io.flutter.plugin.common.PluginRegistry;
import io.flutter.plugin.common.EventChannel.StreamHandler;
import io.flutter.plugin.common.EventChannel.EventSink;

public class ZebraBluetoothPlugin implements FlutterPlugin, MethodChannel.MethodCallHandler, ActivityAware {

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

    private Activity activity;

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

    @Override
    public void onAttachedToActivity(@NonNull ActivityPluginBinding binding) {
        this.activity = binding.getActivity();
    }

    @Override
    public void onDetachedFromActivityForConfigChanges() {
        this.activity = null;
    }

    @Override
    public void onReattachedToActivityForConfigChanges(@NonNull ActivityPluginBinding binding) {
        this.activity = binding.getActivity();
    }

    @Override
    public void onDetachedFromActivity() {
        this.activity = null;
    }

    /**
     * @param requestCode  requestCode
     * @param permissions  permissions
     * @param grantResults grantResults
     * @return boolean
     */
    public boolean onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {

        if (requestCode == REQUEST_COARSE_LOCATION_PERMISSIONS) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                getBondedDevices(pendingResult);
            } else {
                pendingResult.error("no_permissions", "This plugin requires location permissions for scanning", null);
                pendingResult = null;
            }
            return true;
        }
        return false;
    }

    private void getBondedDevices(Result result) {
        if (ContextCompat.checkSelfPermission(activity, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(activity, new String[]{Manifest.permission.ACCESS_COARSE_LOCATION}, REQUEST_COARSE_LOCATION_PERMISSIONS);
            pendingResult = result;
            return;
        }

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
                result.error("disconnection_error", ex.getMessage(), exceptionToString(ex));
            }
        });
    }

    private void write(Result result, String message) {
        if (THREAD == null) {
            result.error("write_error", "not connected", null);
            return;
        }
        THREAD.write(message.getBytes());
        result.success(true);
    }

    private void writeBytes(Result result, byte[] message) {
        if (THREAD == null) {
            result.error("write_error", "not connected", null);
            return;
        }
        THREAD.write(message);
        result.success(true);
    }

    private StreamHandler stateStreamHandler = new StreamHandler() {
        @Override
        public void onListen(Object arguments, EventSink events) {
            statusSink = events;
        }

        @Override
        public void onCancel(Object arguments) {
            statusSink = null;
        }
    };

    private StreamHandler readResultsHandler = new StreamHandler() {
        @Override
        public void onListen(Object arguments, EventSink events) {
            readSink = events;
        }

        @Override
        public void onCancel(Object arguments) {
            readSink = null;
        }
    };

    private class ConnectedThread extends Thread {
        private final BluetoothSocket mmSocket;
        private final InputStream mmInStream;
        private final OutputStream mmOutStream;

        ConnectedThread(BluetoothSocket socket) {
            mmSocket = socket;
            InputStream tmpIn = null;
            OutputStream tmpOut = null;
            try {
                tmpIn = mmSocket.getInputStream();
                tmpOut = mmSocket.getOutputStream();
            } catch (IOException e) {
                Log.e(TAG, "Error", e);
            }

            mmInStream = tmpIn;
            mmOutStream = tmpOut;
        }

        public void run() {
            byte[] buffer = new byte[1024];
            int bytes;
            while (true) {
                try {
                    bytes = mmInStream.read(buffer);
                    if (readSink != null) {
                        readSink.success(buffer);
                    }
                } catch (IOException e) {
                    break;
                }
            }
        }

        public void write(byte[] bytes) {
            try {
                mmOutStream.write(bytes);
            } catch (IOException e) {
                Log.e(TAG, "Error writing to output stream", e);
            }
        }

        public void cancel() {
            try {
                mmSocket.close();
            } catch (IOException e) {
                Log.e(TAG, "Error closing socket", e);
            }
        }
    }
}
