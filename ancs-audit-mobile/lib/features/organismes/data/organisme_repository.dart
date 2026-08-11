import '../../../core/network/dio_client.dart';

class OrganismeRepository {
  final DioClient _dioClient;
  
  OrganismeRepository({required DioClient dioClient}) : _dioClient = dioClient;

  Future<void> createOrganisme(Map<String, dynamic> request) async {
    await _dioClient.instance.post('/api/organismes', data: request);
  }

  Future<List<dynamic>> getOrganismes() async {
    final response = await _dioClient.instance.get('/api/organismes/list');
    return response.data as List<dynamic>;
  }
}
