import 'package:flutter/material.dart';
import 'package:flutter_bloc/flutter_bloc.dart';
import 'package:go_router/go_router.dart';
import 'package:shared_preferences/shared_preferences.dart';
import 'features/auth/bloc/auth_bloc.dart';
import 'features/auth/bloc/auth_event.dart';
import 'features/auth/bloc/auth_state.dart';
import 'features/auth/data/auth_repository.dart';
import 'features/auth/presentation/login_screen.dart';
import 'features/auth/presentation/totp_screen.dart';
import 'features/dashboard/presentation/main_screen.dart';
import 'features/checklist/presentation/checklist_screen.dart';
import 'features/checklist/data/constat_repository.dart';
import 'features/checklist/data/referentiel_repository.dart';
import 'features/rapports/presentation/rapport_generation_screen.dart';
import 'features/rapports/data/rapport_repository.dart';
import 'core/network/dio_client.dart';
import 'core/network/sync_service.dart';
import 'core/theme/app_theme.dart';
import 'core/constants/app_colors.dart';
import 'package:flutter_localizations/flutter_localizations.dart';

const String kBaseUrl = 'http://10.0.2.2:8080'; // Android emulator → localhost

void main() async {
  WidgetsFlutterBinding.ensureInitialized();
  final dioClient = DioClient(kBaseUrl);
  final authRepository = AuthRepository(dioClient: dioClient);
  runApp(AncsAuditApp(authRepository: authRepository));
}

class AncsAuditApp extends StatefulWidget {
  final AuthRepository authRepository;
  const AncsAuditApp({Key? key, required this.authRepository}) : super(key: key);

  @override
  State<AncsAuditApp> createState() => _AncsAuditAppState();
}

class _AncsAuditAppState extends State<AncsAuditApp> {
  Locale _locale = const Locale('fr');
  late final SyncService _syncService;

  @override
  void initState() {
    super.initState();
    _syncService = SyncService();
  }

  @override
  void dispose() {
    _syncService.dispose();
    super.dispose();
  }

  bool get _isArabic => _locale.languageCode == 'ar';

  @override
  Widget build(BuildContext context) {
    return BlocProvider(
      create: (_) => AuthBloc(authRepository: widget.authRepository)..add(AppStarted()),
      child: BlocBuilder<AuthBloc, AuthState>(
        builder: (context, authState) {
          return MaterialApp.router(
            title: _isArabic ? 'تدقيق ANCS' : 'ANCS Audit',
            debugShowCheckedModeBanner: false,
            theme: AppTheme.buildTheme(_isArabic),
            locale: _locale,
            supportedLocales: const [Locale('fr'), Locale('ar')],
            localizationsDelegates: const [
              GlobalMaterialLocalizations.delegate,
              GlobalWidgetsLocalizations.delegate,
              GlobalCupertinoLocalizations.delegate,
            ],
            routerConfig: _buildRouter(context, authState),
          );
        },
      ),
    );
  }

  GoRouter _buildRouter(BuildContext ctx, AuthState authState) {
    return GoRouter(
      initialLocation: authState is AuthAuthenticated ? '/dashboard' : '/login',
      routes: [
        GoRoute(
          path: '/login',
          builder: (context, state) => const LoginScreen(),
        ),
        GoRoute(
          path: '/login/2fa',
          builder: (context, state) {
            final extraData = state.extra as Map<String, dynamic>? ?? {};
            final email = extraData['email'] as String? ?? '';
            final mfaToken = extraData['mfaToken'] as String? ?? '';
            return TotpScreen(email: email, mfaToken: mfaToken);
          },
        ),
        GoRoute(
          path: '/dashboard',
          builder: (context, state) => MainScreen(
            onLocaleSwitch: () {
              setState(() {
                _locale = _isArabic ? const Locale('fr') : const Locale('ar');
              });
            },
            isArabic: _isArabic,
          ),
        ),
        GoRoute(
          path: '/missions/:id/checklist',
          builder: (context, state) {
            final missionId = state.pathParameters['id']!;
            final mission = state.extra as Map<String, dynamic>? ?? {};
            final referentielId = (mission['referentielId'] ?? '').toString();
            final dioClient = DioClient(kBaseUrl);

            return ChecklistScreen(
              missionId: missionId,
              referentielId: referentielId,
              constatRepository: ConstatRepository(
                dioClient: dioClient,
                db: SyncService().database,
              ),
              referentielRepository: ReferentielRepository(dioClient: dioClient),
              isOnline: true, // par défaut connecté en test
            );
          },
        ),
        GoRoute(
          path: '/missions/:id/rapport',
          builder: (context, state) {
            final missionId = state.pathParameters['id']!;
            final mission = state.extra as Map<String, dynamic>? ?? {};
            final orgNom = mission['organismeNom'] ?? 'Organisme';
            final dioClient = DioClient(kBaseUrl);

            return RapportGenerationScreen(
              missionId: missionId,
              organismeNom: orgNom,
              repository: RapportRepository(dioClient: dioClient),
            );
          },
        ),
      ],
      redirect: (context, state) {
        final authenticated = authState is AuthAuthenticated;
        final loggingIn = state.matchedLocation.startsWith('/login');
        if (!authenticated && !loggingIn) return '/login';
        if (authenticated && loggingIn) return '/dashboard';
        return null;
      },
    );
  }
}

