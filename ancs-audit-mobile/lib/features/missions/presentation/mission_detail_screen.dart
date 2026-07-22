import 'package:flutter/material.dart';
import 'package:go_router/go_router.dart';
import '../../../core/constants/app_colors.dart';
import '../../../core/constants/app_spacing.dart';
import '../../../core/widgets/app_card.dart';
import '../../../core/widgets/status_badge.dart';

class MissionDetailScreen extends StatelessWidget {
  final Map<String, dynamic> mission;
  final String userRole;

  const MissionDetailScreen({
    Key? key,
    required this.mission,
    required this.userRole,
  }) : super(key: key);

  @override
  Widget build(BuildContext context) {
    final String status = mission['statut'] ?? 'PLANIFIEE';
    final String organismeNom = mission['organismeNom'] ?? 'Organisme inconnu';
    final String referentielNom = mission['referentielNom'] ?? '-';
    final String perimetre = mission['perimetre'] ?? '-';
    final String dateDebut = mission['dateDebut'] ?? 'Non planifié';
    final String dateFin = mission['dateFin'] ?? '-';
    final String auditeurNom = mission['auditeurNom'] ?? 'Non assigné';

    return Scaffold(
      backgroundColor: AppColors.background,
      appBar: AppBar(
        title: const Text('Détails de la Mission'),
        backgroundColor: AppColors.primary,
        foregroundColor: Colors.white,
      ),
      body: SingleChildScrollView(
        padding: const EdgeInsets.all(AppSpacing.m),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            // Status Badge
            Center(
              child: StatusBadge(status: status),
            ),
            const SizedBox(height: AppSpacing.l),

            // Organisme Card
            AppCard(
              child: Column(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  Text(
                    'Organisme',
                    style: Theme.of(context).textTheme.titleSmall?.copyWith(
                          color: AppColors.textSecondary,
                        ),
                  ),
                  const SizedBox(height: AppSpacing.s),
                  Text(
                    organismeNom,
                    style: Theme.of(context).textTheme.titleLarge?.copyWith(
                          color: AppColors.primary,
                          fontWeight: FontWeight.bold,
                        ),
                  ),
                ],
              ),
            ),
            const SizedBox(height: AppSpacing.m),

            // Référentiel Card
            AppCard(
              child: Column(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  Text(
                    'Référentiel',
                    style: Theme.of(context).textTheme.titleSmall?.copyWith(
                          color: AppColors.textSecondary,
                        ),
                  ),
                  const SizedBox(height: AppSpacing.s),
                  Text(
                    referentielNom,
                    style: Theme.of(context).textTheme.bodyLarge,
                  ),
                ],
              ),
            ),
            const SizedBox(height: AppSpacing.m),

            // Périmètre Card
            AppCard(
              child: Column(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  Text(
                    'Périmètre',
                    style: Theme.of(context).textTheme.titleSmall?.copyWith(
                          color: AppColors.textSecondary,
                        ),
                  ),
                  const SizedBox(height: AppSpacing.s),
                  Text(
                    perimetre,
                    style: Theme.of(context).textTheme.bodyLarge,
                  ),
                ],
              ),
            ),
            const SizedBox(height: AppSpacing.m),

            // Dates Card
            AppCard(
              child: Column(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  Text(
                    'Dates',
                    style: Theme.of(context).textTheme.titleSmall?.copyWith(
                          color: AppColors.textSecondary,
                        ),
                  ),
                  const SizedBox(height: AppSpacing.s),
                  Row(
                    mainAxisAlignment: MainAxisAlignment.spaceBetween,
                    children: [
                      Column(
                        crossAxisAlignment: CrossAxisAlignment.start,
                        children: [
                          const Text(
                            'Début',
                            style: TextStyle(fontWeight: FontWeight.bold),
                          ),
                          Text(dateDebut),
                        ],
                      ),
                      Column(
                        crossAxisAlignment: CrossAxisAlignment.end,
                        children: [
                          const Text(
                            'Fin',
                            style: TextStyle(fontWeight: FontWeight.bold),
                          ),
                          Text(dateFin),
                        ],
                      ),
                    ],
                  ),
                ],
              ),
            ),
            const SizedBox(height: AppSpacing.m),

            // Auditeur Card
            AppCard(
              child: Column(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  Text(
                    'Auditeur',
                    style: Theme.of(context).textTheme.titleSmall?.copyWith(
                          color: AppColors.textSecondary,
                        ),
                  ),
                  const SizedBox(height: AppSpacing.s),
                  Row(
                    children: [
                      const CircleAvatar(
                        backgroundColor: AppColors.primary,
                        child: Icon(Icons.person, color: Colors.white),
                      ),
                      const SizedBox(width: AppSpacing.m),
                      Text(
                        auditeurNom,
                        style: Theme.of(context).textTheme.bodyLarge,
                      ),
                    ],
                  ),
                ],
              ),
            ),
            const SizedBox(height: AppSpacing.xl),

            // Action Buttons
            Column(
              children: [
                // Checklist button — only for auditors on active missions
                if (userRole == 'AUDITEUR' && status != 'TERMINEE')
                  Padding(
                    padding: const EdgeInsets.only(bottom: AppSpacing.s),
                    child: SizedBox(
                      width: double.infinity,
                      child: ElevatedButton.icon(
                        onPressed: () {
                          final missionId = mission['id']?.toString() ?? '';
                          final referentielId = mission['referentielId']?.toString() ?? '';
                          if (missionId.isNotEmpty && referentielId.isNotEmpty) {
                            context.push('/missions/$missionId/checklist', extra: mission);
                          }
                        },
                        style: ElevatedButton.styleFrom(
                          backgroundColor: AppColors.primary,
                          foregroundColor: Colors.white,
                          padding: const EdgeInsets.symmetric(vertical: 14),
                        ),
                        icon: const Icon(Icons.playlist_add_check),
                        label: const Text('Commencer l\'audit'),
                      ),
                    ),
                  ),

                // Rapport button — Auditeur: view + modify; Admin: view + modify; RSSI: not shown here
                if ((userRole == 'AUDITEUR' || userRole == 'ADMIN_ANCS') && status != 'PLANIFIEE')
                  SizedBox(
                    width: double.infinity,
                    child: ElevatedButton.icon(
                      onPressed: () {
                        final missionId = mission['id']?.toString() ?? '';
                        if (missionId.isNotEmpty) {
                          context.push('/missions/$missionId/rapport', extra: mission);
                        }
                      },
                      style: ElevatedButton.styleFrom(
                        backgroundColor: AppColors.accent,
                        foregroundColor: Colors.white,
                        padding: const EdgeInsets.symmetric(vertical: 14),
                      ),
                      icon: const Icon(Icons.description_outlined),
                      label: Text(
                        userRole == 'ADMIN_ANCS' ? 'Voir / Gérer le rapport' : 'Rapport d\'audit',
                      ),
                    ),
                  ),
              ],
            ),
          ],
        ),
      ),
    );
  }
}
