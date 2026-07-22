import 'package:dio/dio.dart';
import '../../../core/network/dio_client.dart';

class RapportRepository {
  final DioClient _dioClient;
  RapportRepository({required DioClient dioClient}) : _dioClient = dioClient;

  /// Génère un brouillon de synthèse par l'IA locale (Ollama/Qwen2.5-Coder).
  Future<Map<String, dynamic>> generateSyntheseIa(String missionId, String langue) async {
    final response = await _dioClient.instance.post(
      '/api/rapports/missions/$missionId/synthese-ia',
      queryParameters: {'langue': langue},
    );
    return response.data as Map<String, dynamic>;
  }

  /// Compile et génère le rapport final (PDF ou DOCX) pour une mission.
  Future<Map<String, dynamic>> generateRapport({
    required String missionId,
    required String type,
    required String syntheseExecutive,
    bool isIaGenerated = false,
  }) async {
    final response = await _dioClient.instance.post(
      '/api/rapports/generer/$missionId',
      queryParameters: {'type': type},
      data: {
        'syntheseExecutive': syntheseExecutive,
        'isIaGenerated': isIaGenerated,
      },
    );
    return response.data as Map<String, dynamic>;
  }

  /// Obtient l'URL de téléchargement pré-signée (MinIO) pour un rapport donné.
  Future<String> getDownloadUrl(String rapportId) async {
    final response = await _dioClient.instance.get('/api/rapports/$rapportId/download');
    return response.data['downloadUrl'] as String;
  }

  /// Liste tous les rapports d'une mission (toutes versions, ordre DESC).
  /// Accessible à l'auditeur assigné et à l'admin.
  Future<List<Map<String, dynamic>>> getRapportsByMission(String missionId) async {
    final response = await _dioClient.instance.get('/api/rapports/missions/$missionId');
    final list = response.data as List<dynamic>;
    return list.map((e) => e as Map<String, dynamic>).toList();
  }

  /// Liste les rapports de l'organisme du RSSI connecté.
  Future<List<Map<String, dynamic>>> getRapportsByOrganisme() async {
    final response = await _dioClient.instance.get('/api/rapports/mon-organisme');
    final list = response.data as List<dynamic>;
    return list.map((e) => e as Map<String, dynamic>).toList();
  }

  /// Soumet le rapport officielement à l'ANCS pour validation.
  Future<Map<String, dynamic>> soumettre(String rapportId) async {
    final response = await _dioClient.instance.post('/api/rapports/$rapportId/soumettre');
    return response.data as Map<String, dynamic>;
  }
}
