import '../../../core/network/dio_client.dart';

class DashboardRepository {
  final DioClient _dioClient;
  DashboardRepository({required DioClient dioClient}) : _dioClient = dioClient;

  Future<Map<String, dynamic>> getAdminDashboard() async {
    final response = await _dioClient.instance.get('/api/dashboard/admin');
    return response.data as Map<String, dynamic>;
  }

  Future<Map<String, dynamic>> getRssiDashboard() async {
    final response = await _dioClient.instance.get('/api/dashboard/rssi');
    return response.data as Map<String, dynamic>;
  }
}
