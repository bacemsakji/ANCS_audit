import 'package:equatable/equatable.dart';

class AuthState extends Equatable {
  @override
  List<Object?> get props => [];
}

class AuthInitial extends AuthState {}

class AuthLoading extends AuthState {}

/// Authentification réussie — les tokens ont été stockés localement.
class AuthAuthenticated extends AuthState {
  final String email;
  final String role;
  final String nom;

  AuthAuthenticated({
    required this.email,
    required this.role,
    required this.nom,
  });

  @override
  List<Object?> get props => [email, role, nom];
}

/// Étape intermédiaire : le mot de passe est vérifié, la 2FA est requise (admin ANCS).
class AuthMfaRequired extends AuthState {
  final String email;
  final String mfaToken;

  AuthMfaRequired({required this.email, required this.mfaToken});

  @override
  List<Object?> get props => [email, mfaToken];
}

class AuthUnauthenticated extends AuthState {}

class AuthError extends AuthState {
  final String message;

  AuthError({required this.message});

  @override
  List<Object?> get props => [message];
}
