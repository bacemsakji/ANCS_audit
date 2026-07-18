import 'dart:async';
import 'dart:developer' as developer;

import 'package:connectivity_plus/connectivity_plus.dart';
import 'package:dio/dio.dart' as dio;
import 'package:drift/drift.dart';
import '../local_db/app_database.dart';
import 'dio_client.dart';

/// Service de synchronisation arrière-plan.
///
/// Écoute les changements de connectivité et, dès que l'appareil est en ligne,
/// téléverse vers le backend tous les constats enregistrés localement
/// ([synced] == false).
///
/// La connexion réseau est partagée avec le reste de l'application via
/// l'instance [DioClient] injectée au constructeur — évitant ainsi toute
/// divergence d'URL de base.
class SyncService {
  final Connectivity _connectivity = Connectivity();
  final StreamController<bool> _syncStatusController =
      StreamController<bool>.broadcast();

  final AppDatabase database;
  final DioClient dioClient;

  SyncService({
    required this.database,
    required this.dioClient,
  }) {
    _connectivity.onConnectivityChanged.listen((result) {
      final isOnline =
          result.any((element) => element != ConnectivityResult.none);
      _syncStatusController.add(isOnline);
      if (isOnline) {
        triggerSync();
      }
    });
  }

  Stream<bool> get connectionStatusStream => _syncStatusController.stream;

  /// Téléverse tous les constats non synchronisés vers le backend.
  ///
  /// Pour chaque constat :
  ///   1. Si une preuve locale existe (chemin de fichier), elle est téléversée
  ///      en multipart via [POST /api/constats/{id}/preuve] en premier.
  ///      En cas d'échec de l'upload, le constat est ignoré et réessayé
  ///      au prochain cycle de synchronisation.
  ///   2. Les métadonnées du constat sont ensuite soumises via
  ///      [POST /api/constats], avec l'URL serveur retournée à l'étape 1.
  ///   3. En cas de succès, [synced] est mis à true dans la base locale.
  Future<void> triggerSync() async {
    try {
      final unsyncedConstats = await (database.select(database.localConstatTable)
            ..where((tbl) => tbl.synced.equals(false)))
          .get();

      if (unsyncedConstats.isEmpty) {
        developer.log('Aucun constat à synchroniser.', name: 'SyncService');
        return;
      }

      developer.log(
        'Synchronisation de ${unsyncedConstats.length} constat(s)...',
        name: 'SyncService',
      );

      for (final constat in unsyncedConstats) {
        try {
          // --- Étape 1 : upload de la preuve si elle est stockée en local ---
          String? serverPreuveUrl;
          final localPath = constat.preuveUrl;

          if (localPath != null && _isLocalFilePath(localPath)) {
            try {
              final formData = dio.FormData.fromMap({
                'file': await dio.MultipartFile.fromFile(localPath),
              });
              final uploadResponse = await dioClient.instance.post(
                '/api/constats/${constat.id}/preuve',
                data: formData,
              );
              serverPreuveUrl = uploadResponse.data['preuveUrl'] as String?;
              developer.log(
                'Preuve téléversée pour le constat ${constat.id} : $serverPreuveUrl',
                name: 'SyncService',
              );
            } catch (e) {
              // L'upload a échoué : on laisse le constat en attente et on
              // passe au suivant pour réessayer au prochain cycle réseau.
              developer.log(
                'Échec de l\'upload de preuve pour le constat ${constat.id}, '
                'synchronisation différée : $e',
                name: 'SyncService',
                error: e,
              );
              continue;
            }
          } else {
            // La valeur est déjà une URL serveur (ou null) — on la transmet telle quelle.
            serverPreuveUrl = localPath;
          }

          // --- Étape 2 : envoi des métadonnées du constat ---
          await dioClient.instance.post(
            '/api/constats',
            data: {
              'missionId': constat.missionId,
              'controleId': constat.controleId,
              'resultat': constat.resultat,
              'commentaire': constat.commentaire,
              'dateConstat': constat.dateConstat.toIso8601String(),
              if (serverPreuveUrl != null) 'preuveUrl': serverPreuveUrl,
              if (constat.criticite != null) 'criticite': constat.criticite,
              if (constat.preuveDescription != null) 'preuveDescription': constat.preuveDescription,
              if (constat.recommandation != null) 'recommandation': constat.recommandation,
              if (constat.composantesImpactees != null) 'composantesImpactees': constat.composantesImpactees,
            },
          );

          // --- Étape 3 : marquer comme synchronisé en local ---
          await (database.update(database.localConstatTable)
                ..where((tbl) => tbl.id.equals(constat.id)))
              .write(LocalConstatTableCompanion(
            synced: const Value(true),
            // Mettre à jour avec l'URL serveur pour cohérence locale
            preuveUrl: Value(serverPreuveUrl),
          ));

          developer.log(
            'Constat ${constat.id} synchronisé avec succès.',
            name: 'SyncService',
          );
        } catch (e) {
          developer.log(
            'Erreur lors de la synchronisation du constat ${constat.id}: $e',
            name: 'SyncService',
            error: e,
          );
          // Continuer avec le constat suivant — celui-ci sera réessayé au prochain cycle.
        }
      }

      developer.log('Synchronisation terminée.', name: 'SyncService');
    } catch (e) {
      developer.log(
        'Erreur lors de la synchronisation: $e',
        name: 'SyncService',
        error: e,
      );
    }
  }

  /// Détermine si [path] est un chemin de fichier local (et non une URL serveur).
  ///
  /// Un chemin local commence par '/' (Unix/Android/iOS) ou une lettre de
  /// lecteur Windows, et ne contient pas de schéma '://'.
  bool _isLocalFilePath(String path) {
    if (path.contains('://')) return false; // http://, https://, etc.
    return path.startsWith('/') || // Android / iOS
        RegExp(r'^[A-Za-z]:\\').hasMatch(path); // Windows (dev)
  }

  void dispose() {
    _syncStatusController.close();
  }
}
