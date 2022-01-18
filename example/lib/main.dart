import 'dart:async';

import 'package:flutter/material.dart';
import 'package:zebra_bluetooth/zebra_bluetooth.dart';

void main() {
  runApp(MyApp());
}

class MyApp extends StatefulWidget {
  @override
  _MyAppState createState() => _MyAppState();
}

class _MyAppState extends State<MyApp> {
  final bluetooth = ZebraBluetooth.instance;
  Future<List<BluetoothDevice>>? _devices;

  @override
  void initState() {
    _devices = bluetooth.getBondedDevices();
    super.initState();
  }

  Future<void> _handleRefresh() async {
    setState(() {
      _devices = bluetooth.getBondedDevices();
    });
  }

  @override
  Widget build(BuildContext context) {
    return MaterialApp(
      home: Scaffold(
        appBar: AppBar(
          title: const Text('Plugin example app'),
        ),
        body: FutureBuilder<List<BluetoothDevice>>(
          future: _devices,
          builder: (context, snapshot) {
            if (snapshot.hasData) {
              if (snapshot.data?.isEmpty ?? true) {
                return Center(
                  child: Column(
                    crossAxisAlignment: CrossAxisAlignment.stretch,
                    children: [
                      const Text(
                        'Принтеры не доступны. Подключите их через настройки Bluetooth',
                        textAlign: TextAlign.justify,
                      ),
                      ElevatedButton(
                        onPressed: _handleRefresh,
                        child: const Text('Обновить'),
                      ),
                    ],
                  ),
                );
              }

              return RefreshIndicator(
                onRefresh: _handleRefresh,
                child: ListView.builder(
                  itemCount: snapshot.data?.length ?? 0,
                  itemBuilder: (context, index) {
                    return TextButton(
                      onPressed: () => print(snapshot.data?[index]),
                      child: Text(snapshot.data?[index].name ?? 'unknown'),
                    );
                  },
                ),
              );
            } else {
              return const Center(
                child: CircularProgressIndicator(),
              );
            }
          },
        ),
      ),
    );
  }
}
