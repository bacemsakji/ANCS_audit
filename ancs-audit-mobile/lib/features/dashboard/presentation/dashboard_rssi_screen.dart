import 'package:flutter/material.dart';
import '../../../core/constants/app_colors.dart';
import '../../../core/constants/app_spacing.dart';
import '../../../core/widgets/app_card.dart';
import '../../../core/widgets/conformite_gauge.dart';
import '../../../core/widgets/shimmer_card.dart';
import '../../../core/widgets/status_badge.dart';
import '../data/dashboard_repository.dart';

class DashboardRssiScreen extends StatefulWidget {
  final DashboardRepository repository;
  const DashboardRssiScreen({Key? key, required this.repository}) : super(key: key);

  @override
  State<DashboardRssiScreen> createState() => _DashboardRssiScreenState();
}

class _DashboardRssiScreenState extends State<DashboardRssiScreen> {
  Map<String, dynamic>? _data;
  bool _isLoading = true;

  @override
  void initState() {
    super.initState();
    _load();
  }

  Future<void> _load() async {
    setState(() => _isLoading = true);
    try {
      final data = await widget.repository.getRssiDashboard();
      setState(() { _data = data; _isLoading = false; });
    } catch (e) {
      setState(() => _isLoading = false);
    }
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      backgroundColor: AppColors.background,
      appBar: AppBar(title: Text(_data?['organismeNom'] ?? 'Mon Tableau de Bord')),
      body: _isLoading
          ? const Padding(
              padding: EdgeInsets.all(AppSpacing.m),
              child: ShimmerList(count: 4),
            )
          : RefreshIndicator(
              onRefresh: _load,
              child: ListView(
                padding: const EdgeInsets.all(AppSpacing.m),
                children: [
                  _buildScoreCard(),
                  const SizedBox(height: AppSpacing.m),
                  _buildActionsStats(),
                  const SizedBox(height: AppSpacing.m),
                  _buildActionsUrgentes(),
                ],
              ),
            ),
    );
  }

  Widget _buildScoreCard() {
    final score = (_data!['scoreDernierAudit'] as num?)?.toDouble() ?? 0.0;
    return AppCard(
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Row(
            children: [
              const Icon(Icons.verified_outlined, color: AppColors.primary, size: 22),
              const SizedBox(width: AppSpacing.s),
              Text('Dernier audit', style: Theme.of(context).textTheme.titleMedium),
            ],
          ),
          const SizedBox(height: AppSpacing.m),
          ConformiteGauge(tauxConformite: score),
          const SizedBox(height: AppSpacing.s),
          Text(
            '${_data!['totalMissionsRealisees'] ?? 0} mission(s) réalisée(s)',
            style: Theme.of(context).textTheme.bodySmall,
          ),
        ],
      ),
    );
  }

  Widget _buildActionsStats() {
    return AppCard(
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Text('Suivi des actions correctives', style: Theme.of(context).textTheme.titleMedium),
          const SizedBox(height: AppSpacing.m),
          Row(
            mainAxisAlignment: MainAxisAlignment.spaceAround,
            children: [
              _StatItem(label: 'À faire', value: '${_data!['totalActionsAFaire'] ?? 0}', color: AppColors.textSecondary),
              _StatItem(label: 'En cours', value: '${_data!['totalActionsEnCours'] ?? 0}', color: AppColors.accent),
              _StatItem(label: 'En retard', value: '${_data!['totalActionsEnRetard'] ?? 0}', color: AppColors.nonConforme),
              _StatItem(label: 'Clôturées', value: '${_data!['totalActionsCloturees'] ?? 0}', color: AppColors.conforme),
            ],
          ),
        ],
      ),
    );
  }

  Widget _buildActionsUrgentes() {
    final actions = _data!['actionsPrioritaires'] as List<dynamic>? ?? [];
    if (actions.isEmpty) return const SizedBox.shrink();
    return AppCard(
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Text('Actions prioritaires', style: Theme.of(context).textTheme.titleMedium),
          const SizedBox(height: AppSpacing.m),
          ...actions.map((a) {
            final action = a as Map<String, dynamic>;
            return Padding(
              padding: const EdgeInsets.only(bottom: AppSpacing.m),
              child: Row(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  _PriorityDot(priorite: action['priorite'] as String? ?? 'MOYEN'),
                  const SizedBox(width: AppSpacing.s),
                  Expanded(
                    child: Column(
                      crossAxisAlignment: CrossAxisAlignment.start,
                      children: [
                        Text(
                          action['description'] as String? ?? '-',
                          style: Theme.of(context).textTheme.bodyLarge,
                          maxLines: 2,
                          overflow: TextOverflow.ellipsis,
                        ),
                        const SizedBox(height: 2),
                        Text(
                          'Échéance : ${action['echeance'] ?? 'N/A'} • Resp : ${action['responsable'] ?? '-'}',
                          style: Theme.of(context).textTheme.bodySmall,
                        ),
                      ],
                    ),
                  ),
                  StatusBadge(status: action['statut'] as String? ?? ''),
                ],
              ),
            );
          }).toList(),
        ],
      ),
    );
  }
}

class _StatItem extends StatelessWidget {
  final String label;
  final String value;
  final Color color;

  const _StatItem({required this.label, required this.value, required this.color});

  @override
  Widget build(BuildContext context) {
    return Column(
      children: [
        Text(value, style: TextStyle(fontSize: 22, fontWeight: FontWeight.bold, color: color)),
        Text(label, style: Theme.of(context).textTheme.bodySmall, textAlign: TextAlign.center),
      ],
    );
  }
}

class _PriorityDot extends StatelessWidget {
  final String priorite;
  const _PriorityDot({required this.priorite});

  @override
  Widget build(BuildContext context) {
    final color = switch (priorite.toUpperCase()) {
      'CRITIQUE' => AppColors.prioriteCritique,
      'HAUTE' => AppColors.prioriteHaute,
      'MOYENNE' => AppColors.prioriteMoyenne,
      _ => AppColors.prioriteFaible,
    };
    return Container(
      width: 10,
      height: 10,
      margin: const EdgeInsets.only(top: 4),
      decoration: BoxDecoration(color: color, shape: BoxShape.circle),
    );
  }
}
