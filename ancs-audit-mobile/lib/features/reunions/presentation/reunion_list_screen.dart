import 'package:flutter/material.dart';
import '../../../core/constants/app_colors.dart';
import '../../../core/constants/app_spacing.dart';
import '../../../core/widgets/app_card.dart';
import '../data/reunion_repository.dart';
import 'create_reunion_screen.dart';

class ReunionListScreen extends StatefulWidget {
  final ReunionRepository repository;
  final String missionId;

  const ReunionListScreen({Key? key, required this.repository, required this.missionId}) : super(key: key);

  @override
  State<ReunionListScreen> createState() => _ReunionListScreenState();
}

class _ReunionListScreenState extends State<ReunionListScreen> {
  List<dynamic> _reunions = [];
  bool _isLoading = true;
  String? _error;

  @override
  void initState() {
    super.initState();
    _loadReunions();
  }

  Future<void> _loadReunions() async {
    setState(() {
      _isLoading = true;
      _error = null;
    });
    try {
      final data = await widget.repository.getReunionsByMission(widget.missionId);
      setState(() {
        _reunions = data;
        _isLoading = false;
      });
    } catch (e) {
      setState(() {
        _error = e.toString();
        _isLoading = false;
      });
    }
  }

  Future<void> _deleteReunion(String id) async {
    final confirm = await showDialog<bool>(
      context: context,
      builder: (ctx) => AlertDialog(
        title: const Text('Confirmer la suppression'),
        content: const Text('Voulez-vous vraiment supprimer cette réunion ?'),
        actions: [
          TextButton(onPressed: () => Navigator.pop(ctx, false), child: const Text('Annuler')),
          TextButton(
            onPressed: () => Navigator.pop(ctx, true), 
            child: const Text('Supprimer', style: TextStyle(color: AppColors.nonConforme)),
          ),
        ],
      ),
    );

    if (confirm != true) return;

    try {
      await widget.repository.deleteReunion(id);
      ScaffoldMessenger.of(context).showSnackBar(
        const SnackBar(content: Text('Réunion supprimée.'), backgroundColor: AppColors.conforme),
      );
      _loadReunions();
    } catch (e) {
      ScaffoldMessenger.of(context).showSnackBar(
        SnackBar(content: Text('Erreur: $e'), backgroundColor: AppColors.nonConforme),
      );
    }
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      backgroundColor: AppColors.background,
      appBar: AppBar(
        title: const Text('Réunions de travail'),
        backgroundColor: AppColors.primary,
        foregroundColor: Colors.white,
        elevation: 0,
      ),
      body: _isLoading
          ? const Center(child: CircularProgressIndicator())
          : _error != null
              ? _buildError()
              : _reunions.isEmpty
                  ? _buildEmpty()
                  : RefreshIndicator(
                      onRefresh: _loadReunions,
                      child: ListView.builder(
                        padding: const EdgeInsets.all(AppSpacing.m),
                        itemCount: _reunions.length,
                        itemBuilder: (context, index) {
                          final r = _reunions[index];
                          return _buildReunionCard(r);
                        },
                      ),
                    ),
      floatingActionButton: FloatingActionButton(
        onPressed: () {
          Navigator.push(
            context,
            MaterialPageRoute(
              builder: (_) => CreateReunionScreen(
                repository: widget.repository,
                missionId: widget.missionId,
              ),
            ),
          ).then((value) {
            if (value == true) _loadReunions();
          });
        },
        backgroundColor: AppColors.primary,
        child: const Icon(Icons.add, color: Colors.white),
      ),
    );
  }

  Widget _buildError() => Center(
        child: Column(
          mainAxisAlignment: MainAxisAlignment.center,
          children: [
            const Icon(Icons.cloud_off, size: 48, color: AppColors.textSecondary),
            const SizedBox(height: AppSpacing.m),
            Text('Erreur: $_error', textAlign: TextAlign.center),
            TextButton(onPressed: _loadReunions, child: const Text('Réessayer')),
          ],
        ),
      );

  Widget _buildEmpty() => const Center(
        child: Text('Aucune réunion de travail trouvée.'),
      );

  Widget _buildReunionCard(Map<String, dynamic> reunion) {
    final id = reunion['id']?.toString() ?? '';
    final dateStr = reunion['dateReunion']?.toString() ?? '';
    final participants = reunion['participants']?.toString() ?? 'Non renseigné';
    final compteRendu = reunion['compteRendu']?.toString() ?? 'Aucun compte rendu';
    
    DateTime? d;
    try {
      if (dateStr.isNotEmpty) d = DateTime.parse(dateStr);
    } catch (_) {}

    final formattedDate = d != null 
        ? '${d.day.toString().padLeft(2, '0')}/${d.month.toString().padLeft(2, '0')}/${d.year} à ${d.hour.toString().padLeft(2, '0')}:${d.minute.toString().padLeft(2, '0')}'
        : dateStr;

    return Padding(
      padding: const EdgeInsets.only(bottom: AppSpacing.m),
      child: AppCard(
        padding: const EdgeInsets.all(AppSpacing.m),
        child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Row(
            mainAxisAlignment: MainAxisAlignment.spaceBetween,
            children: [
              Expanded(
                child: Text(
                  formattedDate,
                  style: Theme.of(context).textTheme.titleMedium?.copyWith(color: AppColors.primary),
                ),
              ),
              IconButton(
                icon: const Icon(Icons.delete_outline, color: AppColors.nonConforme),
                onPressed: () => _deleteReunion(id),
                padding: EdgeInsets.zero,
                constraints: const BoxConstraints(),
              ),
            ],
          ),
          const Divider(height: AppSpacing.l),
          _buildInfoRow(Icons.people_outline, 'Participants:', participants),
          const SizedBox(height: AppSpacing.s),
          _buildInfoRow(Icons.notes, 'Compte rendu:', compteRendu),
        ],
      ),
    ),
  );
}

  Widget _buildInfoRow(IconData icon, String label, String text) {
    return Row(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: [
        Icon(icon, size: 16, color: AppColors.textSecondary),
        const SizedBox(width: 8),
        Expanded(
          child: Column(
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              Text(label, style: const TextStyle(fontSize: 12, fontWeight: FontWeight.w600, color: AppColors.textSecondary)),
              Text(text, style: const TextStyle(fontSize: 13, color: AppColors.textPrimary)),
            ],
          ),
        ),
      ],
    );
  }
}
