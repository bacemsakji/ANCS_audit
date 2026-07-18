import 'package:flutter/material.dart';
import 'package:url_launcher/url_launcher.dart';
import '../../../core/constants/app_colors.dart';
import '../../../core/constants/app_spacing.dart';
import '../../../core/widgets/app_card.dart';
import '../../../core/widgets/app_buttons.dart';
import '../data/rapport_repository.dart';

class RapportGenerationScreen extends StatefulWidget {
  final String missionId;
  final String organismeNom;
  final RapportRepository repository;

  const RapportGenerationScreen({
    Key? key,
    required this.missionId,
    required this.organismeNom,
    required this.repository,
  }) : super(key: key);

  @override
  State<RapportGenerationScreen> createState() => _RapportGenerationScreenState();
}

class _RapportGenerationScreenState extends State<RapportGenerationScreen> {
  final _syntheseController = TextEditingController();
  bool _isGeneratingIa = false;
  bool _isIaGenerated = false;
  bool _isGeneratingReport = false;
  String _selectedFormat = 'PDF';
  String? _downloadUrl;
  String? _message;

  @override
  void dispose() {
    _syntheseController.dispose();
    super.dispose();
  }

  Future<void> _generateIaDraft() async {
    setState(() {
      _isGeneratingIa = true;
      _message = null;
    });
    try {
      final response = await widget.repository.generateSyntheseIa(widget.missionId, 'FR');
      setState(() {
        _syntheseController.text = response['brouillon'] ?? '';
        _isIaGenerated = true;
        _isGeneratingIa = false;
      });
    } catch (e) {
      setState(() {
        _isGeneratingIa = false;
        _message = 'Erreur lors de la génération IA: $e';
      });
    }
  }

