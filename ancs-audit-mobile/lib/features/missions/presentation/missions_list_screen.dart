import 'package:flutter/material.dart';
import 'package:go_router/go_router.dart';
import '../../../core/constants/app_colors.dart';
import '../../../core/constants/app_spacing.dart';
import '../../../core/widgets/app_card.dart';
import '../../../core/widgets/status_badge.dart';
import '../../../core/widgets/shimmer_card.dart';
import '../data/mission_repository.dart';
import 'create_mission_screen.dart';

class MissionsListScreen extends StatefulWidget {
  final MissionRepository repository;
  final String userRole;

  const MissionsListScreen({
    Key? key,
    required this.repository,
    required this.userRole,
  }) : super(key: key);

  @override
  State<MissionsListScreen> createState() => _MissionsListScreenState();
}

class _MissionsListScreenState extends State<MissionsListScreen> {
  List<dynamic> _missions = [];
  bool _isLoading = true;
  String? _error;
  int _page = 0;

  @override
  void initState() {
    super.initState();
    _loadMissions();
  }

  Future<void> _loadMissions() async {
    setState(() {
      _isLoading = true;
      _error = null;
    });
    try {
      final data = await widget.repository.getMissions(page: _page);
      setState(() {
        _missions = data;
        _isLoading = false;
      });
    } catch (e) {
      setState(() {
        _error = e.toString();
        _isLoading = false;
      });
    }
  }

  Future<void> _openCreateMission() async {
    final result = await Navigator.of(context).push<bool>(
      MaterialPageRoute(
        builder: (_) => CreateMissionScreen(repository: widget.repository),
      ),
    );
    if (result == true) _loadMissions(); // refresh list after creation
  }

  @override
  Widget build(BuildContext context) {
    final isAdmin = widget.userRole == 'ADMIN_ANCS';
    return Scaffold(
      backgroundColor: AppColors.background,
      appBar: AppBar(
        title: const Text('Missions d\'Audit'),
        actions: [
          IconButton(
            icon: const Icon(Icons.refresh),
            onPressed: _loadMissions,
          ),
        ],
      ),
      floatingActionButton: isAdmin
          ? FloatingActionButton.extended(
              onPressed: _openCreateMission,
              backgroundColor: AppColors.primary,
              foregroundColor: Colors.white,
              icon: const Icon(Icons.add),
              label: const Text('Créer une mission'),
            )
          : null,
      body: _isLoading
          ? const Padding(
              padding: EdgeInsets.all(AppSpacing.m),
              child: ShimmerList(count: 4),
            )
          : _error != null
              ? _buildErrorWidget()
              : _missions.isEmpty
                  ? _buildEmptyWidget()
                  : RefreshIndicator(
                      onRefresh: _loadMissions,
                      child: ListView.builder(
                        padding: const EdgeInsets.all(AppSpacing.m),
                        itemCount: _missions.length,
                        itemBuilder: (context, index) {
                          final mission = _missions[index] as Map<String, dynamic>;
                          return _buildMissionCard(mission);
                        },
                      ),
                    ),
    );
  }

  Widget _buildMissionCard(Map<String, dynamic> mission) {
    final String missionId = mission['id'].toString();
    final String status = mission['statut'] ?? 'PLANIFIEE';
    final bool canAudit = widget.userRole == 'AUDITEUR' && status != 'TERMINEE';
    // Rapport visible for auditor any time it's not PLANIFIEE
    final bool canSeeRapport =
        (widget.userRole == 'AUDITEUR' || widget.userRole == 'ADMIN_ANCS') &&
        status != 'PLANIFIEE';

    return Padding(
      padding: const EdgeInsets.only(bottom: AppSpacing.m),
      child: AppCard(
        onTap: () => context.push('/missions/$missionId', extra: mission),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            Row(
              mainAxisAlignment: MainAxisAlignment.spaceBetween,
              children: [
                Expanded(
                  child: Text(
                    mission['organismeNom'] ?? 'Organisme inconnu',
                    style: Theme.of(context).textTheme.titleMedium?.copyWith(
                          color: AppColors.primary,
                        ),
                    maxLines: 1,
                    overflow: TextOverflow.ellipsis,
                  ),
                ),
                StatusBadge(status: status),
              ],
            ),
            const SizedBox(height: AppSpacing.s),
            Text(
              'Référentiel : ${mission['referentielNom'] ?? '-'}',
              style: Theme.of(context).textTheme.bodyLarge,
            ),
            if (mission['perimetre'] != null) ...[
              const SizedBox(height: 4),
              Text(
                'Périmètre : ${mission['perimetre']}',
                style: Theme.of(context).textTheme.bodySmall,
                maxLines: 1,
                overflow: TextOverflow.ellipsis,
              ),
            ],
            const Divider(height: AppSpacing.l),
            Row(
              mainAxisAlignment: MainAxisAlignment.spaceBetween,
              children: [
                Text(
                  'Début : ${mission['dateDebut'] ?? 'Non planifié'}',
                  style: Theme.of(context).textTheme.bodySmall,
                ),
                if (canAudit)
                  ElevatedButton.icon(
                    onPressed: () => context.push('/missions/$missionId/checklist', extra: mission),
                    style: ElevatedButton.styleFrom(
                      backgroundColor: AppColors.primary,
                      foregroundColor: Colors.white,
                      padding: const EdgeInsets.symmetric(horizontal: AppSpacing.m, vertical: 8),
                      shape: RoundedRectangleBorder(
                        borderRadius: BorderRadius.circular(8),
                      ),
                    ),
                    icon: const Icon(Icons.playlist_add_check, size: 18),
                    label: const Text('Checklist'),
                  )
                else if (canSeeRapport)
                  ElevatedButton.icon(
                    onPressed: () => context.push('/missions/$missionId/rapport', extra: mission),
                    style: ElevatedButton.styleFrom(
                      backgroundColor: AppColors.accent,
                      foregroundColor: Colors.white,
                      padding: const EdgeInsets.symmetric(horizontal: AppSpacing.m, vertical: 8),
                      shape: RoundedRectangleBorder(
                        borderRadius: BorderRadius.circular(8),
                      ),
                    ),
                    icon: const Icon(Icons.description_outlined, size: 18),
                    label: const Text('Rapport'),
                  )
                else
                  Text(
                    'Fin : ${mission['dateFin'] ?? '-'}',
                    style: Theme.of(context).textTheme.bodySmall,
                  ),
              ],
            ),
          ],
        ),
      ),
    );
  }

  Widget _buildErrorWidget() {
    return Center(
      child: Column(
        mainAxisAlignment: MainAxisAlignment.center,
        children: [
          const Icon(Icons.error_outline, size: 48, color: AppColors.nonConforme),
          const SizedBox(height: AppSpacing.m),
          Text(
            'Erreur de chargement des missions',
            style: Theme.of(context).textTheme.titleMedium,
          ),
          const SizedBox(height: AppSpacing.s),
          ElevatedButton(
            onPressed: _loadMissions,
            child: const Text('Réessayer'),
          ),
        ],
      ),
    );
  }

  Widget _buildEmptyWidget() {
    return Center(
      child: Column(
        mainAxisAlignment: MainAxisAlignment.center,
        children: [
          const Icon(Icons.assignment_turned_in_outlined, size: 48, color: AppColors.textSecondary),
          const SizedBox(height: AppSpacing.m),
          Text(
            'Aucune mission d\'audit assignée',
            style: Theme.of(context).textTheme.titleMedium,
          ),
        ],
      ),
    );
  }
}
