import 'package:equatable/equatable.dart';

abstract class AuthEvent extends Equatable {
  @override
  List<Object?> get props => [];
}

class LoginSubmitted extends AuthEvent {
  final String email;
  final String password;

  LoginSubmitted({required this.email, required this.password});

  @override
  List<Object?> get props => [email, password];
}

/// Soumission du code 2FA.
///
/// Utilise [mfaToken] (jeton intermédiaire signé côté serveur) au lieu de l'email
/// en clair pour garantir que l'étape 1 (mot de passe) a bien été validée.
class TotpSubmitted extends AuthEvent {
  final String mfaToken;
  final String code;

  TotpSubmitted({required this.mfaToken, required this.code});

  @override
  List<Object?> get props => [mfaToken, code];
}

class LogoutRequested extends AuthEvent {}

class AppStarted extends AuthEvent {}
