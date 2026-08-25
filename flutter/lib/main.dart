import 'dart:async';

import 'package:adhan_dart/adhan_dart.dart';
import 'package:flutter/material.dart';
import 'package:geolocator/geolocator.dart';
import 'package:intl/intl.dart';
import 'package:shared_preferences/shared_preferences.dart';

void main() {
  runApp(const JamSholatApp());
}

class JamSholatApp extends StatelessWidget {
  const JamSholatApp({super.key});

  @override
  Widget build(BuildContext context) {
    return MaterialApp(
      title: 'Jam Sholat',
      debugShowCheckedModeBanner: false,
      theme: ThemeData(
        brightness: Brightness.dark,
        primarySwatch: Colors.green,
        scaffoldBackgroundColor: Colors.black,
      ),
      home: const JamSholatHomePage(),
    );
  }
}

class JamSholatSettings {
  const JamSholatSettings({
    this.mosqueName = 'Nama Masjid',
    this.address = 'Alamat Lengkap dan Nomor Telepon',
    this.latitude = -6.224655537226517,
    this.longitude = 106.80679437749554,
  });

  final String mosqueName;
  final String address;
  final double latitude;
  final double longitude;

  JamSholatSettings copyWith({
    String? mosqueName,
    String? address,
    double? latitude,
    double? longitude,
  }) {
    return JamSholatSettings(
      mosqueName: mosqueName ?? this.mosqueName,
      address: address ?? this.address,
      latitude: latitude ?? this.latitude,
      longitude: longitude ?? this.longitude,
    );
  }
}

class JamSholatHomePage extends StatefulWidget {
  const JamSholatHomePage({super.key});

  @override
  State<JamSholatHomePage> createState() => _JamSholatHomePageState();
}

class _JamSholatHomePageState extends State<JamSholatHomePage> {
  static const JamSholatSettings _defaultSettings = JamSholatSettings();
  late Timer _timer;
  JamSholatSettings _settings = _defaultSettings;
  PrayerTimes? _prayerTimes;
  DateTime _now = DateTime.now();
  String _nextPrayerLabel = 'Subuh';
  bool _isLoading = false;

  @override
  void initState() {
    super.initState();
    _computePrayerTimes();
    _loadSettings();
    _timer = Timer.periodic(const Duration(seconds: 1), (_) {
      setState(() {
        _now = DateTime.now();
        _updateNextPrayer();
      });
    });
  }

  @override
  void dispose() {
    _timer.cancel();
    super.dispose();
  }

  Future<void> _loadSettings() async {
    final prefs = await SharedPreferences.getInstance();
    setState(() {
      _settings = JamSholatSettings(
        mosqueName: prefs.getString('mosqueName') ?? _defaultSettings.mosqueName,
        address: prefs.getString('address') ?? _defaultSettings.address,
        latitude: prefs.getDouble('latitude') ?? _defaultSettings.latitude,
        longitude: prefs.getDouble('longitude') ?? _defaultSettings.longitude,
      );
      _isLoading = false;
    });
    _computePrayerTimes();
  }

  Future<void> _saveSettings(JamSholatSettings settings) async {
    final prefs = await SharedPreferences.getInstance();
    await prefs.setString('mosqueName', settings.mosqueName);
    await prefs.setString('address', settings.address);
    await prefs.setDouble('latitude', settings.latitude);
    await prefs.setDouble('longitude', settings.longitude);
    if (!mounted) return;
    setState(() {
      _settings = settings;
    });
    _computePrayerTimes();
  }

  void _computePrayerTimes() {
    final coordinates = Coordinates(_settings.latitude, _settings.longitude);
    final prayerTimes = PrayerTimes(
      date: DateTime.now(),
      coordinates: coordinates,
      calculationParameters: CalculationMethodParameters.indonesian(),
    );
    setState(() {
      _prayerTimes = prayerTimes;
      _updateNextPrayer();
    });
  }

  void _updateNextPrayer() {
    final prayerMap = _buildPrayerMap();
    final now = DateTime.now();
    String nextLabel = 'Subuh';

    for (final entry in prayerMap.entries) {
      if (entry.value.isAfter(now)) {
        nextLabel = entry.key;
        break;
      }
    }

    _nextPrayerLabel = nextLabel;
  }

