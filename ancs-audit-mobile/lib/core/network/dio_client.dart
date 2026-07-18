import 'package:dio/dio.dart';
import 'package:shared_preferences/shared_preferences.dart';

/// URL de base de l'API.
///
/// Peut être surchargée au moment de la compilation avec :
///   flutter run --dart-define=API_BASE_URL=https://api.ancs.gov.tn
///   flutter build apk --dart-define=API_BASE_URL=https://api.ancs.gov.tn
///
/// Valeur par défaut : loopback de l'émulateur Android (développement local).
/// Ne JAMAIS mettre une URL de production comme valeur par défaut ici —
/// utiliser toujours la variable --dart-define en CI/CD.
const String kBaseUrl = String.fromEnvironment(
  'API_BASE_URL',
  defaultValue: 'http://localhost:8081',
);

class DioClient {
  late final Dio _dio;

  DioClient([String? baseUrl]) {
    _dio = Dio(BaseOptions(
      baseUrl: baseUrl ?? kBaseUrl,
      connectTimeout: const Duration(seconds: 10),
      receiveTimeout: const Duration(seconds: 10),
      headers: {
        'Content-Type': 'application/json',
        'Accept': 'application/json',
      },
    ));

    _dio.interceptors.add(
      InterceptorsWrapper(
        onRequest: (options, handler) async {
          final prefs = await SharedPreferences.getInstance();
          final String? token = prefs.getString('access_token');
          if (token != null) {
            options.headers['Authorization'] = 'Bearer $token';
          }
          return handler.next(options);
        },

        onError: (DioException e, handler) async {
          // Tentative de rafraîchissement automatique du token sur 401
          if (e.response?.statusCode == 401) {
            final prefs = await SharedPreferences.getInstance();
            final String? refreshToken = prefs.getString('refresh_token');

            // Si pas de refresh token disponible, on laisse passer le 401
            if (refreshToken == null) {
              return handler.next(e);
            }

            try {
              // Appel au endpoint refresh sans intercepteur (pour éviter une boucle)
              final refreshDio = Dio(BaseOptions(baseUrl: _dio.options.baseUrl));
              final response = await refreshDio.post(
                '/api/auth/refresh',
                data: {'refreshToken': refreshToken},
              );

              final newAccessToken = response.data['accessToken'] as String?;
              final newRefreshToken = response.data['refreshToken'] as String?;

              if (newAccessToken == null) {
                // Refresh token expiré — forcer la déconnexion
                await _clearSession(prefs);
                return handler.next(e);
              }

              // Persister les nouveaux tokens
              await prefs.setString('access_token', newAccessToken);
              if (newRefreshToken != null) {
                await prefs.setString('refresh_token', newRefreshToken);
              }

              // Rejouer la requête originale avec le nouveau token
              final retryOptions = e.requestOptions;
              retryOptions.headers['Authorization'] = 'Bearer $newAccessToken';
              final retryResponse = await _dio.fetch(retryOptions);
              return handler.resolve(retryResponse);
            } catch (_) {
              // Refresh échoué (token expiré côté serveur ou réseau) — déconnexion
              await _clearSession(await SharedPreferences.getInstance());
              return handler.next(e);
            }
          }
          return handler.next(e);
        },
      ),
    );
  }

  Future<void> _clearSession(SharedPreferences prefs) async {
    await prefs.remove('access_token');
    await prefs.remove('refresh_token');
    await prefs.remove('user_email');
    await prefs.remove('user_role');
    await prefs.remove('user_nom');
  }

  Dio get instance => _dio;
}
