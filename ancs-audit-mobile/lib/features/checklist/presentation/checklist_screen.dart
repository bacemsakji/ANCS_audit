import 'package:flutter/material.dart';
import '../../../core/constants/app_colors.dart';
import '../../../core/constants/app_spacing.dart';
import '../../../core/widgets/app_card.dart';
import '../../../core/widgets/shimmer_card.dart';
import '../data/constat_repository.dart';
import '../data/referentiel_repository.dart';

class ChecklistScreen extends StatefulWidget {
  final String missionId;
  final String referentielId;
  final ConstatRepository constatRepository;
  final ReferentielRepository referentielRepository;
  final bool isOnline;

  const ChecklistScreen({
    Key? key,
    required this.missionId,
    required this.referentielId,
    required this.constatRepository,
    required this.referentielRepository,
    required this.isOnline,
  }) : super(key: key);

  @override
  State<ChecklistScreen> createState() => _ChecklistScreenState();
}

class _ChecklistScreenState extends State<ChecklistScreen> {
  List<dynamic> _controles = [];
  Map<String, Map<String, dynamic>> _constatsMap = {}; // Key: controleId, Value: constatData
  bool _isLoading = true;
  String? _error;

  @override
  void initState() {
    super.initState();
    _loadData();
  }

  Future<void> _loadData() async {
    setState(() {
      _isLoading = true;
      _error = null;
    });
    try {
      // 1. Charger les contrôles du référentiel
      final controlesData = await widget.referentielRepository.getControlesByReferentiel(widget.referentielId);
      
      // 2. Charger les constats existants pour la mission
      Map<String, Map<String, dynamic>> map = {};
      try {
        final constatsData = await widget.constatRepository.getConstatsByMission(widget.missionId);
        for (var c in constatsData) {
          final cMap = c as Map<String, dynamic>;
          map[cMap['controleId'].toString()] = cMap;
        }
      } catch (e) {
        // En cas d'échec offline, on ignore les constats existants du serveur (on s'appuie sur le cache local Drift en production)
        print("Erreur réseau lors de la récupération des constats du serveur: $e");
      }

      setState(() {
        _controles = controlesData;
        _constatsMap = map;
        _isLoading = false;
      });
    } catch (e) {
      setState(() {
        _error = e.toString();
        _isLoading = false;
      });
    }
  }

  Future<void> _saveConstat(String controleId, String resultat, String commentaire) async {
    final mockId = '${widget.missionId}_$controleId';
    
    // Mettre à jour l'UI locale
    setState(() {
      _constatsMap[controleId] = {
        'resultat': resultat,
        'commentaire': commentaire,
        'synced': widget.isOnline,
      };
    });

    try {
      await widget.constatRepository.submitConstat(
        id: mockId,
        missionId: widget.missionId,
        controleId: controleId,
        resultat: resultat,
        commentaire: commentaire,
        isOnline: widget.isOnline,
      );

      ScaffoldMessenger.of(context).showSnackBar(
        SnackBar(
          content: Text(widget.isOnline 
              ? 'Constat enregistré et synchronisé' 
              : 'Constat enregistré en local (mode hors-ligne)'),
          backgroundColor: AppColors.conforme,
          duration: const Duration(seconds: 1),
        ),
      );
    } catch (e) {
      ScaffoldMessenger.of(context).showSnackBar(
        const SnackBar(
          content: Text('Erreur d\'enregistrement'),
          backgroundColor: AppColors.nonConforme,
        ),
      );
    }
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      backgroundColor: AppColors.background,
      appBar: AppBar(
        title: const Text('Checklist d\'Audit'),
        actions: [
          IconButton(
            icon: const Icon(Icons.sync),
            onPressed: () async {
              if (widget.isOnline) {
                await widget.constatRepository.syncPendingConstats();
                _loadData();
              } else {
                ScaffoldMessenger.of(context).showSnackBar(
                  const SnackBar(
                    content: Text('Impossible de synchroniser en mode hors-ligne'),
                    backgroundColor: AppColors.observation,
                  ),
                );
              }
            },
          ),
        ],
      ),
      body: _isLoading
          ? const Padding(
              padding: EdgeInsets.all(AppSpacing.m),
              child: ShimmerList(count: 3),
            )
          : _error != null
              ? _buildError()
              : ListView.builder(
                  padding: const EdgeInsets.all(AppSpacing.m),
                  itemCount: _controles.length,
                  itemBuilder: (context, index) {
                    final ctrl = _controles[index] as Map<String, dynamic>;
                    final ctrlId = ctrl['id'].toString();
                    final constat = _constatsMap[ctrlId];

                    return _ChecklistItemCard(
                      controle: ctrl,
                      constat: constat,
                      onSave: (resultat, commentaire) => _saveConstat(ctrlId, resultat, commentaire),
                    );
                  },
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
          const Text('Impossible de charger le référentiel'),
          TextButton(onPressed: _loadData, child: const Text('Réessayer')),
        ],
      ),
    );
  }
}

