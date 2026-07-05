import 'package:dio/dio.dart';
import '../../../core/network/dio_client.dart';

class AuthRepository {
  final DioClient _dioClient;

  AuthRepository({required DioClient dioClient}) : _dioClient = dioClient;

  Future<Map<String, dynamic>> login(String email, String password) async {
    try {
      final response = await _dioClient.instance.post(
        '/api/auth/login',
        data: {'email': email, 'password': password},
      );
      return response.data as Map<String, dynamic>;
    } on DioException catch (e) {
      _handleError(e);
      rethrow;
    }
  }

  /// Vérifie le code TOTP via le [mfaToken] intermédiaire (jamais l'email en clair).
  ///
  /// Le serveur valide la signature du [mfaToken] pour confirmer que le mot de passe
  /// a bien été vérifié à l'étape précédente avant d'accepter le code TOTP.
  Future<Map<String, dynamic>> verifyTotp(
      String mfaToken, String code) async {
    try {
      final response = await _dioClient.instance.post(
        '/api/auth/2fa/verify',
        data: {'mfaToken': mfaToken, 'code': code},
      );
      return response.data as Map<String, dynamic>;
    } on DioException catch (e) {
      _handleError(e);
      rethrow;
    }
  }

  /// Régénère un couple de tokens via le refresh token passé dans le corps JSON.
  /// Le refresh token ne doit jamais transiter en query string (logs serveur).
  Future<Map<String, dynamic>> refreshToken(String refreshToken) async {
    try {
      final response = await _dioClient.instance.post(
        '/api/auth/refresh',
        data: {'refreshToken': refreshToken},
      );
      return response.data as Map<String, dynamic>;
    } on DioException catch (e) {
      _handleError(e);
      rethrow;
    }
  }

  void _handleError(DioException e) {
    if (e.response?.statusCode == 401) {
      throw Exception('BadCredentials: ${e.response?.data}');
    }
    if (e.response?.statusCode == 423) {
      throw Exception('AccountLocked: Compte temporairement bloqué');
    }
    throw Exception('Erreur réseau: ${e.message}');
  }
}
