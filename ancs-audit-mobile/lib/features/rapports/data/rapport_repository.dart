import 'package:dio/dio.dart';
import '../../../core/network/dio_client.dart';

class RapportRepository {
  final DioClient _dioClient;
  RapportRepository({required DioClient dioClient}) : _dioClient = dioClient;

  Future<Map<String, dynamic>> generateSyntheseIa(String missionId, String langue) async {
    final response = await _dioClient.instance.post(
      '/api/rapports/missions/$missionId/synthese-ia',
      queryParameters: {'langue': langue},
    );
    return response.data as Map<String, dynamic>;
  }

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

  Future<String> getDownloadUrl(String rapportId) async {
    final response = await _dioClient.instance.get('/api/rapports/$rapportId/download');
    return response.data['downloadUrl'] as String;
  }
}
