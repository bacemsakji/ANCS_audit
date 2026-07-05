import 'package:dio/dio.dart';
import '../../../core/network/dio_client.dart';
import '../../../core/local_db/app_database.dart';
import 'package:drift/drift.dart';

class ConstatRepository {
  final DioClient _dioClient;
  final AppDatabase _db;

  ConstatRepository({required DioClient dioClient, required AppDatabase db})
      : _dioClient = dioClient,
        _db = db;

  Future<List<dynamic>> getConstatsByMission(String missionId) async {
    final response = await _dioClient.instance.get('/api/constats/mission/$missionId');
    return response.data as List<dynamic>;
  }

  /// Soumet un constat (online). Si offline, enregistre localement (synced=false).
  Future<void> submitConstat({
    required String id,
    required String missionId,
    required String controleId,
    required String resultat,
    String? commentaire,
    bool isOnline = true,
  }) async {
    if (isOnline) {
      await _dioClient.instance.post('/api/constats', data: {
        'missionId': missionId,
        'controleId': controleId,
        'resultat': resultat,
        'commentaire': commentaire,
      });
    } else {
      // Stockage local en attente de synchronisation
      await _db.into(_db.localConstatTable).insertOnConflictUpdate(
        LocalConstatTableCompanion(
          id: Value(id),
          missionId: Value(missionId),
          controleId: Value(controleId),
          resultat: Value(resultat),
          commentaire: Value(commentaire),
          synced: const Value(false),
        ),
      );
    }
  }

  /// Synchronise tous les constats locaux non synchronisés.
  Future<void> syncPendingConstats() async {
    final pending = await (_db.select(_db.localConstatTable)
          ..where((t) => t.synced.equals(false)))
        .get();

    for (final c in pending) {
      try {
        await _dioClient.instance.post('/api/constats', data: {
          'missionId': c.missionId,
          'controleId': c.controleId,
          'resultat': c.resultat,
          'commentaire': c.commentaire,
        });
        await (_db.update(_db.localConstatTable)
              ..where((t) => t.id.equals(c.id)))
            .write(const LocalConstatTableCompanion(synced: Value(true)));
      } catch (_) {
        // Laisser en attente si la synchronisation échoue
      }
    }
  }
}
