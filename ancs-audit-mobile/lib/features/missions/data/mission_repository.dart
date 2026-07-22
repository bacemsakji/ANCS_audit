import '../../../core/network/dio_client.dart';

class MissionRepository {
  final DioClient _dioClient;
  MissionRepository({required DioClient dioClient}) : _dioClient = dioClient;

  Future<List<dynamic>> getMissions({int page = 0, int size = 20}) async {
    final response = await _dioClient.instance.get(
      '/api/missions',
      queryParameters: {'page': page, 'size': size},
    );
    final data = response.data;
    if (data is Map && data.containsKey('content')) {
      return data['content'] as List<dynamic>;
    }
    return data is List ? data : [];
  }

  Future<Map<String, dynamic>> getMissionById(String id) async {
    final response = await _dioClient.instance.get('/api/missions/$id');
    return response.data as Map<String, dynamic>;
  }

  Future<void> updateStatus(String id, String statut) async {
    await _dioClient.instance.patch(
      '/api/missions/$id/statut',
      queryParameters: {'statut': statut},
    );
  }

  Future<List<dynamic>> getOrganismes() async {
    final response = await _dioClient.instance.get('/api/organismes/list');
    return response.data as List<dynamic>;
  }

  Future<List<dynamic>> getAuditeurs() async {
    final response = await _dioClient.instance.get('/api/auditeurs/list');
    return response.data as List<dynamic>;
  }

  Future<List<dynamic>> getReferentiels() async {
    final response = await _dioClient.instance.get(
      '/api/referentiels',
      queryParameters: {'type': 'CONTROLE_TECHNIQUE'},
    );
    return response.data as List<dynamic>;
  }

  Future<void> createMission(Map<String, dynamic> request) async {
    await _dioClient.instance.post('/api/missions', data: request);
  }
}
