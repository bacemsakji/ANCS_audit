import 'dart:developer' as developer;
import 'package:dio/dio.dart' as dio;
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
    final response =
        await _dioClient.instance.get('/api/constats/mission/$missionId');
    return response.data as List<dynamic>;
  }

  /// Soumet un constat (online). Si offline, enregistre localement (synced=false).
  Future<void> submitConstat({
    required String id,
    required String missionId,
    required String controleId,
    required String resultat,
    String? commentaire,
    String? imagePath,
    bool isOnline = true,
    String? criticite,
    String? preuveDescription,
    String? recommandation,
    String? composantesImpactees,
  }) async {
    if (isOnline) {
      // Si une image est fournie, l'uploader d'abord
      String? preuveUrl;
      if (imagePath != null) {
        try {
          final formData = dio.FormData.fromMap({
            'file': await dio.MultipartFile.fromFile(imagePath),
          });
          final response = await _dioClient.instance.post(
            '/api/constats/$id/preuve',
            data: formData,
          );
          preuveUrl = response.data['preuveUrl'];
        } catch (e) {
          developer.log("Erreur lors de l'upload de la preuve: $e",
              name: 'ConstatRepository', error: e);
          // Continuer sans la preuve en cas d'erreur
        }
      }

      await _dioClient.instance.post('/api/constats', data: {
        'missionId': missionId,
        'controleId': controleId,
        'resultat': resultat,
        'commentaire': commentaire,
        if (preuveUrl != null) 'preuveUrl': preuveUrl,
        if (criticite != null) 'criticite': criticite,
        if (preuveDescription != null) 'preuveDescription': preuveDescription,
        if (recommandation != null) 'recommandation': recommandation,
        if (composantesImpactees != null)
          'composantesImpactees': composantesImpactees,
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
              preuveUrl:
                  Value(imagePath), // Stocker le chemin local temporairement
              criticite: Value(criticite),
              preuveDescription: Value(preuveDescription),
              recommandation: Value(recommandation),
              composantesImpactees: Value(composantesImpactees),
              synced: const Value(false),
            ),
          );
    }
  }

  // La synchronisation des constats hors-ligne est gérée exclusivement par
  // SyncService.triggerSync() (core/network/sync_service.dart), qui gère
  // également l'upload multipart des preuves avant l'envoi des métadonnées.
  // Ne pas dupliquer cette logique ici.
}
