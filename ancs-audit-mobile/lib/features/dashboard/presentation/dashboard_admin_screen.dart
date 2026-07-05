import 'package:flutter/material.dart';
import '../../../core/constants/app_colors.dart';
import '../../../core/constants/app_spacing.dart';
import '../../../core/widgets/app_card.dart';
import '../../../core/widgets/conformite_gauge.dart';
import '../../../core/widgets/shimmer_card.dart';
import '../data/dashboard_repository.dart';

class DashboardAdminScreen extends StatefulWidget {
  final DashboardRepository repository;
  const DashboardAdminScreen({Key? key, required this.repository}) : super(key: key);

  @override
  State<DashboardAdminScreen> createState() => _DashboardAdminScreenState();
}

class _DashboardAdminScreenState extends State<DashboardAdminScreen> {
  Map<String, dynamic>? _data;
  bool _isLoading = true;
  String? _error;

  @override
  void initState() {
    super.initState();
    _load();
  }

  Future<void> _load() async {
    setState(() { _isLoading = true; _error = null; });
    try {
      final data = await widget.repository.getAdminDashboard();
      setState(() { _data = data; _isLoading = false; });
    } catch (e) {
      setState(() { _error = e.toString(); _isLoading = false; });
    }
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      backgroundColor: AppColors.background,
      appBar: AppBar(
        title: const Text('Tableau de Bord — Administration'),
        actions: [
          IconButton(icon: const Icon(Icons.refresh), onPressed: _load),
        ],
      ),
      body: _isLoading
          ? const Padding(
              padding: EdgeInsets.all(AppSpacing.m),
              child: ShimmerList(count: 5),
            )
          : _error != null
              ? _buildError()
              : RefreshIndicator(
                  onRefresh: _load,
                  child: ListView(
                    padding: const EdgeInsets.all(AppSpacing.m),
                    children: [
                      _buildKpiGrid(),
                      const SizedBox(height: AppSpacing.m),
                      _buildConformiteCard(),
                      const SizedBox(height: AppSpacing.m),
                      _buildAlertesCertification(),
                      const SizedBox(height: AppSpacing.m),
                      _buildMissionsRecentes(),
                    ],
                  ),
                ),
    );
  }

  Widget _buildKpiGrid() {
    final d = _data!;
    return GridView.count(
      crossAxisCount: 2,
      shrinkWrap: true,
      physics: const NeverScrollableScrollPhysics(),
      crossAxisSpacing: AppSpacing.m,
      mainAxisSpacing: AppSpacing.m,
      childAspectRatio: 1.5,
      children: [
        _KpiCard(
          label: 'Missions en cours',
          value: '${d['totalMissionsEnCours'] ?? 0}',
          icon: Icons.assignment_outlined,
          color: AppColors.accent,
        ),
        _KpiCard(
          label: 'Missions planifiées',
          value: '${d['totalMissionsPlanifiees'] ?? 0}',
          icon: Icons.calendar_today_outlined,
          color: AppColors.primary,
        ),
        _KpiCard(
          label: 'Organismes',
          value: '${d['totalOrganismes'] ?? 0}',
          icon: Icons.business_outlined,
          color: const Color(0xFF455A64),
        ),
        _KpiCard(
          label: 'Auditeurs actifs',
          value: '${d['totalAuditeursActifs'] ?? 0}',
          icon: Icons.verified_user_outlined,
          color: AppColors.conforme,
        ),
      ],
    );
  }

  Widget _buildConformiteCard() {
    final taux = (_data!['tauxConformiteGlobal'] as num?)?.toDouble() ?? 0.0;
    return AppCard(
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Text('Conformité globale', style: Theme.of(context).textTheme.titleMedium),
          const SizedBox(height: AppSpacing.m),
          ConformiteGauge(tauxConformite: taux),
        ],
      ),
    );
  }

  Widget _buildAlertesCertification() {
    final count = _data!['alertesCertificationExpiration'] as int? ?? 0;
    if (count == 0) return const SizedBox.shrink();
    return AppCard(
      color: AppColors.nonConforme.withOpacity(0.05),
      child: Row(
        children: [
          const Icon(Icons.warning_amber_rounded, color: AppColors.nonConforme, size: 28),
          const SizedBox(width: AppSpacing.m),
          Expanded(
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                Text(
                  'Alertes de certification',
                  style: Theme.of(context).textTheme.titleMedium?.copyWith(
                    color: AppColors.nonConforme,
                  ),
                ),
                Text(
                  '$count auditeur(s) avec certification expirant dans 30 jours',
                  style: Theme.of(context).textTheme.bodySmall,
                ),
              ],
            ),
          ),
        ],
      ),
    );
  }

  Widget _buildMissionsRecentes() {
    final missions = _data!['missionsRecentes'] as List<dynamic>? ?? [];
    if (missions.isEmpty) return const SizedBox.shrink();
    return AppCard(
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Text('Missions récentes', style: Theme.of(context).textTheme.titleMedium),
          const SizedBox(height: AppSpacing.m),
          ...missions.map((m) => _MissionTile(mission: m as Map<String, dynamic>)),
        ],
      ),
    );
  }

  Widget _buildError() => Center(
        child: Column(
          mainAxisAlignment: MainAxisAlignment.center,
          children: [
            const Icon(Icons.cloud_off, size: 48, color: AppColors.textSecondary),
            const SizedBox(height: AppSpacing.m),
            Text('Données non disponibles', style: Theme.of(context).textTheme.titleMedium),
            TextButton(onPressed: _load, child: const Text('Réessayer')),
          ],
        ),
      );
}

class _KpiCard extends StatelessWidget {
  final String label;
  final String value;
  final IconData icon;
  final Color color;

  const _KpiCard({
    required this.label,
    required this.value,
    required this.icon,
    required this.color,
  });

  @override
  Widget build(BuildContext context) {
    return AppCard(
      padding: const EdgeInsets.all(AppSpacing.m),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        mainAxisAlignment: MainAxisAlignment.spaceBetween,
        children: [
          Icon(icon, color: color, size: 28),
          Column(
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              Text(
                value,
                style: TextStyle(
                  fontSize: 24,
                  fontWeight: FontWeight.bold,
                  color: color,
                ),
              ),
              Text(
                label,
                style: Theme.of(context).textTheme.bodySmall,
                maxLines: 2,
              ),
            ],
          ),
        ],
      ),
    );
  }
}

class _MissionTile extends StatelessWidget {
  final Map<String, dynamic> mission;
  const _MissionTile({required this.mission});

  @override
  Widget build(BuildContext context) {
    return Padding(
      padding: const EdgeInsets.only(bottom: AppSpacing.s),
      child: Row(
        children: [
          const Icon(Icons.chevron_right, color: AppColors.accent, size: 18),
          const SizedBox(width: AppSpacing.s),
          Expanded(
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                Text(
                  mission['organismeNom'] ?? '-',
                  style: Theme.of(context).textTheme.bodyLarge?.copyWith(fontWeight: FontWeight.w500),
                  maxLines: 1,
                  overflow: TextOverflow.ellipsis,
                ),
                Text(
                  'Auditeur : ${mission['auditeurNom'] ?? '-'} • ${mission['statut'] ?? ''}',
                  style: Theme.of(context).textTheme.bodySmall,
                ),
              ],
            ),
          ),
        ],
      ),
    );
  }
}
