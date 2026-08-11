import '../../../core/network/dio_client.dart';

class ReunionRepository {
  final DioClient _dioClient;

  ReunionRepository({required DioClient dioClient}) : _dioClient = dioClient;

  Future<void> createReunion(Map<String, dynamic> request) async {
    await _dioClient.instance.post('/api/reunions', data: request);
  }

  Future<List<dynamic>> getReunionsByMission(String missionId) async {
    final response = await _dioClient.instance.get('/api/reunions/mission/$missionId');
    return response.data as List<dynamic>;
  }

  Future<void> deleteReunion(String id) async {
    await _dioClient.instance.delete('/api/reunions/$id');
  }
}
