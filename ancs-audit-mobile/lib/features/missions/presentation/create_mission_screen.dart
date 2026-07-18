import 'package:flutter/material.dart';
import '../../../core/constants/app_colors.dart';
import '../../../core/constants/app_spacing.dart';
import '../../../core/widgets/app_buttons.dart';
import '../data/mission_repository.dart';

class CreateMissionScreen extends StatefulWidget {
  final MissionRepository repository;

  const CreateMissionScreen({Key? key, required this.repository})
      : super(key: key);

  @override
  State<CreateMissionScreen> createState() => _CreateMissionScreenState();
}

class _CreateMissionScreenState extends State<CreateMissionScreen> {
  final _formKey = GlobalKey<FormState>();

  // Dropdown data
  List<dynamic> _organismes = [];
  List<dynamic> _auditeurs = [];
  List<dynamic> _referentiels = [];

  // Selected values
  String? _selectedOrganismeId;
  String? _selectedAuditeurId;
  String? _selectedReferentielId;
  DateTime? _dateDebut;
  DateTime? _dateFin;

  // Text controllers
  final _perimetreController = TextEditingController();

  bool _isLoadingData = true;
  bool _isSubmitting = false;
  String? _loadError;

  @override
  void initState() {
    super.initState();
    _loadFormData();
  }

  @override
  void dispose() {
    _perimetreController.dispose();
    super.dispose();
  }

  Future<void> _loadFormData() async {
    setState(() {
      _isLoadingData = true;
      _loadError = null;
    });
    try {
      final results = await Future.wait([
        widget.repository.getOrganismes(),
        widget.repository.getAuditeurs(),
        widget.repository.getReferentiels(),
      ]);
      setState(() {
        _organismes = results[0];
        _auditeurs = results[1];
        _referentiels = results[2];
        _isLoadingData = false;
      });
    } catch (e) {
      setState(() {
        _loadError = 'Impossible de charger les données : $e';
        _isLoadingData = false;
      });
    }
  }

  Future<void> _pickDate(bool isStart) async {
    final now = DateTime.now();
    final picked = await showDatePicker(
      context: context,
      initialDate: isStart
          ? (_dateDebut ?? now)
          : (_dateFin ?? (_dateDebut ?? now).add(const Duration(days: 30))),
      firstDate: isStart ? now.subtract(const Duration(days: 365)) : (_dateDebut ?? now),
      lastDate: now.add(const Duration(days: 730)),
      builder: (context, child) => Theme(
        data: Theme.of(context).copyWith(
          colorScheme: const ColorScheme.light(primary: AppColors.primary),
        ),
        child: child!,
      ),
    );
    if (picked != null) {
      setState(() {
        if (isStart) {
          _dateDebut = picked;
          if (_dateFin != null && _dateFin!.isBefore(picked)) {
            _dateFin = null;
          }
        } else {
          _dateFin = picked;
        }
      });
    }
  }