  Map<String, DateTime> _buildPrayerMap() {
    if (_prayerTimes == null) {
      return {
        'Imsak': DateTime.now(),
        'Subuh': DateTime.now(),
        'Terbit': DateTime.now(),
        'Dzuhur': DateTime.now(),
        'Ashar': DateTime.now(),
        'Maghrib': DateTime.now(),
        'Isya': DateTime.now(),
      };
    }

    final prayerTimes = _prayerTimes!;
    final imsak = prayerTimes.fajr.subtract(const Duration(minutes: 10));

    return {
      'Imsak': imsak,
      'Subuh': prayerTimes.fajr,
      'Terbit': prayerTimes.sunrise,
      'Dzuhur': prayerTimes.dhuhr,
      'Ashar': prayerTimes.asr,
      'Maghrib': prayerTimes.maghrib,
      'Isya': prayerTimes.isha,
    };
  }

  Future<void> _useCurrentLocation() async {
    final permission = await Geolocator.requestPermission();
    if (permission == LocationPermission.denied ||
        permission == LocationPermission.deniedForever) {
      if (mounted) {
        ScaffoldMessenger.of(context).showSnackBar(
          const SnackBar(content: Text('Location permission denied.')),
        );
      }
      return;
    }

    final position = await Geolocator.getCurrentPosition(
      desiredAccuracy: LocationAccuracy.low,
    );
    final updated = _settings.copyWith(
      latitude: position.latitude,
      longitude: position.longitude,
    );
    await _saveSettings(updated);
    if (mounted) {
      ScaffoldMessenger.of(context).showSnackBar(
        const SnackBar(content: Text('Current location used.')),
      );
    }
  }

  Future<void> _openSettings() async {
    final mosqueController = TextEditingController(text: _settings.mosqueName);
    final addressController = TextEditingController(text: _settings.address);
    final latController = TextEditingController(text: _settings.latitude.toString());
    final lonController = TextEditingController(text: _settings.longitude.toString());

    final updated = await showDialog<JamSholatSettings>(
      context: context,
      builder: (context) {
        return AlertDialog(
          title: const Text('Settings'),
          content: SizedBox(
            width: 420,
            child: SingleChildScrollView(
              child: Column(
                children: [
                  TextField(
                    controller: mosqueController,
                    decoration: const InputDecoration(labelText: 'Nama Masjid'),
                  ),
                  const SizedBox(height: 12),
                  TextField(
                    controller: addressController,
                    decoration: const InputDecoration(labelText: 'Alamat Masjid'),
                    maxLines: 2,
                  ),
                  const SizedBox(height: 12),
                  Row(
                    children: [
                      Expanded(
                        child: TextField(
                          controller: latController,
                          keyboardType: const TextInputType.numberWithOptions(decimal: true),
                          decoration: const InputDecoration(labelText: 'Latitude'),
                        ),
                      ),
                      const SizedBox(width: 12),
                      Expanded(
                        child: TextField(
                          controller: lonController,
                          keyboardType: const TextInputType.numberWithOptions(decimal: true),
                          decoration: const InputDecoration(labelText: 'Longitude'),
                        ),
                      ),
                    ],
                  ),
                  const SizedBox(height: 16),
                  Align(
                    alignment: Alignment.centerLeft,
                    child: ElevatedButton.icon(
                      onPressed: _useCurrentLocation,
                      icon: const Icon(Icons.my_location),
                      label: const Text('Use current location'),
                    ),
                  ),
                ],
              ),
            ),
          ),
          actions: [
            TextButton(
              onPressed: () => Navigator.pop(context),
              child: const Text('Cancel'),
            ),
            TextButton(
              onPressed: () {
                final cleaned = JamSholatSettings(
                  mosqueName: mosqueController.text.trim().isEmpty
                      ? _settings.mosqueName
                      : mosqueController.text.trim(),
                  address: addressController.text.trim().isEmpty
                      ? _settings.address
                      : addressController.text.trim(),
                  latitude: double.tryParse(latController.text) ?? _settings.latitude,
                  longitude: double.tryParse(lonController.text) ?? _settings.longitude,
                );
                Navigator.pop(context, cleaned);
              },
              child: const Text('Save'),
            ),
          ],
        );
      },
    );

    if (updated != null) {
      await _saveSettings(updated);
    }
  }

