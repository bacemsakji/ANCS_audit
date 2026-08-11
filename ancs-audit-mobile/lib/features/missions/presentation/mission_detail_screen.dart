import 'package:flutter/material.dart';
import 'package:go_router/go_router.dart';
import '../../../core/constants/app_colors.dart';
import '../../../core/constants/app_spacing.dart';
import '../../../core/widgets/app_card.dart';
import '../../../core/widgets/status_badge.dart';
import '../../reunions/data/reunion_repository.dart';
import '../../reunions/presentation/reunion_list_screen.dart';
import '../../../core/network/dio_client.dart';

class MissionDetailScreen extends StatelessWidget {
  final Map<String, dynamic> mission;
  final String userRole;

  const MissionDetailScreen({
    Key? key,
    required this.mission,
    required this.userRole,
  }) : super(key: key);

  String _formatDate(String? rawDate) {
    if (rawDate == null || rawDate.isEmpty || rawDate == '-' || rawDate == 'Non planifié') {
      return 'Non planifié';
    }
    try {
      final dt = DateTime.parse(rawDate);
      return '${dt.day.toString().padLeft(2, '0')}/${dt.month.toString().padLeft(2, '0')}/${dt.year}';
    } catch (_) {
      return rawDate;
    }
  }

  @override
  Widget build(BuildContext context) {
    final String status = mission['statut'] ?? 'PLANIFIEE';
    final String organismeNom = mission['organismeNom'] ?? 'Organisme inconnu';
    final String referentielNom = mission['referentielNom'] ?? '-';
    final String perimetre = mission['perimetre'] ?? '-';
    final String dateDebut = _formatDate(mission['dateDebut']);
    final String dateFin = _formatDate(mission['dateFin']);
    final String auditeurNom = mission['auditeurNom'] ?? 'Non assigné';

    final textTheme = Theme.of(context).textTheme;

    return Scaffold(
      backgroundColor: AppColors.background,
      appBar: AppBar(
        title: const Text('Détails de la Mission'),
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

            // Consolidated Mission Information Card
            AppCard(
              child: Column(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  // Header section: Organisme
                  Text(
                    'Organisme',
                    style: textTheme.titleSmall?.copyWith(color: AppColors.textSecondary),
                  ),
                  const SizedBox(height: AppSpacing.xs),
                  Text(
                    organismeNom,
                    style: textTheme.titleLarge?.copyWith(
                      color: AppColors.primary,
                      fontWeight: FontWeight.bold,
                    ),
                  ),
                  const Divider(height: AppSpacing.l),

                  // Metadata section 1: Référentiel & Périmètre
                  Row(
                    crossAxisAlignment: CrossAxisAlignment.start,
                    children: [
                      Expanded(
                        child: _buildMetaField(
                          context,
                          label: 'Référentiel',
                          value: referentielNom,
                          icon: Icons.book_outlined,
                        ),
                      ),
                      const SizedBox(width: AppSpacing.m),
                      Expanded(
                        child: _buildMetaField(
                          context,
                          label: 'Périmètre',
                          value: perimetre,
                          icon: Icons.grid_view_outlined,
                        ),
                      ),
                    ],
                  ),
                  const Divider(height: AppSpacing.l),

                  // Metadata section 2: Dates
                  Row(
                    crossAxisAlignment: CrossAxisAlignment.start,
                    children: [
                      Expanded(
                        child: _buildMetaField(
                          context,
                          label: 'Date de début',
                          value: dateDebut,
                          icon: Icons.calendar_today_outlined,
                        ),
                      ),
                      const SizedBox(width: AppSpacing.m),
                      Expanded(
                        child: _buildMetaField(
                          context,
                          label: 'Date de fin',
                          value: dateFin,
                          icon: Icons.event_available_outlined,
                        ),
                      ),
                    ],
                  ),
                  const Divider(height: AppSpacing.l),

                  // Metadata section 3: Auditeur
                  Text(
                    'Auditeur assigné',
                    style: textTheme.titleSmall?.copyWith(color: AppColors.textSecondary),
                  ),
                  const SizedBox(height: AppSpacing.s),
                  Row(
                    children: [
                      const CircleAvatar(
                        radius: 18,
                        backgroundColor: AppColors.primary,
                        child: Icon(Icons.person, color: Colors.white, size: 20),
                      ),
                      const SizedBox(width: AppSpacing.m),
                      Expanded(
                        child: Text(
                          auditeurNom,
                          style: textTheme.bodyLarge?.copyWith(fontWeight: FontWeight.w600),
                        ),
                      ),
                    ],
                  ),
                ],
              ),
            ),
            const SizedBox(height: AppSpacing.xl),

            // Action Buttons Section
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

                // Rapport button
                if ((userRole == 'AUDITEUR' || userRole == 'ADMIN_ANCS') && status != 'PLANIFIEE')
                  Padding(
                    padding: const EdgeInsets.only(bottom: AppSpacing.s),
                    child: SizedBox(
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
                  ),

                // Réunions de travail button — All roles
                SizedBox(
                  width: double.infinity,
                  child: ElevatedButton.icon(
                    onPressed: () {
                      final missionId = mission['id']?.toString() ?? '';
                      if (missionId.isNotEmpty) {
                        Navigator.push(
                          context,
                          MaterialPageRoute(
                            builder: (_) => ReunionListScreen(
                              repository: ReunionRepository(dioClient: DioClient()),
                              missionId: missionId,
                            ),
                          ),
                        );
                      }
                    },
                    style: ElevatedButton.styleFrom(
                      backgroundColor: AppColors.neutralIcon,
                      foregroundColor: Colors.white,
                      padding: const EdgeInsets.symmetric(vertical: 14),
                    ),
                    icon: const Icon(Icons.group_outlined),
                    label: const Text('Réunions de travail'),
                  ),
                ),
              ],
            ),
          ],
        ),
      ),
    );
  }

  Widget _buildMetaField(
    BuildContext context, {
    required String label,
    required String value,
    required IconData icon,
  }) {
    final textTheme = Theme.of(context).textTheme;
    return Column(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: [
        Row(
          children: [
            Icon(icon, size: 14, color: AppColors.textSecondary),
            const SizedBox(width: 4),
            Text(
              label,
              style: textTheme.titleSmall?.copyWith(color: AppColors.textSecondary, fontSize: 12),
            ),
          ],
        ),
        const SizedBox(height: 4),
        Text(
          value,
          style: textTheme.bodyMedium?.copyWith(
            fontWeight: FontWeight.w600,
            color: AppColors.textPrimary,
          ),
        ),
      ],
    );
  }
}
