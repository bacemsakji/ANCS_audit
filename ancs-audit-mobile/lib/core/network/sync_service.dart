import 'dart:async';
import 'package:connectivity_plus/connectivity_plus.dart';

class SyncService {
  final Connectivity _connectivity = Connectivity();
  final StreamController<bool> _syncStatusController = StreamController<bool>.broadcast();

  SyncService() {
    _connectivity.onConnectivityChanged.listen((result) {
      final isOnline = result.any((element) => element != ConnectivityResult.none);
      _syncStatusController.add(isOnline);
      if (isOnline) {
        triggerSync();
      }
    });
  }

  Stream<bool> get connectionStatusStream => _syncStatusController.stream;

  Future<void> triggerSync() async {
    // Parcourt les tables locales non synchronisées et les envoie au backend (Phase 6)
    print("Déclenchement de la synchronisation locale vers le serveur...");
  }

  void dispose() {
    _syncStatusController.close();
  }
}