class _ChecklistItemCard extends StatefulWidget {
  final Map<String, dynamic> controle;
  final Map<String, dynamic>? constat;
  final Function(String, String) onSave;

  const _ChecklistItemCard({
    Key? key,
    required this.controle,
    required this.constat,
    required this.onSave,
  }) : super(key: key);

  @override
  State<_ChecklistItemCard> createState() => _ChecklistItemCardState();
}

class _ChecklistItemCardState extends State<_ChecklistItemCard> {
  String? _selectedResultat;
  final _commentController = TextEditingController();

  @override
  void initState() {
    super.initState();
    _selectedResultat = widget.constat?['resultat'];
    _commentController.text = widget.constat?['commentaire'] ?? '';
  }

  @override
  void didUpdateWidget(covariant _ChecklistItemCard oldWidget) {
    super.didUpdateWidget(oldWidget);
    if (widget.constat != oldWidget.constat) {
      _selectedResultat = widget.constat?['resultat'];
      _commentController.text = widget.constat?['commentaire'] ?? '';
    }
  }

  @override
  void dispose() {
    _commentController.dispose();
    super.dispose();
  }

  @override
  Widget build(BuildContext context) {
    final c = widget.controle;
    return Padding(
      padding: const EdgeInsets.only(bottom: AppSpacing.m),
      child: AppCard(
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            Row(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                Container(
                  padding: const EdgeInsets.symmetric(horizontal: 6, vertical: 2),
                  decoration: BoxDecoration(
                    color: AppColors.primary.withOpacity(0.1),
                    borderRadius: BorderRadius.circular(4),
                  ),
                  child: Text(
                    c['categorie'] ?? 'Technique',
                    style: const TextStyle(fontSize: 10, color: AppColors.primary, fontWeight: FontWeight.bold),
                  ),
                ),
                const SizedBox(width: AppSpacing.s),
                Expanded(
                  child: Text(
                    c['libelle'] ?? '-',
                    style: Theme.of(context).textTheme.titleMedium,
                  ),
                ),
              ],
            ),
            if (c['description'] != null) ...[
              const SizedBox(height: AppSpacing.s),
              Text(
                c['description'],
                style: Theme.of(context).textTheme.bodySmall,
              ),
            ],
            const SizedBox(height: AppSpacing.m),
            
            // Sélecteurs de résultat
            Row(
              mainAxisAlignment: MainAxisAlignment.spaceBetween,
              children: [
                _buildResultButton('CONFORME', 'Conforme', AppColors.conforme),
                _buildResultButton('NON_CONFORME', 'Non Conforme', AppColors.nonConforme),
                _buildResultButton('OBSERVATION', 'Observation', AppColors.observation),
              ],
            ),
            const SizedBox(height: AppSpacing.m),

            // Commentaire libre
            TextField(
              controller: _commentController,
              decoration: const InputDecoration(
                labelText: 'Commentaire / Observation terrain',
                border: OutlineInputBorder(),
                contentPadding: EdgeInsets.symmetric(horizontal: 12, vertical: 8),
              ),
              maxLines: 2,
            ),
            const SizedBox(height: AppSpacing.m),

            Row(
              mainAxisAlignment: MainAxisAlignment.spaceBetween,
              children: [
                if (widget.constat?['synced'] == false)
                  const Row(
                    children: [
                      Icon(Icons.cloud_upload_outlined, color: AppColors.observation, size: 16),
                      SizedBox(width: 4),
                      Text('Non synchronisé', style: TextStyle(fontSize: 11, color: AppColors.observation)),
                    ],
                  )
                else
                  const SizedBox.shrink(),
                ElevatedButton.icon(
                  onPressed: _selectedResultat == null 
                      ? null 
                      : () => widget.onSave(_selectedResultat!, _commentController.text),
                  style: ElevatedButton.styleFrom(
                    backgroundColor: AppColors.primary,
                    foregroundColor: Colors.white,
                  ),
                  icon: const Icon(Icons.save, size: 16),
                  label: const Text('Enregistrer'),
                ),
              ],
            ),
          ],
        ),
      ),
    );
  }

  Widget _buildResultButton(String value, String label, Color activeColor) {
    final isSelected = _selectedResultat == value;
    return InkWell(
      onTap: () => setState(() => _selectedResultat = value),
      child: Container(
        padding: const EdgeInsets.symmetric(horizontal: AppSpacing.s, vertical: 8),
        decoration: BoxDecoration(
          color: isSelected ? activeColor.withOpacity(0.15) : AppColors.background,
          border: Border.all(
            color: isSelected ? activeColor : AppColors.divider,
            width: isSelected ? 2 : 1,
          ),
          borderRadius: BorderRadius.circular(6),
        ),
        child: Text(
          label,
          style: TextStyle(
            color: isSelected ? activeColor : AppColors.textSecondary,
            fontWeight: isSelected ? FontWeight.bold : FontWeight.normal,
            fontSize: 12,
          ),
        ),
      ),
    );
  }
}
