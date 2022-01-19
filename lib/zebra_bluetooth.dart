import 'dart:async';
import 'dart:typed_data';

import 'package:flutter/services.dart';
import 'package:zebra_bluetooth/models/bluetooth_device.dart';

export 'package:zebra_bluetooth/models/bluetooth_device.dart';

class ZebraBluetooth {
  static const int stateOFF = 10;
  static const int stateTurningON = 11;
  static const int stateON = 12;
  static const int stateTurningOFF = 13;
  static const int stateBLETurningON = 14;
  static const int stateBLEON = 15;
  static const int stateBLETurningOFF = 16;
  static const int error = -1;
  static const int connected = 1;
  static const int disconnected = 0;

  static const String namespace = 'zebra_bluetooth';
  static const MethodChannel _channel = MethodChannel('$namespace/methods');

  static const EventChannel _readChannel = EventChannel('$namespace/read');

  static const EventChannel _stateChannel = EventChannel('$namespace/state');

  final StreamController<MethodCall> _methodStreamController = StreamController.broadcast();

  ZebraBluetooth._() {
    _channel.setMethodCallHandler((MethodCall call) async {
      _methodStreamController.add(call);
    });
  }

  static final ZebraBluetooth _instance = ZebraBluetooth._();
  static ZebraBluetooth get instance => _instance;

  Stream<int?> onStateChanged() => _stateChannel.receiveBroadcastStream().map((buffer) => buffer);

  Stream<String> onRead() => _readChannel.receiveBroadcastStream().map((buffer) => buffer.toString());

  Future<bool?> get isAvailable async => await _channel.invokeMethod('isAvailable');

  Future<bool?> get isOn async => await _channel.invokeMethod('isOn');

  Future<bool?> get isConnected async => await _channel.invokeMethod('isConnected');

  Future<bool?> get openSettings async => await _channel.invokeMethod('openSettings');

  Future<List<BluetoothDevice>> getBondedDevices() async {
    final List<dynamic> list = await (_channel.invokeMethod('getBondedDevices'));

    if (list.isEmpty) return [];

    return list
        .cast<Map<Object?, Object?>>()
        .map((Map<Object?, Object?> map) =>
            BluetoothDevice.fromJson(map.map((key, value) => MapEntry(key.toString(), value))))
        .toList();
  }

  Future<dynamic> connect(BluetoothDevice device) => _channel.invokeMethod('connect', device.toJson());

  Future<dynamic> disconnect() => _channel.invokeMethod('disconnect');

  Future<dynamic> write(String message) => _channel.invokeMethod('write', {'message': message});

  Future<dynamic> writeBytes(List<int> message) =>
      _channel.invokeMethod('writeBytes', {'message': Uint8List.fromList(message)});
}
