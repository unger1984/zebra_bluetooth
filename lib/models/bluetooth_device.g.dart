// GENERATED CODE - DO NOT MODIFY BY HAND

part of 'bluetooth_device.dart';

// **************************************************************************
// JsonSerializableGenerator
// **************************************************************************

_$_BluetoothDevice _$$_BluetoothDeviceFromJson(Map<String, dynamic> json) =>
    _$_BluetoothDevice(
      name: json['name'] as String? ?? '',
      address: json['address'] as String? ?? '',
      type: json['type'] as int? ?? 0,
      connected: json['connected'] ?? false,
    );

Map<String, dynamic> _$$_BluetoothDeviceToJson(_$_BluetoothDevice instance) =>
    <String, dynamic>{
      'name': instance.name,
      'address': instance.address,
      'type': instance.type,
      'connected': instance.connected,
    };
