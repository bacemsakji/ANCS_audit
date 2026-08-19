import 'package:flutter/material.dart';
import 'package:flutter_bloc/flutter_bloc.dart';
import '../../../core/constants/app_spacing.dart';
import '../../../core/network/dio_client.dart';
import '../../auth/bloc/auth_bloc.dart';
import '../../auth/bloc/auth_event.dart';
import '../../auth/bloc/auth_state.dart';
import '../data/dashboard_repository.dart';
import '../../missions/data/mission_repository.dart';
import '../../missions/presentation/missions_list_screen.dart';
import '../../actions/data/action_repository.dart';
import '../../actions/presentation/actions_list_screen.dart';
import 'dashboard_admin_screen.dart';
import 'dashboard_rssi_screen.dart';

class MainScreen extends StatefulWidget {
  final VoidCallback onLocaleSwitch;
  final bool isArabic;

  const MainScreen({
    Key? key,
    required this.onLocaleSwitch,
    required this.isArabic,
  }) : super(key: key);

  @override
  State<MainScreen> createState() => _MainScreenState();
}

class _MainScreenState extends State<MainScreen> {
  int _selectedIndex = 0;
  late final DioClient _dioClient;
  late final DashboardRepository _dashboardRepository;
  late final MissionRepository _missionRepository;
  late final ActionRepository _actionRepository;

  @override
  void initState() {
    super.initState();
    _dioClient = DioClient();
    _dashboardRepository = DashboardRepository(dioClient: _dioClient);
    _missionRepository = MissionRepository(dioClient: _dioClient);
    _actionRepository = ActionRepository(dioClient: _dioClient);
  }

  List<Widget> _buildScreens(String role) {
    if (role == 'ADMIN_ANCS') {
      return [
        DashboardAdminScreen(repository: _dashboardRepository),
        MissionsListScreen(repository: _missionRepository, userRole: role),
      ];
    } else if (role == 'RSSI') {
      return [
        DashboardRssiScreen(repository: _dashboardRepository),
        ActionsListScreen(repository: _actionRepository, userRole: role),
      ];
    } else {
      // Rôle: AUDITEUR
      return [
        MissionsListScreen(repository: _missionRepository, userRole: role),
      ];
    }
  }

  List<NavigationDestination> _buildNavDestinations(String role) {
    final String homeLabel =
        widget.isArabic ? 'لوحة التحكم' : 'Tableau de bord';
    final String missionsLabel = widget.isArabic ? 'المهمات' : 'Missions';
    final String actionsLabel = widget.isArabic ? 'الإجراءات' : 'Actions';

    if (role == 'ADMIN_ANCS') {
      return [
        NavigationDestination(
          icon: const Icon(Icons.dashboard_outlined),
          selectedIcon: const Icon(Icons.dashboard),
          label: homeLabel,
        ),
        NavigationDestination(
          icon: const Icon(Icons.assignment_outlined),
          selectedIcon: const Icon(Icons.assignment),
          label: missionsLabel,
        ),
      ];
    } else if (role == 'RSSI') {
      return [
        NavigationDestination(
          icon: const Icon(Icons.business_center_outlined),
          selectedIcon: const Icon(Icons.business_center),
          label: homeLabel,
        ),
        NavigationDestination(
          icon: const Icon(Icons.playlist_add_check_outlined),
          selectedIcon: const Icon(Icons.playlist_add_check),
          label: actionsLabel,
        ),
      ];
    } else {
      // AUDITEUR
      return [
        NavigationDestination(
          icon: const Icon(Icons.assignment_outlined),
          selectedIcon: const Icon(Icons.assignment),
          label: missionsLabel,
        ),
      ];
    }
  }

  @override
  Widget build(BuildContext context) {
    return BlocBuilder<AuthBloc, AuthState>(
      builder: (context, authState) {
        String role = 'AUDITEUR';
        String userNom = 'Auditeur';
        if (authState is AuthAuthenticated) {
          role = authState.role;
          userNom = authState.nom;
        }

        final screens = _buildScreens(role);
        final navDestinations = _buildNavDestinations(role);

        // Protection de débordement d'index lors des changements de profil
        if (_selectedIndex >= screens.length) {
          _selectedIndex = 0;
        }

        final String userRoleText = switch (role) {
          'ADMIN_ANCS' =>
            widget.isArabic ? 'مدير الوكالة' : 'Administrateur ANCS',
          'RSSI' => widget.isArabic ? 'مسؤول السلامة' : 'RSSI',
          _ => widget.isArabic ? 'مدقق معتمد' : 'Auditeur ANCS',
        };

        return Scaffold(
          appBar: AppBar(
            title: Row(
              children: [
                ClipRRect(
                  borderRadius: BorderRadius.circular(6),
                  child: Image.asset(
                    'assets/images/logo--ancs.png',
                    width: 32,
                    height: 32,
                    fit: BoxFit.contain,
                  ),
                ),
                const SizedBox(width: AppSpacing.s),
                Expanded(
                  child: Column(
                    crossAxisAlignment: CrossAxisAlignment.start,
                    children: [
                      Text(
                        userNom,
                        style: const TextStyle(
                            fontSize: 14,
                            fontWeight: FontWeight.bold,
                            color: Colors.white),
                        overflow: TextOverflow.ellipsis,
                      ),
                      Text(
                        userRoleText,
                        style: TextStyle(
                            fontSize: 10,
                            color: Colors.white.withValues(alpha: 0.8)),
                      ),
                    ],
                  ),
                ),
              ],
            ),
            actions: [
              IconButton(
                icon: const Icon(Icons.language),
                tooltip: widget.isArabic ? 'تغيير اللغة' : 'Changer la langue',
                onPressed: widget.onLocaleSwitch,
              ),
              IconButton(
                icon: const Icon(Icons.logout),
                tooltip: widget.isArabic ? 'تسجيل الخروج' : 'Déconnexion',
                onPressed: () {
                  context.read<AuthBloc>().add(LogoutRequested());
                },
              ),
            ],
          ),
          body: IndexedStack(
            index: _selectedIndex,
            children: screens,
          ),
          bottomNavigationBar: navDestinations.length <= 1
              ? null
              : NavigationBar(
                  selectedIndex: _selectedIndex,
                  onDestinationSelected: (index) =>
                      setState(() => _selectedIndex = index),
                  destinations: navDestinations,
                ),
        );
      },
    );
  }
}