  Future<void> _compileReport() async {
    if (_syntheseController.text.trim().isEmpty) {
      ScaffoldMessenger.of(context).showSnackBar(
        const SnackBar(
          content: Text('Veuillez saisir ou générer une synthèse exécutive'),
          backgroundColor: AppColors.observation,
        ),
      );
      return;
    }

    setState(() {
      _isGeneratingReport = true;
      _downloadUrl = null;
      _message = null;
    });

    try {
      final rapportData = await widget.repository.generateRapport(
        missionId: widget.missionId,
        type: _selectedFormat,
        syntheseExecutive: _syntheseController.text,
        isIaGenerated: _isIaGenerated,
      );

      final String rapportId = rapportData['rapportId'].toString();
      final downloadUrl = await widget.repository.getDownloadUrl(rapportId);

      setState(() {
        _downloadUrl = downloadUrl;
        _isGeneratingReport = false;
        _message = 'Rapport généré avec succès !';
      });
    } catch (e) {
      setState(() {
        _isGeneratingReport = false;
        _message = 'Erreur lors de la génération du rapport : $e';
      });
    }
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      backgroundColor: AppColors.background,
      appBar: AppBar(
        title: Text('Rapport — ${widget.organismeNom}'),
      ),
      body: SingleChildScrollView(
        padding: const EdgeInsets.all(AppSpacing.m),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            // Module d'assistance IA
            AppCard(
              child: Column(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  Text('Synthèse exécutive', style: Theme.of(context).textTheme.titleMedium),
                  const SizedBox(height: AppSpacing.s),
                  Text(
                    'Rédigez la synthèse exécutive du rapport ou utilisez notre assistant IA souverain pour générer un premier brouillon à partir de vos constats.',
                    style: Theme.of(context).textTheme.bodySmall,
                  ),
                  const SizedBox(height: AppSpacing.m),
                  
                  _isGeneratingIa
                      ? const Center(
                          child: Padding(
                            padding: EdgeInsets.symmetric(vertical: AppSpacing.m),
                            child: Column(
                              children: [
                                CircularProgressIndicator(color: AppColors.primary),
                                SizedBox(height: AppSpacing.s),
                                Text('Modèle local Ollama (Mistral) en cours de rédaction...', style: TextStyle(fontSize: 12)),
                              ],
                            ),
                          ),
                        )
                      : PrimaryButton(
                          label: 'Générer un brouillon (IA)',
                          icon: Icons.auto_awesome_outlined,
                          onPressed: _generateIaDraft,
                        ),
                ],
              ),
            ),
            const SizedBox(height: AppSpacing.m),

            // Éditeur de texte et indicateurs
            if (_isIaGenerated)
              Container(
                width: double.infinity,
                padding: const EdgeInsets.all(AppSpacing.s),
                margin: const EdgeInsets.only(bottom: AppSpacing.s),
                decoration: BoxDecoration(
                  color: AppColors.observationBg,
                  borderRadius: BorderRadius.circular(6),
                  border: Border.all(color: AppColors.observation.withOpacity(0.3)),
                ),
                child: const Row(
                  children: [
                    Icon(Icons.info_outline, color: AppColors.observation, size: 18),
                    SizedBox(width: AppSpacing.s),
                    Expanded(
                      child: Text(
                        'Brouillon généré par IA. L\'auditeur doit valider le texte final sous sa responsabilité.',
                        style: TextStyle(color: AppColors.observation, fontSize: 11, fontWeight: FontWeight.bold),
                      ),
                    ),
                  ],
                ),
              ),

            TextField(
              controller: _syntheseController,
              maxLines: 8,
              decoration: const InputDecoration(
                hintText: 'Saisissez la synthèse du rapport...',
                border: OutlineInputBorder(),
                filled: true,
                fillColor: AppColors.surface,
              ),
            ),
            const SizedBox(height: AppSpacing.m),

            // Format & Compilations
            AppCard(
              child: Column(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  Text('Format d\'exportation', style: Theme.of(context).textTheme.titleMedium),
                  Row(
                    children: [
                      Radio<String>(
                        value: 'PDF',
                        groupValue: _selectedFormat,
                        onChanged: (v) => setState(() => _selectedFormat = v!),
                      ),
                      const Text('PDF (Document officiel ANCS)'),
                    ],
                  ),
                  Row(
                    children: [
                      Radio<String>(
                        value: 'DOCX',
                        groupValue: _selectedFormat,
                        onChanged: (v) => setState(() => _selectedFormat = v!),
                      ),
                      const Text('Word (DOCX — modifiable)'),
                    ],
                  ),
                  const Divider(height: AppSpacing.l),
                  _isGeneratingReport
                      ? const Center(child: CircularProgressIndicator(color: AppColors.primary))
                      : PrimaryButton(
                          label: 'Compiler le rapport final',
                          icon: Icons.document_scanner_outlined,
                          onPressed: _compileReport,
                        ),
                ],
              ),
            ),
            const SizedBox(height: AppSpacing.m),

            // Retours messages & liens de téléchargement
            if (_message != null)
              Padding(
                padding: const EdgeInsets.symmetric(vertical: AppSpacing.s),
                child: Center(
                  child: Text(
                    _message!,
                    style: TextStyle(
                      color: _downloadUrl != null ? AppColors.conforme : AppColors.nonConforme,
                      fontWeight: FontWeight.bold,
                    ),
                  ),
                ),
              ),

            if (_downloadUrl != null)
              AppCard(
                color: AppColors.conforme.withOpacity(0.05),
                child: Column(
                  children: [
                    const Icon(Icons.check_circle_outline, color: AppColors.conforme, size: 36),
                    const SizedBox(height: AppSpacing.s),
                    Text(
                      'Votre document est prêt pour le téléchargement sécurisé.',
                      textAlign: TextAlign.center,
                      style: Theme.of(context).textTheme.bodySmall,
                    ),
                    const SizedBox(height: AppSpacing.m),
                    SecondaryButton(
                      label: 'Télécharger le document',
                      icon: Icons.download_for_offline_outlined,
                      onPressed: () async {
                        final uri = Uri.parse(_downloadUrl!);
                        if (await canLaunchUrl(uri)) {
                          await launchUrl(uri, mode: LaunchMode.externalApplication);
                        } else {
                          ScaffoldMessenger.of(context).showSnackBar(
                            const SnackBar(
                              content: Text('Impossible d\'ouvrir le lien de téléchargement'),
                              backgroundColor: AppColors.nonConforme,
                            ),
                          );
                        }
                      },
                    ),
                  ],
                ),
              ),
          ],
        ),
      ),
    );
  }
}
