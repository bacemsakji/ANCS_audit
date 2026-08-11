import '../../../core/network/dio_client.dart';

class ActionRepository {
  final DioClient _dioClient;
  ActionRepository({required DioClient dioClient}) : _dioClient = dioClient;

  Future<void> createAction(Map<String, dynamic> request) async {
    await _dioClient.instance.post('/api/actions', data: request);
  }

  Future<List<dynamic>> getConstatsByMission(String missionId) async {
    final response = await _dioClient.instance.get('/api/constats/mission/$missionId');
    return response.data as List<dynamic>;
  }

  Future<List<dynamic>> getActionsForMission(String missionId,
      {int page = 0}) async {
    final response = await _dioClient.instance.get(
      '/api/actions/mission/$missionId',
      queryParameters: {'page': page, 'size': 50},
    );
    final data = response.data;
    if (data is Map && data.containsKey('content'))
      return data['content'] as List<dynamic>;
    return data is List ? data : [];
  }

  Future<List<dynamic>> getActiveActionsRssi() async {
    final response = await _dioClient.instance.get('/api/actions/rssi/actives');
    return response.data as List<dynamic>;
  }

  Future<void> updateStatus(String id, String statut) async {
    await _dioClient.instance.patch(
      '/api/actions/$id/statut',
      queryParameters: {'statut': statut},
    );
  }
}
