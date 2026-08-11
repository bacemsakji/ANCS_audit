import 'package:flutter/material.dart';
import 'package:go_router/go_router.dart';
import 'package:url_launcher/url_launcher.dart';
import '../../../core/constants/app_colors.dart';
import '../../../core/constants/app_spacing.dart';
import '../../../core/widgets/app_card.dart';
import '../../../core/widgets/app_buttons.dart';
import '../../../core/widgets/shimmer_card.dart';
import '../data/rapport_repository.dart';

/// Écran de consultation des rapports d'une mission.
///
/// • Si [missionId] est fourni → charge les rapports de la mission (AUDITEUR / ADMIN).
/// • Si [missionId] est null et [isOrganismeView] est true → charge les rapports
///   de l'organisme du RSSI connecté.
class RapportViewScreen extends StatefulWidget {
  final String? missionId;
  final String organismeNom;
  final String userRole;
  final bool isOrganismeView; // true = RSSI (pas de missionId)
  final RapportRepository repository;

  const RapportViewScreen({
    Key? key,
    this.missionId,
    required this.organismeNom,
    required this.userRole,
    this.isOrganismeView = false,
    required this.repository,
  }) : super(key: key);

  @override
  State<RapportViewScreen> createState() => _RapportViewScreenState();
}

class _RapportViewScreenState extends State<RapportViewScreen> {
  List<Map<String, dynamic>> _rapports = [];
  bool _isLoading = true;
  String? _error;
  // Track which rapport is currently downloading (full report)
  String? _downloadingId;
  // Track which rapport résumé is currently downloading
  String? _downloadingResumeId;

  @override
  void initState() {
    super.initState();
    _load();
  }

  Future<void> _load() async {
    setState(() {
      _isLoading = true;
      _error = null;
    });
    try {
      final data = widget.isOrganismeView
          ? await widget.repository.getRapportsByOrganisme()
          : await widget.repository.getRapportsByMission(widget.missionId!);
      setState(() {
        _rapports = data;
        _isLoading = false;
      });
    } catch (e) {
      setState(() {
        _error = e.toString();
        _isLoading = false;
      });
    }
  }

  Future<void> _download(String rapportId) async {
    setState(() => _downloadingId = rapportId);
    try {
      final url = await widget.repository.getDownloadUrl(rapportId);
      final uri = Uri.parse(url);
      if (await canLaunchUrl(uri)) {
        await launchUrl(uri, mode: LaunchMode.externalApplication);
      } else {
        _showSnack('Impossible d\'ouvrir le lien de téléchargement.', isError: true);
      }
    } catch (e) {
      _showSnack('Erreur : $e', isError: true);
    } finally {
      if (mounted) setState(() => _downloadingId = null);
    }
  }

  Future<void> _downloadResume(String rapportId) async {
    setState(() => _downloadingResumeId = rapportId);
    try {
      final url = await widget.repository.getResumeDownloadUrl(rapportId);
      final uri = Uri.parse(url);
      if (await canLaunchUrl(uri)) {
        await launchUrl(uri, mode: LaunchMode.externalApplication);
      } else {
        _showSnack('Impossible d\'ouvrir le lien de téléchargement du résumé.', isError: true);
      }
    } catch (e) {
      _showSnack('Erreur : $e', isError: true);
    } finally {
      if (mounted) setState(() => _downloadingResumeId = null);
    }
  }

  Future<void> _soumettre(String rapportId) async {
    try {
      await widget.repository.soumettre(rapportId);
      _showSnack('Rapport soumis avec succès à l\'ANCS.');
      _load();
    } catch (e) {
      _showSnack('Erreur lors de la soumission : $e', isError: true);
    }
  }

  Future<void> _accepter(String rapportId) async {
    try {
      await widget.repository.accepter(rapportId);
      _showSnack('Rapport accepté avec succès.');
      _load();
    } catch (e) {
      _showSnack('Erreur lors de l\'acceptation : $e', isError: true);
    }
  }