  @override
  Widget build(BuildContext context) {
    if (_isLoading) {
      return const Scaffold(
        body: Center(child: CircularProgressIndicator()),
      );
    }

    final prayerMap = _buildPrayerMap();
    final headerText = DateFormat('dd MMMM yyyy, HH:mm:ss').format(_now);

    return Scaffold(
      body: DecoratedBox(
        decoration: const BoxDecoration(
          gradient: LinearGradient(
            begin: Alignment.topCenter,
            end: Alignment.bottomCenter,
            colors: [Color(0xFF001D13), Color(0xFF000000)],
          ),
        ),
        child: SafeArea(
          child: Padding(
            padding: const EdgeInsets.all(12),
            child: Column(
              children: [
                Row(
                  crossAxisAlignment: CrossAxisAlignment.start,
                  children: [
                    Expanded(
                      child: Column(
                        crossAxisAlignment: CrossAxisAlignment.start,
                        children: [
                          Text(
                            _settings.mosqueName,
                            style: const TextStyle(
                              fontSize: 32,
                              fontWeight: FontWeight.bold,
                              color: Colors.white,
                            ),
                          ),
                          const SizedBox(height: 4),
                          Text(
                            _settings.address,
                            style: const TextStyle(
                              fontSize: 18,
                              color: Colors.white70,
                            ),
                          ),
                        ],
                      ),
                    ),
                    Text(
                      headerText,
                      textAlign: TextAlign.right,
                      style: const TextStyle(
                        fontSize: 24,
                        fontWeight: FontWeight.w600,
                        color: Colors.white,
                      ),
                    ),
                  ],
                ),
                const SizedBox(height: 16),
                Expanded(
                  child: GridView.count(
                    crossAxisCount: 7,
                    mainAxisSpacing: 10,
                    crossAxisSpacing: 10,
                    childAspectRatio: 0.9,
                    children: prayerMap.entries.map((entry) {
                      final label = entry.key;
                      final time = entry.value;
                      final isNext = _nextPrayerLabel == label;

                      return Container(
                        decoration: BoxDecoration(
                          border: Border.all(
                            color: isNext ? Colors.yellow : const Color(0xFF006300),
                            width: 3,
                          ),
                          borderRadius: BorderRadius.circular(8),
                          color: isNext
                              ? Colors.yellow.withAlpha(40)
                              : Colors.black.withAlpha(90),
                        ),
                        child: Column(
                          children: [
                            Container(
                              width: double.infinity,
                              padding: const EdgeInsets.symmetric(vertical: 10),
                              decoration: BoxDecoration(
                                color: isNext
                                    ? Colors.yellow.withAlpha(80)
                                    : const Color(0xFF006300),
                                borderRadius: const BorderRadius.vertical(
                                  top: Radius.circular(6),
                                ),
                              ),
                              child: Text(
                                label,
                                textAlign: TextAlign.center,
                                style: const TextStyle(
                                  fontSize: 22,
                                  fontWeight: FontWeight.bold,
                                  color: Colors.white,
                                ),
                              ),
                            ),
                            Expanded(
                              child: Center(
                                child: Text(
                                  DateFormat('HH:mm').format(time),
                                  style: const TextStyle(
                                    fontSize: 28,
                                    fontWeight: FontWeight.bold,
                                    color: Colors.white,
                                  ),
                                ),
                              ),
                            ),
                          ],
                        ),
                      );
                    }).toList(),
                  ),
                ),
                const SizedBox(height: 8),
                Container(
                  width: double.infinity,
                  padding: const EdgeInsets.all(10),
                  decoration: BoxDecoration(
                    color: Colors.blue.withAlpha(60),
                    borderRadius: BorderRadius.circular(10),
                  ),
                  child: Text(
                    'Next prayer: $_nextPrayerLabel • ${DateFormat('dd MMMM yyyy, HH:mm:ss').format(_now)}',
                    textAlign: TextAlign.center,
                    style: const TextStyle(
                      fontSize: 20,
                      color: Colors.white,
                    ),
                  ),
                ),
              ],
            ),
          ),
        ),
      ),
      floatingActionButton: FloatingActionButton(
        onPressed: _openSettings,
        backgroundColor: Colors.green,
        child: const Icon(Icons.settings),
      ),
    );
  }
}
