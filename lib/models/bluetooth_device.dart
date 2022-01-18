import 'package:freezed_annotation/freezed_annotation.dart';

part 'bluetooth_device.freezed.dart';
part 'bluetooth_device.g.dart';

@freezed
class BluetoothDevice with _$BluetoothDevice {
  const factory BluetoothDevice({
    @Default('') String name,
    @Default('') String address,
    @Default(0) int type,
    @Default(false) connected,
  }) = _BluetoothDevice;

  factory BluetoothDevice.fromJson(Map<String, dynamic> json) =>
      _$BluetoothDeviceFromJson(json);

  @override
  operator ==(Object other) {
    return other is BluetoothDevice && other.address == address;
  }
}
