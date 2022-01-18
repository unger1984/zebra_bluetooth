// coverage:ignore-file
// GENERATED CODE - DO NOT MODIFY BY HAND
// ignore_for_file: type=lint
// ignore_for_file: unused_element, deprecated_member_use, deprecated_member_use_from_same_package, use_function_type_syntax_for_parameters, unnecessary_const, avoid_init_to_null, invalid_override_different_default_values_named, prefer_expression_function_bodies, annotate_overrides, invalid_annotation_target

part of 'bluetooth_device.dart';

// **************************************************************************
// FreezedGenerator
// **************************************************************************

T _$identity<T>(T value) => value;

final _privateConstructorUsedError = UnsupportedError(
    'It seems like you constructed your class using `MyClass._()`. This constructor is only meant to be used by freezed and you are not supposed to need it nor use it.\nPlease check the documentation here for more informations: https://github.com/rrousselGit/freezed#custom-getters-and-methods');

BluetoothDevice _$BluetoothDeviceFromJson(Map<String, dynamic> json) {
  return _BluetoothDevice.fromJson(json);
}

/// @nodoc
class _$BluetoothDeviceTearOff {
  const _$BluetoothDeviceTearOff();

  _BluetoothDevice call(
      {String name = '',
      String address = '',
      int type = 0,
      dynamic connected = false}) {
    return _BluetoothDevice(
      name: name,
      address: address,
      type: type,
      connected: connected,
    );
  }

  BluetoothDevice fromJson(Map<String, Object?> json) {
    return BluetoothDevice.fromJson(json);
  }
}

/// @nodoc
const $BluetoothDevice = _$BluetoothDeviceTearOff();

/// @nodoc
mixin _$BluetoothDevice {
  String get name => throw _privateConstructorUsedError;
  String get address => throw _privateConstructorUsedError;
  int get type => throw _privateConstructorUsedError;
  dynamic get connected => throw _privateConstructorUsedError;

  Map<String, dynamic> toJson() => throw _privateConstructorUsedError;
  @JsonKey(ignore: true)
  $BluetoothDeviceCopyWith<BluetoothDevice> get copyWith =>
      throw _privateConstructorUsedError;
}

/// @nodoc
abstract class $BluetoothDeviceCopyWith<$Res> {
  factory $BluetoothDeviceCopyWith(
          BluetoothDevice value, $Res Function(BluetoothDevice) then) =
      _$BluetoothDeviceCopyWithImpl<$Res>;
  $Res call({String name, String address, int type, dynamic connected});
}

/// @nodoc
class _$BluetoothDeviceCopyWithImpl<$Res>
    implements $BluetoothDeviceCopyWith<$Res> {
  _$BluetoothDeviceCopyWithImpl(this._value, this._then);

  final BluetoothDevice _value;
  // ignore: unused_field
  final $Res Function(BluetoothDevice) _then;

  @override
  $Res call({
    Object? name = freezed,
    Object? address = freezed,
    Object? type = freezed,
    Object? connected = freezed,
  }) {
    return _then(_value.copyWith(
      name: name == freezed
          ? _value.name
          : name // ignore: cast_nullable_to_non_nullable
              as String,
      address: address == freezed
          ? _value.address
          : address // ignore: cast_nullable_to_non_nullable
              as String,
      type: type == freezed
          ? _value.type
          : type // ignore: cast_nullable_to_non_nullable
              as int,
      connected: connected == freezed
          ? _value.connected
          : connected // ignore: cast_nullable_to_non_nullable
              as dynamic,
    ));
  }
}

/// @nodoc
abstract class _$BluetoothDeviceCopyWith<$Res>
    implements $BluetoothDeviceCopyWith<$Res> {
  factory _$BluetoothDeviceCopyWith(
          _BluetoothDevice value, $Res Function(_BluetoothDevice) then) =
      __$BluetoothDeviceCopyWithImpl<$Res>;
  @override
  $Res call({String name, String address, int type, dynamic connected});
}

/// @nodoc
class __$BluetoothDeviceCopyWithImpl<$Res>
    extends _$BluetoothDeviceCopyWithImpl<$Res>
    implements _$BluetoothDeviceCopyWith<$Res> {
  __$BluetoothDeviceCopyWithImpl(
      _BluetoothDevice _value, $Res Function(_BluetoothDevice) _then)
      : super(_value, (v) => _then(v as _BluetoothDevice));

  @override
  _BluetoothDevice get _value => super._value as _BluetoothDevice;

  @override
  $Res call({
    Object? name = freezed,
    Object? address = freezed,
    Object? type = freezed,
    Object? connected = freezed,
  }) {
    return _then(_BluetoothDevice(
      name: name == freezed
          ? _value.name
          : name // ignore: cast_nullable_to_non_nullable
              as String,
      address: address == freezed
          ? _value.address
          : address // ignore: cast_nullable_to_non_nullable
              as String,
      type: type == freezed
          ? _value.type
          : type // ignore: cast_nullable_to_non_nullable
              as int,
      connected: connected == freezed ? _value.connected : connected,
    ));
  }
}

/// @nodoc
@JsonSerializable()
class _$_BluetoothDevice implements _BluetoothDevice {
  const _$_BluetoothDevice(
      {this.name = '',
      this.address = '',
      this.type = 0,
      this.connected = false});

  factory _$_BluetoothDevice.fromJson(Map<String, dynamic> json) =>
      _$$_BluetoothDeviceFromJson(json);

  @JsonKey()
  @override
  final String name;
  @JsonKey()
  @override
  final String address;
  @JsonKey()
  @override
  final int type;
  @JsonKey()
  @override
  final dynamic connected;

  @override
  String toString() {
    return 'BluetoothDevice(name: $name, address: $address, type: $type, connected: $connected)';
  }

  @JsonKey(ignore: true)
  @override
  _$BluetoothDeviceCopyWith<_BluetoothDevice> get copyWith =>
      __$BluetoothDeviceCopyWithImpl<_BluetoothDevice>(this, _$identity);

  @override
  Map<String, dynamic> toJson() {
    return _$$_BluetoothDeviceToJson(this);
  }
}

abstract class _BluetoothDevice implements BluetoothDevice {
  const factory _BluetoothDevice(
      {String name,
      String address,
      int type,
      dynamic connected}) = _$_BluetoothDevice;

  factory _BluetoothDevice.fromJson(Map<String, dynamic> json) =
      _$_BluetoothDevice.fromJson;

  @override
  String get name;
  @override
  String get address;
  @override
  int get type;
  @override
  dynamic get connected;
  @override
  @JsonKey(ignore: true)
  _$BluetoothDeviceCopyWith<_BluetoothDevice> get copyWith =>
      throw _privateConstructorUsedError;
}
