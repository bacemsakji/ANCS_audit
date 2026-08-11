import '../../../core/network/dio_client.dart';

class AuditeurRepository {
  final DioClient _dioClient;
  
  AuditeurRepository({required DioClient dioClient}) : _dioClient = dioClient;

  Future<void> certifyAuditeur(Map<String, dynamic> request) async {
    await _dioClient.instance.post('/api/auditeurs', data: request);
  }

  Future<List<dynamic>> getUtilisateurs() async {
    final response = await _dioClient.instance.get('/api/users');
    return response.data as List<dynamic>;
  }
}
