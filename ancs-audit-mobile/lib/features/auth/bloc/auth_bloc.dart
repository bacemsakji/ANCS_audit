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

      if (result['mfaRequired'] == true) {
        // SÉCURITÉ : on passe le mfaToken signé (pas l'email) pour lier
        // la validation 2FA à cette session de connexion réussie.
        final mfaToken = result['mfaToken'] as String? ?? '';
        final email = result['email'] as String? ?? '';
        emit(AuthMfaRequired(email: email, mfaToken: mfaToken));
      } else {
        await _storeTokens(result);
        emit(AuthAuthenticated(
          email: result['email'] ?? '',
          role: result['role'] ?? '',
          nom: result['name'] ?? '',
        ));
      }
    } catch (e) {
      emit(AuthError(message: _parseError(e)));
    }
  }

  Future<void> _onTotpSubmitted(
      TotpSubmitted event, Emitter<AuthState> emit) async {
    emit(AuthLoading());
    try {
      // SÉCURITÉ : utilise mfaToken (pas l'email) — le serveur valide la signature
      // avant d'accepter le code TOTP.
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