  Future<void> _rejeter(String rapportId) async {
    final motifController = TextEditingController();
    final motif = await showDialog<String>(
      context: context,
      builder: (ctx) => AlertDialog(
        title: const Text('Rejeter le rapport'),
        content: TextField(
          controller: motifController,
          decoration: const InputDecoration(hintText: 'Motif du rejet (obligatoire)'),
          maxLines: 3,
        ),
        actions: [
          TextButton(onPressed: () => Navigator.pop(ctx), child: const Text('Annuler')),
          TextButton(
            onPressed: () => Navigator.pop(ctx, motifController.text.trim()),
            child: const Text('Rejeter', style: TextStyle(color: AppColors.nonConforme)),
          ),
        ],
      ),
    );

    if (motif == null || motif.isEmpty) return;

    try {
      await widget.repository.rejeter(rapportId, motif);
      _showSnack('Rapport rejeté avec succès.');
      _load();
    } catch (e) {
      _showSnack('Erreur lors du rejet : $e', isError: true);
    }
  }

  void _showSnack(String msg, {bool isError = false}) {
    ScaffoldMessenger.of(context).showSnackBar(SnackBar(
      content: Text(msg),
      backgroundColor: isError ? AppColors.nonConforme : AppColors.conforme,
    ));
  }

  /// Navigate to the generation/modification screen for this mission.
  void _openGenerationScreen({Map<String, dynamic>? mission}) {
    if (widget.missionId == null) return;
    context.push('/missions/${widget.missionId}/rapport/nouveau', extra: {
      'organismeNom': widget.organismeNom,
      'missionId': widget.missionId,
    });
  }

