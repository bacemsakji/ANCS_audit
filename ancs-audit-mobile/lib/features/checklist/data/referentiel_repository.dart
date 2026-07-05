import 'package:dio/dio.dart';
import '../../../core/network/dio_client.dart';

class ReferentielRepository {
  final DioClient _dioClient;
  ReferentielRepository({required DioClient dioClient}) : _dioClient = dioClient;

  Future<List<dynamic>> getControlesByReferentiel(String referentielId) async {
    final response = await _dioClient.instance.get('/api/referentiels/$referentielId/controles');
    return response.data as List<dynamic>;
  }

  Future<List<dynamic>> getReferentiels() async {
    final response = await _dioClient.instance.get('/api/referentiels');
    return response.data as List<dynamic>;
  }
}
