import 'package:flutter/material.dart';
import '../../../core/constants/app_colors.dart';
import '../../../core/constants/app_spacing.dart';
import '../../../core/widgets/app_buttons.dart';
import '../data/organisme_repository.dart';

class CreateOrganismeScreen extends StatefulWidget {
  final OrganismeRepository repository;

  const CreateOrganismeScreen({Key? key, required this.repository}) : super(key: key);

  @override
  State<CreateOrganismeScreen> createState() => _CreateOrganismeScreenState();
}

class _CreateOrganismeScreenState extends State<CreateOrganismeScreen> {
  final _formKey = GlobalKey<FormState>();

  final _nomController = TextEditingController();
  final _secteurController = TextEditingController();
  final _typeObligationController = TextEditingController();
  final _adresseController = TextEditingController();
  final _contactRssiController = TextEditingController();
  final _acronymeController = TextEditingController();
  final _statutController = TextEditingController();
  final _categorieController = TextEditingController();

  bool _isSubmitting = false;

  @override
  void dispose() {
    _nomController.dispose();
    _secteurController.dispose();
    _typeObligationController.dispose();
    _adresseController.dispose();
    _contactRssiController.dispose();
    _acronymeController.dispose();
    _statutController.dispose();
    _categorieController.dispose();
    super.dispose();
  }

  Future<void> _submit() async {
    if (!_formKey.currentState!.validate()) return;

    setState(() => _isSubmitting = true);

    final body = <String, dynamic>{
      'nom': _nomController.text.trim(),
      if (_secteurController.text.trim().isNotEmpty) 'secteurActivite': _secteurController.text.trim(),
      if (_typeObligationController.text.trim().isNotEmpty) 'typeObligationAudit': _typeObligationController.text.trim(),
      if (_adresseController.text.trim().isNotEmpty) 'adresse': _adresseController.text.trim(),
      if (_contactRssiController.text.trim().isNotEmpty) 'contactRssiEmail': _contactRssiController.text.trim(),
      if (_acronymeController.text.trim().isNotEmpty) 'acronyme': _acronymeController.text.trim(),
      if (_statutController.text.trim().isNotEmpty) 'statut': _statutController.text.trim(),
      if (_categorieController.text.trim().isNotEmpty) 'categorie': _categorieController.text.trim(),
    };

    try {
      await widget.repository.createOrganisme(body);
      if (mounted) {
        ScaffoldMessenger.of(context).showSnackBar(
          const SnackBar(
            content: Text('Organisme créé avec succès !'),
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
        title: const Text('Nouvel Organisme'),
        backgroundColor: AppColors.primary,
        foregroundColor: Colors.white,
        elevation: 0,
      ),
      body: SingleChildScrollView(
        padding: const EdgeInsets.all(AppSpacing.m),
        child: Form(
          key: _formKey,
          child: Column(
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              _buildTextField('Nom *', _nomController, isRequired: true),
              const SizedBox(height: AppSpacing.m),
              _buildTextField('Acronyme', _acronymeController),
              const SizedBox(height: AppSpacing.m),
              _buildTextField('Secteur d\'activité', _secteurController),
              const SizedBox(height: AppSpacing.m),
              _buildTextField('Type d\'obligation d\'audit', _typeObligationController),
              const SizedBox(height: AppSpacing.m),
              _buildTextField('Catégorie', _categorieController),
              const SizedBox(height: AppSpacing.m),
              _buildTextField('Adresse', _adresseController, maxLines: 2),
              const SizedBox(height: AppSpacing.m),
              _buildTextField('Email RSSI', _contactRssiController, keyboardType: TextInputType.emailAddress),
              const SizedBox(height: AppSpacing.m),
              _buildTextField('Statut', _statutController),
              const SizedBox(height: AppSpacing.xl),
              PrimaryButton(
                label: 'Créer l\'organisme',
                icon: Icons.business,
                isLoading: _isSubmitting,
                onPressed: _submit,
              ),
              const SizedBox(height: AppSpacing.m),
            ],
          ),
        ),
      ),
    );
  }

  Widget _buildTextField(String label, TextEditingController controller, {bool isRequired = false, int maxLines = 1, TextInputType? keyboardType}) {
    return Column(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: [
        Padding(
          padding: const EdgeInsets.only(bottom: 6),
          child: Text(
            label,
            style: const TextStyle(fontWeight: FontWeight.w600, fontSize: 13, color: AppColors.textSecondary),
          ),
        ),
        TextFormField(
          controller: controller,
          maxLines: maxLines,
          keyboardType: keyboardType,
          validator: isRequired ? (v) => v == null || v.trim().isEmpty ? 'Champ obligatoire' : null : null,
          decoration: InputDecoration(
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
        ),
      ],
    );
  }
}
