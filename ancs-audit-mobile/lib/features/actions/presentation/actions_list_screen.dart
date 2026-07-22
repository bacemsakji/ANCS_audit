import 'package:flutter/material.dart';
import '../../../core/constants/app_colors.dart';
import '../../../core/constants/app_spacing.dart';
import '../../../core/widgets/app_card.dart';
import '../../../core/widgets/status_badge.dart';
import '../../../core/widgets/shimmer_card.dart';
import '../data/action_repository.dart';

class ActionsListScreen extends StatefulWidget {
  final ActionRepository repository;
  final String?
      missionId; // Optionnel : si null, charge le dashboard RSSI général
  final String userRole;

  const ActionsListScreen({
    Key? key,
    required this.repository,
    this.missionId,
    required this.userRole,
  }) : super(key: key);

  @override
  State<ActionsListScreen> createState() => _ActionsListScreenState();
}

class _ActionsListScreenState extends State<ActionsListScreen> {
  List<dynamic> _actions = [];
  bool _isLoading = true;
  String? _error;

  @override
  void initState() {
    super.initState();
    _loadActions();
  }

  Future<void> _loadActions() async {
    setState(() {
      _isLoading = true;
      _error = null;
    });
    try {
      final List<dynamic> data;
      if (widget.missionId != null) {
        data = await widget.repository.getActionsForMission(widget.missionId!);
      } else {
        data = await widget.repository.getActiveActionsRssi();
      }
      setState(() {
        _actions = data;
        _isLoading = false;
      });
    } catch (e) {
      setState(() {
        _error = e.toString();
        _isLoading = false;
      });
    }
  }

  Future<void> _updateActionStatus(String id, String currentStatus) async {
    // Déterminer le prochain statut
    String nextStatus;
    if (currentStatus == 'A_FAIRE') {
      nextStatus = 'EN_COURS';
    } else if (currentStatus == 'EN_COURS') {
      nextStatus = 'CLOTUREE';
    } else {
      return; // Déjà clôturée ou en retard
    }

    try {
      await widget.repository.updateStatus(id, nextStatus);
      ScaffoldMessenger.of(context).showSnackBar(
        SnackBar(
          content: Text('Statut mis à jour : $nextStatus'),
          backgroundColor: AppColors.conforme,
        ),
      );
      _loadActions();
    } catch (e) {
      ScaffoldMessenger.of(context).showSnackBar(
        const SnackBar(
          content: Text('Échec de la mise à jour du statut'),
          backgroundColor: AppColors.nonConforme,
        ),
      );
    }
  }

  @override
  Widget build(BuildContext context) {
    final title = widget.missionId != null
        ? 'Actions correctives de la mission'
        : 'Mes Actions Correctives (RSSI)';

    return Scaffold(
      backgroundColor: AppColors.background,
      appBar: AppBar(
        title: Text(title),
        actions: [
          IconButton(icon: const Icon(Icons.refresh), onPressed: _loadActions),
        ],
      ),
      body: _isLoading
          ? const Padding(
              padding: EdgeInsets.all(AppSpacing.m),
              child: ShimmerList(count: 4),
            )
          : _error != null
              ? _buildError()
              : _actions.isEmpty
                  ? _buildEmpty()
                  : RefreshIndicator(
                      onRefresh: _loadActions,
                      child: ListView.builder(
                        padding: const EdgeInsets.all(AppSpacing.m),
                        itemCount: _actions.length,
                        itemBuilder: (context, index) {
                          final action =
                              _actions[index] as Map<String, dynamic>;
                          return _buildActionCard(action);
                        },
                      ),
                    ),
    );
  }

  Widget _buildActionCard(Map<String, dynamic> action) {
    final String id = action['id'].toString();
    final String statut = action['statut'] ?? 'A_FAIRE';
    final String priorite = action['priorite'] ?? 'MOYENNE';
    final bool canModify = widget.userRole == 'RSSI' && statut != 'CLOTUREE';

    return Padding(
      padding: const EdgeInsets.only(bottom: AppSpacing.m),
      child: AppCard(
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            Row(
              mainAxisAlignment: MainAxisAlignment.spaceBetween,
              children: [
                _PriorityBadge(priority: priorite),
                StatusBadge(status: statut),
              ],
            ),
            const SizedBox(height: AppSpacing.s),
            Text(
              action['description'] ?? '-',
              style: Theme.of(context)
                  .textTheme
                  .bodyLarge
                  ?.copyWith(fontWeight: FontWeight.w500),
            ),
            const Divider(height: AppSpacing.l),
            Row(
              mainAxisAlignment: MainAxisAlignment.spaceBetween,
              children: [
                Column(
                  crossAxisAlignment: CrossAxisAlignment.start,
                  children: [
                    Text(
                        'Responsable : ${action['responsable'] ?? 'Non assigné'}',
                        style: Theme.of(context).textTheme.bodySmall),
                    Text('Échéance : ${action['echeance'] ?? 'N/A'}',
                        style: Theme.of(context).textTheme.bodySmall),
                  ],
                ),
                if (canModify)
                  ElevatedButton(
                    onPressed: () => _updateActionStatus(id, statut),
                    style: ElevatedButton.styleFrom(
                      backgroundColor: AppColors.primaryLight,
                      foregroundColor: Colors.white,
                      padding: const EdgeInsets.symmetric(
                          horizontal: 12, vertical: 4),
                    ),
                    child: Text(statut == 'A_FAIRE' ? 'Commencer' : 'Clôturer'),
                  ),
              ],
            ),
          ],
        ),
      ),
    );
  }

  Widget _buildError() {
    return Center(
      child: Column(
        mainAxisAlignment: MainAxisAlignment.center,
        children: [
          const Icon(Icons.cloud_off, size: 48, color: AppColors.textSecondary),
          const SizedBox(height: AppSpacing.m),
          const Text('Erreur de récupération des actions'),
          TextButton(onPressed: _loadActions, child: const Text('Réessayer')),
        ],
      ),
    );
  }

  Widget _buildEmpty() {
    return Center(
      child: Column(
        mainAxisAlignment: MainAxisAlignment.center,
        children: [
          const Icon(Icons.check_circle_outline,
              size: 48, color: AppColors.conforme),
          const SizedBox(height: AppSpacing.m),
          Text(
            'Aucune action corrective planifiée',
            style: Theme.of(context).textTheme.titleMedium,
          ),
        ],
      ),
    );
  }
}

class _PriorityBadge extends StatelessWidget {
  final String priority;
  const _PriorityBadge({required this.priority});

  @override
  Widget build(BuildContext context) {
    Color color = AppColors.prioriteFaible;
    String label = 'Faible';

    switch (priority.toUpperCase()) {
      case 'CRITIQUE':
        color = AppColors.prioriteCritique;
        label = 'Critique';
        break;
      case 'HAUTE':
      case 'MOYENNE':
        color = AppColors.prioriteHaute;
        label = 'Haute';
        break;
      default:
        color = AppColors.prioriteFaible;
        label = 'Faible';
    }

    return Container(
      padding: const EdgeInsets.symmetric(horizontal: 8, vertical: 2),
      decoration: BoxDecoration(
        color: color.withValues(alpha: 0.1),
        borderRadius: BorderRadius.circular(4),
      ),
      child: Text(
        label,
        style:
            TextStyle(color: color, fontSize: 11, fontWeight: FontWeight.bold),
      ),
    );
  }
}