  @override
  Widget build(BuildContext context) {
    final canEdit = widget.userRole == 'AUDITEUR' || widget.userRole == 'ADMIN_ANCS';

    return Scaffold(
      backgroundColor: AppColors.background,
      appBar: AppBar(
        leading: IconButton(
          icon: const Icon(Icons.arrow_back),
          onPressed: () {
            if (context.canPop()) {
              context.pop();
            } else {
              context.go('/dashboard');
            }
          },
        ),
        backgroundColor: AppColors.primary,
        foregroundColor: Colors.white,
        title: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            Text(
              widget.isOrganismeView ? 'Rapports d\'audit' : 'Rapports — ${widget.organismeNom}',
              style: const TextStyle(fontSize: 15, fontWeight: FontWeight.bold),
            ),
            if (!widget.isOrganismeView)
              Text(
                widget.organismeNom,
                style: TextStyle(fontSize: 11, color: Colors.white.withValues(alpha: 0.75)),
              ),
          ],
        ),
        actions: [
          IconButton(
            icon: const Icon(Icons.refresh),
            tooltip: 'Actualiser',
            onPressed: _load,
          ),
        ],
      ),
      // FAB: only AUDITEUR and ADMIN can create a new version
      floatingActionButton: canEdit && widget.missionId != null
          ? FloatingActionButton.extended(
              onPressed: _openGenerationScreen,
              backgroundColor: AppColors.accent,
              foregroundColor: Colors.white,
              icon: const Icon(Icons.add),
              label: const Text('Nouvelle version'),
            )
          : null,
      body: _isLoading
          ? const Padding(
              padding: EdgeInsets.all(AppSpacing.m),
              child: ShimmerList(count: 3),
            )
          : _error != null
              ? _buildError()
              : _rapports.isEmpty
                  ? _buildEmpty(canEdit)
                  : RefreshIndicator(
                      onRefresh: _load,
                      child: ListView.builder(
                        padding: const EdgeInsets.fromLTRB(
                          AppSpacing.m,
                          AppSpacing.m,
                          AppSpacing.m,
                          AppSpacing.m + 80, // space for FAB
                        ),
                        itemCount: _rapports.length,
                        itemBuilder: (ctx, i) => _buildRapportCard(_rapports[i], canEdit),
                      ),
                    ),
    );
  }

  Widget _buildRapportCard(Map<String, dynamic> rapport, bool canEdit) {
    final String id = rapport['id']?.toString() ?? '';
    final int version = (rapport['version'] as num?)?.toInt() ?? 1;
    final String type = rapport['type'] ?? 'PDF';
    final String statut = rapport['statutSoumissionAncs'] ?? 'NON_SOUMIS';
    final bool iaUsed = rapport['syntheseGenereeParIa'] == true;
    final bool resumeDisponible = rapport['resumeDisponible'] == true;
    final String? auditeur = rapport['auditeurNom'];
    final String? organisme = rapport['organismeNom'];
    final String? dateStr = rapport['dateGeneration']?.toString();
    final String? certif = rapport['numeroCertificationAncs'];
    final String? motifRejet = rapport['motifRejet'];

    final formattedDate = _formatDate(dateStr);

    return Padding(
      padding: const EdgeInsets.only(bottom: AppSpacing.m),
      child: AppCard(
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            // --- Header row ---
            Row(
              children: [
                _typeIcon(type),
                const SizedBox(width: AppSpacing.s),
                Expanded(
                  child: Column(
                    crossAxisAlignment: CrossAxisAlignment.start,
                    children: [
                      Text(
                        'Version $version — $type',
                        style: Theme.of(context).textTheme.titleMedium?.copyWith(
                              color: AppColors.primary,
                              fontWeight: FontWeight.bold,
                            ),
                      ),
                      Text(
                        formattedDate,
                        style: Theme.of(context).textTheme.bodySmall,
                      ),
                    ],
                  ),
                ),
                _statutBadge(statut),
              ],
            ),

            const Divider(height: AppSpacing.l),

            // --- Details ---
            if (organisme != null && widget.isOrganismeView) ...[
              _infoRow(Icons.business_outlined, 'Organisme', organisme),
              const SizedBox(height: 4),
            ],
            if (auditeur != null) ...[
              _infoRow(Icons.verified_user_outlined, 'Auditeur', auditeur +
                  (certif != null ? '  •  N° $certif' : '')),
              const SizedBox(height: 4),
            ],
            if (iaUsed) ...[
              _infoRow(Icons.auto_awesome_outlined, 'Synthèse', 'Brouillon généré par IA',
                  color: AppColors.accent),
              const SizedBox(height: 4),
            ],
            if (statut == 'REJETE' && motifRejet != null) ...[
              const SizedBox(height: AppSpacing.s),
              Container(
                width: double.infinity,
                padding: const EdgeInsets.all(AppSpacing.s),
                decoration: BoxDecoration(
                  color: AppColors.nonConformeBg,
                  borderRadius: BorderRadius.circular(6),
                  border: Border.all(color: AppColors.nonConforme.withValues(alpha: 0.3)),
                ),
                child: Row(
                  crossAxisAlignment: CrossAxisAlignment.start,
                  children: [
                    const Icon(Icons.error_outline, color: AppColors.nonConforme, size: 16),
                    const SizedBox(width: 6),
                    Expanded(
                      child: Text(
                        'Motif de rejet : $motifRejet',
                        style: const TextStyle(
                          color: AppColors.nonConforme,
                          fontSize: 12,
                          fontWeight: FontWeight.w500,
                        ),
                      ),
                    ),
                  ],
                ),
              ),
            ],

            const SizedBox(height: AppSpacing.m),

            // --- Action buttons ---
            Row(
              children: [
                // Download button
                Expanded(
                  child: _downloadingId == id
                      ? const Center(
                          child: SizedBox(
                            height: 24,
                            width: 24,
                            child: CircularProgressIndicator(
                                strokeWidth: 2, color: AppColors.primary),
                          ),
                        )
                      : OutlinedButton.icon(
                          onPressed: id.isNotEmpty ? () => _download(id) : null,
                          style: OutlinedButton.styleFrom(
                            foregroundColor: AppColors.primary,
                            side: const BorderSide(color: AppColors.primary, width: 1.5),
                            shape: RoundedRectangleBorder(
                                borderRadius: BorderRadius.circular(8)),
                          ),
                          icon: const Icon(Icons.download_for_offline_outlined, size: 18),
                          label: const Text('Télécharger'),
                        ),
                ),
                // Modifier button — only for AUDITEUR and ADMIN, only if missionId known and not SOUMIS/ACCEPTE
                if (canEdit && widget.missionId != null && (statut == 'NON_SOUMIS' || statut == 'REJETE')) ...[
                  const SizedBox(width: AppSpacing.s),
                  Expanded(
                    child: ElevatedButton.icon(
                      onPressed: _openGenerationScreen,
                      style: ElevatedButton.styleFrom(
                        backgroundColor: AppColors.accent,
                        foregroundColor: Colors.white,
                        shape: RoundedRectangleBorder(
                            borderRadius: BorderRadius.circular(8)),
                      ),
                      icon: const Icon(Icons.edit_outlined, size: 18),
                      label: const Text('Modifier'),
                    ),
                  ),
                ],
              ],
            ),
            // Résumé download row — only shown when the summary document is available
            if (resumeDisponible) ...[
              const SizedBox(height: AppSpacing.s),
              SizedBox(
                width: double.infinity,
                child: _downloadingResumeId == id
                    ? const Center(
                        child: SizedBox(
                          height: 24,
                          width: 24,
                          child: CircularProgressIndicator(
                              strokeWidth: 2, color: AppColors.primary),
                        ),
                      )
                    : OutlinedButton.icon(
                        onPressed: id.isNotEmpty ? () => _downloadResume(id) : null,
                        style: OutlinedButton.styleFrom(
                          foregroundColor: AppColors.textSecondary,
                          side: const BorderSide(color: AppColors.textSecondary, width: 1),
                          shape: RoundedRectangleBorder(
                              borderRadius: BorderRadius.circular(8)),
                        ),
                        icon: const Icon(Icons.summarize_outlined, size: 18),
                        label: const Text('Télécharger le résumé'),
                      ),
              ),
            ],
            const SizedBox(height: AppSpacing.s),
            if (widget.userRole == 'AUDITEUR' && (statut == 'NON_SOUMIS' || statut == 'REJETE')) ...[
              SizedBox(
                width: double.infinity,
                child: ElevatedButton.icon(
                  onPressed: () => _soumettre(id),
                  style: ElevatedButton.styleFrom(
                    backgroundColor: AppColors.primary,
                    foregroundColor: Colors.white,
                    shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(8)),
                  ),
                  icon: const Icon(Icons.send),
                  label: const Text('Soumettre à l\'ANCS'),
                ),
              ),
            ],
            if (widget.userRole == 'ADMIN_ANCS' && statut == 'SOUMIS') ...[
              Row(
                children: [
                  Expanded(
                    child: ElevatedButton.icon(
                      onPressed: () => _accepter(id),
                      style: ElevatedButton.styleFrom(
                        backgroundColor: AppColors.conforme,
                        foregroundColor: Colors.white,
                        shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(8)),
                      ),
                      icon: const Icon(Icons.check),
                      label: const Text('Accepter'),
                    ),
                  ),
                  const SizedBox(width: AppSpacing.s),
                  Expanded(
                    child: ElevatedButton.icon(
                      onPressed: () => _rejeter(id),
                      style: ElevatedButton.styleFrom(
                        backgroundColor: AppColors.nonConforme,
                        foregroundColor: Colors.white,
                        shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(8)),
                      ),
                      icon: const Icon(Icons.close),
                      label: const Text('Rejeter'),
                    ),
                  ),
                ],
              ),
            ],
          ],
        ),
      ),
    );
  }

  // ─── Helpers ────────────────────────────────────────────────────────────────

  Widget _typeIcon(String type) {
    final isDocx = type.toUpperCase() == 'DOCX';
    return Container(
      width: 42,
      height: 42,
      decoration: BoxDecoration(
        color: isDocx
            ? const Color(0xFF1565C0).withValues(alpha: 0.1)
            : AppColors.nonConformeBg,
        borderRadius: BorderRadius.circular(8),
      ),
      child: Icon(
        isDocx ? Icons.description_outlined : Icons.picture_as_pdf_outlined,
        color: isDocx ? const Color(0xFF1565C0) : AppColors.nonConforme,
        size: 22,
      ),
    );
  }

  Widget _statutBadge(String statut) {
    final (color, label) = switch (statut) {
      'SOUMIS' => (AppColors.accent, 'Soumis'),
      'ACCEPTE' => (AppColors.conforme, 'Accepté'),
      'REJETE' => (AppColors.nonConforme, 'Rejeté'),
      _ => (AppColors.textSecondary, 'Non soumis'),
    };
    return Container(
      padding: const EdgeInsets.symmetric(horizontal: 10, vertical: 4),
      decoration: BoxDecoration(
        color: color.withValues(alpha: 0.1),
        borderRadius: BorderRadius.circular(20),
        border: Border.all(color: color.withValues(alpha: 0.4)),
      ),
      child: Text(
        label,
        style: TextStyle(color: color, fontSize: 11, fontWeight: FontWeight.bold),
      ),
    );
  }

  Widget _infoRow(IconData icon, String label, String value, {Color? color}) {
    return Row(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: [
        Icon(icon, size: 14, color: color ?? AppColors.textSecondary),
        const SizedBox(width: 6),
        Text(
          '$label : ',
          style: TextStyle(
            fontSize: 12,
            color: color ?? AppColors.textSecondary,
            fontWeight: FontWeight.w600,
          ),
        ),
        Expanded(
          child: Text(
            value,
            style: TextStyle(fontSize: 12, color: color ?? AppColors.textSecondary),
            overflow: TextOverflow.ellipsis,
          ),
        ),
      ],
    );
  }

  Widget _buildEmpty(bool canEdit) {
    return Center(
      child: Padding(
        padding: const EdgeInsets.all(AppSpacing.xl),
        child: Column(
          mainAxisAlignment: MainAxisAlignment.center,
          children: [
            Container(
              width: 80,
              height: 80,
              decoration: BoxDecoration(
                color: AppColors.primary.withValues(alpha: 0.08),
                shape: BoxShape.circle,
              ),
              child: const Icon(Icons.folder_open_outlined,
                  size: 40, color: AppColors.primary),
            ),
            const SizedBox(height: AppSpacing.m),
            Text(
              'Aucun rapport généré',
              style: Theme.of(context)
                  .textTheme
                  .titleMedium
                  ?.copyWith(color: AppColors.textPrimary),
              textAlign: TextAlign.center,
            ),
            const SizedBox(height: AppSpacing.s),
            Text(
              canEdit
                  ? 'L\'auditeur doit d\'abord compléter la checklist puis compiler le rapport.'
                  : 'Le rapport d\'audit n\'a pas encore été généré par l\'auditeur.',
              style: Theme.of(context).textTheme.bodySmall,
              textAlign: TextAlign.center,
            ),
            if (canEdit && widget.missionId != null) ...[
              const SizedBox(height: AppSpacing.l),
              PrimaryButton(
                label: 'Générer le premier rapport',
                icon: Icons.document_scanner_outlined,
                onPressed: _openGenerationScreen,
              ),
            ],
          ],
        ),
      ),
    );
  }

  Widget _buildError() {
    return Center(
      child: Padding(
        padding: const EdgeInsets.all(AppSpacing.xl),
        child: Column(
          mainAxisAlignment: MainAxisAlignment.center,
          children: [
            const Icon(Icons.cloud_off, size: 48, color: AppColors.textSecondary),
            const SizedBox(height: AppSpacing.m),
            Text(
              'Impossible de charger les rapports',
              style: Theme.of(context).textTheme.titleMedium,
              textAlign: TextAlign.center,
            ),
            const SizedBox(height: AppSpacing.s),
            Text(
              _error ?? '',
              style: Theme.of(context)
                  .textTheme
                  .bodySmall
                  ?.copyWith(color: AppColors.nonConforme),
              textAlign: TextAlign.center,
              maxLines: 3,
              overflow: TextOverflow.ellipsis,
            ),
            const SizedBox(height: AppSpacing.m),
            SecondaryButton(
              label: 'Réessayer',
              icon: Icons.refresh,
              onPressed: _load,
            ),
          ],
        ),
      ),
    );
  }

  String _formatDate(String? iso) {
    if (iso == null) return 'Date inconnue';
    try {
      final dt = DateTime.parse(iso).toLocal();
      return '${dt.day.toString().padLeft(2, '0')}/'
          '${dt.month.toString().padLeft(2, '0')}/'
          '${dt.year}  à  '
          '${dt.hour.toString().padLeft(2, '0')}:${dt.minute.toString().padLeft(2, '0')}';
    } catch (_) {
      return iso;
    }
  }
}