  Future<void> _submit() async {
    if (!_formKey.currentState!.validate()) return;
    if (_selectedOrganismeId == null || _selectedAuditeurId == null || _selectedReferentielId == null) {
      ScaffoldMessenger.of(context).showSnackBar(
        const SnackBar(
          content: Text('Veuillez renseigner tous les champs obligatoires.'),
          backgroundColor: AppColors.nonConforme,
        ),
      );
      return;
    }

    setState(() => _isSubmitting = true);

    final body = <String, dynamic>{
      'organismeId': _selectedOrganismeId,
      'auditeurId': _selectedAuditeurId,
      'referentielId': _selectedReferentielId,
      if (_perimetreController.text.trim().isNotEmpty)
        'perimetre': _perimetreController.text.trim(),
      if (_dateDebut != null)
        'dateDebut': '${_dateDebut!.year.toString().padLeft(4, '0')}-'
            '${_dateDebut!.month.toString().padLeft(2, '0')}-'
            '${_dateDebut!.day.toString().padLeft(2, '0')}',
      if (_dateFin != null)
        'dateFin': '${_dateFin!.year.toString().padLeft(4, '0')}-'
            '${_dateFin!.month.toString().padLeft(2, '0')}-'
            '${_dateFin!.day.toString().padLeft(2, '0')}',
    };

    try {
      await widget.repository.createMission(body);
      if (mounted) {
        ScaffoldMessenger.of(context).showSnackBar(
          const SnackBar(
            content: Text('Mission créée avec succès !'),
            backgroundColor: AppColors.conforme,
          ),
        );
        Navigator.of(context).pop(true); // signal caller to refresh
      }
    } catch (e) {
      setState(() => _isSubmitting = false);
      if (mounted) {
        ScaffoldMessenger.of(context).showSnackBar(
          SnackBar(
            content: Text('Erreur lors de la création : $e'),
            backgroundColor: AppColors.nonConforme,
          ),
        );
      }
    }
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      backgroundColor: AppColors.background,
      appBar: AppBar(
        title: const Text('Nouvelle Mission d\'Audit'),
        backgroundColor: AppColors.primary,
        foregroundColor: Colors.white,
        elevation: 0,
      ),
      body: _isLoadingData
          ? const Center(child: CircularProgressIndicator())
          : _loadError != null
              ? _buildLoadError()
              : _buildForm(),
    );
  }

  Widget _buildLoadError() {
    return Center(
      child: Padding(
        padding: const EdgeInsets.all(AppSpacing.l),
        child: Column(
          mainAxisAlignment: MainAxisAlignment.center,
          children: [
            const Icon(Icons.cloud_off, size: 48, color: AppColors.textSecondary),
            const SizedBox(height: AppSpacing.m),
            Text(_loadError!, textAlign: TextAlign.center),
            const SizedBox(height: AppSpacing.m),
            ElevatedButton(onPressed: _loadFormData, child: const Text('Réessayer')),
          ],
        ),
      ),
    );
  }

  Widget _buildForm() {
    return SingleChildScrollView(
      padding: const EdgeInsets.all(AppSpacing.m),
      child: Form(
        key: _formKey,
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            _sectionTitle('Organisme audité *'),
            _buildDropdown(
              hint: 'Sélectionner l\'organisme',
              value: _selectedOrganismeId,
              items: _organismes.map((o) {
                final id = o['id'].toString();
                final nom = o['nom']?.toString() ?? id;
                return DropdownMenuItem(value: id, child: Text(nom));
              }).toList(),
              onChanged: (v) => setState(() => _selectedOrganismeId = v as String?),
              validator: (v) => v == null ? 'Champ obligatoire' : null,
            ),
            const SizedBox(height: AppSpacing.m),

            _sectionTitle('Auditeur assigné *'),
            _buildDropdown(
              hint: 'Sélectionner l\'auditeur',
              value: _selectedAuditeurId,
              items: _auditeurs.map((a) {
                final id = a['id'].toString();
                final nom = a['nom']?.toString() ?? a['utilisateurNom']?.toString() ?? id;
                return DropdownMenuItem(value: id, child: Text(nom));
              }).toList(),
              onChanged: (v) => setState(() => _selectedAuditeurId = v as String?),
              validator: (v) => v == null ? 'Champ obligatoire' : null,
            ),
            const SizedBox(height: AppSpacing.m),

            _sectionTitle('Référentiel applicable *'),
            _buildDropdown(
              hint: 'Sélectionner le référentiel',
              value: _selectedReferentielId,
              items: _referentiels.map((r) {
                final id = r['id'].toString();
                final nom = r['nom']?.toString() ?? id;
                return DropdownMenuItem(value: id, child: Text(nom));
              }).toList(),
              onChanged: (v) => setState(() => _selectedReferentielId = v as String?),
              validator: (v) => v == null ? 'Champ obligatoire' : null,
            ),
            const SizedBox(height: AppSpacing.m),

            _sectionTitle('Périmètre audité'),
            TextFormField(
              controller: _perimetreController,
              maxLines: 3,
              decoration: InputDecoration(
                hintText: 'Ex: Datacenter, Applications métier, Réseau interne…',
                filled: true,
                fillColor: AppColors.surface,
                border: OutlineInputBorder(
                  borderRadius: BorderRadius.circular(10),
                  borderSide: const BorderSide(color: AppColors.divider),
                ),
                focusedBorder: OutlineInputBorder(
                  borderRadius: BorderRadius.circular(10),
                  borderSide: const BorderSide(color: AppColors.primary, width: 2),
                ),
              ),
            ),
            const SizedBox(height: AppSpacing.m),

            _sectionTitle('Période de la mission'),
            Row(
              children: [
                Expanded(child: _buildDateButton('Début', _dateDebut, () => _pickDate(true))),
                const SizedBox(width: AppSpacing.m),
                Expanded(child: _buildDateButton('Fin', _dateFin, () => _pickDate(false))),
              ],
            ),
            const SizedBox(height: AppSpacing.xl),

            PrimaryButton(
              label: 'Créer la mission',
              icon: Icons.add_task,
              isLoading: _isSubmitting,
              onPressed: _submit,
            ),
            const SizedBox(height: AppSpacing.m),
          ],
        ),
      ),
    );
  }

  Widget _sectionTitle(String title) {
    return Padding(
      padding: const EdgeInsets.only(bottom: 6),
      child: Text(
        title,
        style: const TextStyle(
          fontWeight: FontWeight.w600,
          fontSize: 13,
          color: AppColors.textSecondary,
        ),
      ),
    );
  }

  Widget _buildDropdown({
    required String hint,
    required String? value,
    required List<DropdownMenuItem<String>> items,
    required ValueChanged<Object?> onChanged,
    required FormFieldValidator<String?> validator,
  }) {
    return DropdownButtonFormField<String>(
      value: value,
      items: items,
      onChanged: onChanged,
      validator: validator,
      isExpanded: true,
      decoration: InputDecoration(
        hintText: hint,
        filled: true,
        fillColor: AppColors.surface,
        contentPadding: const EdgeInsets.symmetric(horizontal: 12, vertical: 14),
        border: OutlineInputBorder(
          borderRadius: BorderRadius.circular(10),
          borderSide: const BorderSide(color: AppColors.divider),
        ),
        focusedBorder: OutlineInputBorder(
          borderRadius: BorderRadius.circular(10),
          borderSide: const BorderSide(color: AppColors.primary, width: 2),
        ),
      ),
    );
  }

  Widget _buildDateButton(String label, DateTime? date, VoidCallback onTap) {
    final text = date == null
        ? 'Non définie'
        : '${date.day.toString().padLeft(2, '0')}/${date.month.toString().padLeft(2, '0')}/${date.year}';

    return InkWell(
      onTap: onTap,
      borderRadius: BorderRadius.circular(10),
      child: Container(
        padding: const EdgeInsets.symmetric(horizontal: 12, vertical: 14),
        decoration: BoxDecoration(
          color: AppColors.surface,
          borderRadius: BorderRadius.circular(10),
          border: Border.all(color: AppColors.divider),
        ),
        child: Row(
          children: [
            const Icon(Icons.calendar_today_outlined, size: 16, color: AppColors.primary),
            const SizedBox(width: 8),
            Expanded(
              child: Column(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  Text(label, style: const TextStyle(fontSize: 10, color: AppColors.textSecondary)),
                  Text(
                    text,
                    style: TextStyle(
                      fontSize: 13,
                      fontWeight: date != null ? FontWeight.w600 : FontWeight.normal,
                      color: date != null ? AppColors.primary : AppColors.textSecondary,
                    ),
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
