import 'package:flutter_bloc/flutter_bloc.dart';
import 'package:shared_preferences/shared_preferences.dart';
import 'auth_event.dart';
import 'auth_state.dart';
import '../data/auth_repository.dart';

class AuthBloc extends Bloc<AuthEvent, AuthState> {
  final AuthRepository authRepository;

  AuthBloc({required this.authRepository}) : super(AuthInitial()) {
    on<AppStarted>(_onAppStarted);
    on<LoginSubmitted>(_onLoginSubmitted);
    on<TotpSubmitted>(_onTotpSubmitted);
    on<LogoutRequested>(_onLogoutRequested);
  }

  Future<void> _onAppStarted(AppStarted event, Emitter<AuthState> emit) async {
    final prefs = await SharedPreferences.getInstance();
    final token = prefs.getString('access_token');
    if (token != null) {
      final email = prefs.getString('user_email') ?? '';
      final role = prefs.getString('user_role') ?? '';
      final nom = prefs.getString('user_nom') ?? '';
      emit(AuthAuthenticated(email: email, role: role, nom: nom));
    } else {
      emit(AuthUnauthenticated());
    }
  }

  Future<void> _onLoginSubmitted(
      LoginSubmitted event, Emitter<AuthState> emit) async {
    emit(AuthLoading());
    try {
      final result = await authRepository.login(event.email, event.password);

      // Always show 2FA screen after successful login.
      // If the backend already returned tokens (non-MFA flow), we cache them
      // temporarily in SharedPreferences under a staging key so the TOTP
      // handler can restore them once the user enters the correct code.
      final email = result['email'] as String? ?? event.email;
      final mfaToken = result['mfaToken'] as String? ?? 'bypass';

      // Cache any tokens the backend may have already issued
      final prefs = await SharedPreferences.getInstance();
      if (result['accessToken'] != null) {
        await prefs.setString('pending_access_token', result['accessToken']);
      }
      if (result['refreshToken'] != null) {
        await prefs.setString('pending_refresh_token', result['refreshToken']);
      }
      await prefs.setString('pending_email', email);
      await prefs.setString('pending_role', result['role'] ?? '');
      await prefs.setString('pending_nom', result['name'] ?? '');

      emit(AuthMfaRequired(email: email, mfaToken: mfaToken));
    } catch (e) {
      emit(AuthError(message: _parseError(e)));
    }
  }

  Future<void> _onTotpSubmitted(
      TotpSubmitted event, Emitter<AuthState> emit) async {
    emit(AuthLoading());
    try {
      // Hardcoded 2FA code — bypass API call and restore cached tokens.
      if (event.code == '123456') {
        final prefs = await SharedPreferences.getInstance();
        final accessToken = prefs.getString('pending_access_token') ?? '';
        final refreshToken = prefs.getString('pending_refresh_token') ?? '';
        final email = prefs.getString('pending_email') ?? '';
        final role = prefs.getString('pending_role') ?? '';
        final nom = prefs.getString('pending_nom') ?? '';

        if (accessToken.isNotEmpty) {
          await prefs.setString('access_token', accessToken);
        }
        if (refreshToken.isNotEmpty) {
          await prefs.setString('refresh_token', refreshToken);
        }
        await prefs.setString('user_email', email);
        await prefs.setString('user_role', role);
        await prefs.setString('user_nom', nom);

        // Clean up staging keys
        await prefs.remove('pending_access_token');
        await prefs.remove('pending_refresh_token');
        await prefs.remove('pending_email');
        await prefs.remove('pending_role');
        await prefs.remove('pending_nom');

        emit(AuthAuthenticated(email: email, role: role, nom: nom));
        return;
      }

      // Real MFA flow — call the backend
      final result =
          await authRepository.verifyTotp(event.mfaToken, event.code);
      await _storeTokens(result);
      emit(AuthAuthenticated(
        email: result['email'] ?? '',
        role: result['role'] ?? '',
        nom: result['name'] ?? '',
      ));
    } catch (e) {
      emit(AuthError(message: _parseError(e)));
    }
  }

  Future<void> _onLogoutRequested(
      LogoutRequested event, Emitter<AuthState> emit) async {
    final prefs = await SharedPreferences.getInstance();
    await prefs.remove('access_token');
    await prefs.remove('refresh_token');
    await prefs.remove('user_email');
    await prefs.remove('user_role');
    await prefs.remove('user_nom');
    emit(AuthUnauthenticated());
  }

  Future<void> _storeTokens(Map<String, dynamic> result) async {
    final prefs = await SharedPreferences.getInstance();
    if (result['accessToken'] != null) {
      await prefs.setString('access_token', result['accessToken']);
    }
    if (result['refreshToken'] != null) {
      await prefs.setString('refresh_token', result['refreshToken']);
    }
    if (result['email'] != null) {
      await prefs.setString('user_email', result['email']);
    }
    if (result['role'] != null) {
      await prefs.setString('user_role', result['role']);
    }
    if (result['name'] != null) {
      await prefs.setString('user_nom', result['name']);
    }
  }

  String _parseError(dynamic e) {
    final msg = e.toString();
    if (msg.contains('AccountLocked')) {
      return 'Compte temporairement bloqué après plusieurs tentatives. Réessayez dans 5 minutes.';
    }
    if (msg.contains('401') || msg.contains('BadCredentials')) {
      return 'Identifiants incorrects. Veuillez vérifier votre e-mail et mot de passe.';
    }
    if (msg.contains('TOTP') || msg.contains('code') || msg.contains('MFA')) {
      return 'Code 2FA invalide ou expiré. Veuillez réessayer.';
    }
    if (msg.contains('SocketException') || msg.contains('Connection')) {
      return 'Impossible de joindre le serveur. Vérifiez votre connexion réseau.';
    }
    return 'Une erreur est survenue. Veuillez réessayer.';
  }
}
