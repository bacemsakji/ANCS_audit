import 'package:flutter/material.dart';
import 'package:flutter_bloc/flutter_bloc.dart';
import 'package:go_router/go_router.dart';
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
import 'features/missions/presentation/mission_detail_screen.dart';
import 'features/missions/data/mission_repository.dart';
import 'core/network/dio_client.dart';
import 'core/network/sync_service.dart';
import 'core/theme/app_theme.dart';
import 'core/local_db/app_database.dart';
import 'l10n/app_localizations.dart';

// L'URL de base est configurée via --dart-define=API_BASE_URL=https://api.ancs.gov.tn
// à la compilation. La valeur par défaut (localhost) est définie dans dio_client.dart.
// Ne pas redéfinir kBaseUrl ici — utiliser directement DioClient() sans argument.
//
// Exemple de build de production :
//   flutter build apk --dart-define=API_BASE_URL=https://api.ancs.gov.tn

void main() async {
  WidgetsFlutterBinding.ensureInitialized();
  // Une seule instance de DioClient, partagée avec tous les repositories et
  // le SyncService — garantit que le mécanisme --dart-define est appliqué
  // de manière cohérente dans toute l'application.
  final dioClient = DioClient();
  final authRepository = AuthRepository(dioClient: dioClient);
  final appDatabase = AppDatabase();
  runApp(AncsAuditApp(
    dioClient: dioClient,
    authRepository: authRepository,
    appDatabase: appDatabase,
  ));
}

class AncsAuditApp extends StatefulWidget {
  final DioClient dioClient;
  final AuthRepository authRepository;
  final AppDatabase appDatabase;
  const AncsAuditApp({
    Key? key,
    required this.dioClient,
    required this.authRepository,
    required this.appDatabase,
  }) : super(key: key);

  @override
  State<AncsAuditApp> createState() => _AncsAuditAppState();
}

class _AncsAuditAppState extends State<AncsAuditApp> {
  Locale _locale = const Locale('fr');
  late final SyncService _syncService;

  @override
  void initState() {
    super.initState();
    // SyncService reçoit le même DioClient que le reste de l'application —
    // la synchronisation arrière-plan cible exactement la même URL que l'UI.
    _syncService = SyncService(
      database: widget.appDatabase,
      dioClient: widget.dioClient,
    );
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
            supportedLocales: AppLocalizations.supportedLocales,
            localizationsDelegates: AppLocalizations.localizationsDelegates,
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
          path: '/missions/:id',
          builder: (context, state) {
            final mission = state.extra as Map<String, dynamic>? ?? {};
            final userRole = authState is AuthAuthenticated ? authState.role : 'AUDITEUR';
            return MissionDetailScreen(mission: mission, userRole: userRole);
          },
        ),
        GoRoute(
          path: '/missions/:id/checklist',
          builder: (context, state) {
            final missionId = state.pathParameters['id']!;
            final mission = state.extra as Map<String, dynamic>? ?? {};
            final referentielId = (mission['referentielId'] ?? '').toString();

            // Le DioClient partagé est réutilisé — pas de nouvelle instance ici.
            return ChecklistScreen(
              missionId: missionId,
              referentielId: referentielId,
              constatRepository: ConstatRepository(
                dioClient: widget.dioClient,
                db: widget.appDatabase,
              ),
              referentielRepository:
                  ReferentielRepository(dioClient: widget.dioClient),
              missionRepository: MissionRepository(dioClient: widget.dioClient),
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

            return RapportGenerationScreen(
              missionId: missionId,
              organismeNom: orgNom,
              repository: RapportRepository(dioClient: widget.dioClient),
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
