import 'package:flutter/material.dart';
import 'package:go_router/go_router.dart';
import 'package:shared_preferences/shared_preferences.dart';

class AppRouter {
  static final GoRouter router = GoRouter(
    initialLocation: '/login',
    routes: [
      GoRoute(
        path: '/login',
        builder: (context, state) => const Scaffold(
          body: Center(child: Text('Écran de Connexion (Phase 6)')),
        ),
      ),
      GoRoute(
        path: '/dashboard',
        builder: (context, state) => const Scaffold(
          body: Center(child: Text('Tableau de Bord (Phase 6)')),
        ),
      ),
    ],
    redirect: (context, state) async {
      final prefs = await SharedPreferences.getInstance();
      final String? token = prefs.getString('access_token');
      final loggingIn = state.matchedLocation == '/login';

      if (token == null && !loggingIn) {
        return '/login';
      }
      if (token != null && loggingIn) {
        return '/dashboard';
      }
      return null;
    },
  );
}
