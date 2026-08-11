import 'package:flutter/material.dart';
import '../../../core/constants/app_colors.dart';
import '../../../core/constants/app_spacing.dart';
import '../../../core/widgets/app_buttons.dart';
import '../data/auditeur_repository.dart';

class CertifyAuditeurScreen extends StatefulWidget {
  final AuditeurRepository repository;

  const CertifyAuditeurScreen({Key? key, required this.repository}) : super(key: key);

  @override
  State<CertifyAuditeurScreen> createState() => _CertifyAuditeurScreenState();
}

class _CertifyAuditeurScreenState extends State<CertifyAuditeurScreen> {
  final _formKey = GlobalKey<FormState>();

  List<dynamic> _utilisateurs = [];
  String? _selectedUtilisateurId;

  final _numeroCertificationController = TextEditingController();
  DateTime? _dateCertification;
  DateTime? _dateExpiration;
  final _specialitesController = TextEditingController();

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
    _numeroCertificationController.dispose();
    _specialitesController.dispose();
    super.dispose();
  }

  Future<void> _loadFormData() async {
    setState(() {
      _isLoadingData = true;
      _loadError = null;
    });
    try {
      final utilisateurs = await widget.repository.getUtilisateurs();
      setState(() {
        // Only include users who might need to be certified (e.g. AUDITEUR role)
        // Since we don't know the exact filtering, we'll just show them all.
        _utilisateurs = utilisateurs;
        _isLoadingData = false;
      });
    } catch (e) {
      setState(() {
        _loadError = 'Impossible de charger les utilisateurs : $e';
        _isLoadingData = false;
      });
    }
  }

  Future<void> _pickDate(bool isCertification) async {
    final now = DateTime.now();
    final picked = await showDatePicker(
      context: context,
      initialDate: isCertification
          ? (_dateCertification ?? now)
          : (_dateExpiration ?? (_dateCertification ?? now).add(const Duration(days: 365))),
      firstDate: isCertification
          ? now.subtract(const Duration(days: 3650))
          : (_dateCertification ?? now),
      lastDate: now.add(const Duration(days: 3650)),
      builder: (context, child) => Theme(
        data: Theme.of(context).copyWith(
          colorScheme: const ColorScheme.light(primary: AppColors.primary),
        ),
        child: child!,
      ),
    );
    if (picked != null) {
      setState(() {
        if (isCertification) {
          _dateCertification = picked;
          if (_dateExpiration != null && _dateExpiration!.isBefore(picked)) {
            _dateExpiration = null;
          }
        } else {
          _dateExpiration = picked;
        }
      });
    }
  }

  Future<void> _submit() async {
    if (!_formKey.currentState!.validate()) return;
    
    if (_selectedUtilisateurId == null || _dateCertification == null || _dateExpiration == null) {
      ScaffoldMessenger.of(context).showSnackBar(
        const SnackBar(
          content: Text('Veuillez renseigner tous les champs obligatoires (Dates et Utilisateur).'),
          backgroundColor: AppColors.nonConforme,
        ),
      );
      return;
    }

    setState(() => _isSubmitting = true);

    List<String> specialites = [];
    if (_specialitesController.text.trim().isNotEmpty) {
      specialites = _specialitesController.text.split(',').map((e) => e.trim()).toList();
    }

    final body = <String, dynamic>{
      'utilisateurId': _selectedUtilisateurId,
      'numeroCertification': _numeroCertificationController.text.trim(),
      'dateCertification': '${_dateCertification!.year.toString().padLeft(4, '0')}-${_dateCertification!.month.toString().padLeft(2, '0')}-${_dateCertification!.day.toString().padLeft(2, '0')}',
      'dateExpiration': '${_dateExpiration!.year.toString().padLeft(4, '0')}-${_dateExpiration!.month.toString().padLeft(2, '0')}-${_dateExpiration!.day.toString().padLeft(2, '0')}',
      if (specialites.isNotEmpty) 'specialites': specialites,
    };

    try {
      await widget.repository.certifyAuditeur(body);
      if (mounted) {
        ScaffoldMessenger.of(context).showSnackBar(
          const SnackBar(
            content: Text('Auditeur certifié avec succès !'),
            backgroundColor: AppColors.conforme,
          ),
        );
        Navigator.of(context).pop(true);
      }
    } catch (e) {
      setState(() => _isSubmitting = false);
      if (mounted) {
        ScaffoldMessenger.of(context).showSnackBar(
          SnackBar(
            content: Text('Erreur lors de la certification : $e'),
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
        title: const Text('Certifier un Auditeur'),
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
            _sectionTitle('Utilisateur *'),
            _buildDropdown(
              hint: 'Sélectionner l\'utilisateur',
              value: _selectedUtilisateurId,
              items: _utilisateurs.map((u) {
                final id = u['id'].toString();
                final nom = '${u['prenom'] ?? ''} ${u['nom'] ?? ''}'.trim();
                final display = nom.isEmpty ? id : nom;
                return DropdownMenuItem(value: id, child: Text(display));
              }).toList(),
              onChanged: (v) => setState(() => _selectedUtilisateurId = v as String?),
              validator: (v) => v == null ? 'Champ obligatoire' : null,
            ),
            const SizedBox(height: AppSpacing.m),
            _sectionTitle('Numéro de certification *'),
            TextFormField(
              controller: _numeroCertificationController,
              validator: (v) => v == null || v.trim().isEmpty ? 'Champ obligatoire' : null,
              decoration: _inputDecoration(hint: 'Ex: CERT-2026-001'),
            ),
            const SizedBox(height: AppSpacing.m),
            _sectionTitle('Période de validité *'),
            Row(
              children: [
                Expanded(child: _buildDateButton('Date de certification', _dateCertification, () => _pickDate(true))),
                const SizedBox(width: AppSpacing.m),
                Expanded(child: _buildDateButton('Date d\'expiration', _dateExpiration, () => _pickDate(false))),
              ],
            ),
            const SizedBox(height: AppSpacing.m),
            _sectionTitle('Spécialités'),
            TextFormField(
              controller: _specialitesController,
              maxLines: 2,
              decoration: _inputDecoration(hint: 'Ex: Réseau, Cloud, Physique (Séparées par des virgules)'),
            ),
            const SizedBox(height: AppSpacing.xl),
            PrimaryButton(
              label: 'Certifier',
              icon: Icons.verified_user,
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
        style: const TextStyle(fontWeight: FontWeight.w600, fontSize: 13, color: AppColors.textSecondary),
      ),
    );
  }

  InputDecoration _inputDecoration({String? hint}) {
    return InputDecoration(
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
      initialValue: value,
      items: items,
      onChanged: onChanged,
      validator: validator,
      isExpanded: true,
      decoration: _inputDecoration(hint: hint),
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
