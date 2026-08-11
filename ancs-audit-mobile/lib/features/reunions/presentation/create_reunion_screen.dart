import 'package:flutter/material.dart';
import '../../../core/constants/app_colors.dart';
import '../../../core/constants/app_spacing.dart';
import '../../../core/widgets/app_buttons.dart';
import '../data/reunion_repository.dart';

class CreateReunionScreen extends StatefulWidget {
  final ReunionRepository repository;
  final String missionId;

  const CreateReunionScreen({Key? key, required this.repository, required this.missionId}) : super(key: key);

  @override
  State<CreateReunionScreen> createState() => _CreateReunionScreenState();
}

class _CreateReunionScreenState extends State<CreateReunionScreen> {
  final _formKey = GlobalKey<FormState>();

  DateTime? _dateReunion;
  TimeOfDay? _timeReunion;
  final _participantsController = TextEditingController();
  final _compteRenduController = TextEditingController();

  bool _isSubmitting = false;

  @override
  void dispose() {
    _participantsController.dispose();
    _compteRenduController.dispose();
    super.dispose();
  }

  Future<void> _pickDate() async {
    final now = DateTime.now();
    final picked = await showDatePicker(
      context: context,
      initialDate: _dateReunion ?? now,
      firstDate: now.subtract(const Duration(days: 365)),
      lastDate: now.add(const Duration(days: 365)),
      builder: (context, child) => Theme(
        data: Theme.of(context).copyWith(
          colorScheme: const ColorScheme.light(primary: AppColors.primary),
        ),
        child: child!,
      ),
    );
    if (picked != null) {
      setState(() => _dateReunion = picked);
    }
  }

  Future<void> _pickTime() async {
    final now = TimeOfDay.now();
    final picked = await showTimePicker(
      context: context,
      initialTime: _timeReunion ?? now,
      builder: (context, child) => Theme(
        data: Theme.of(context).copyWith(
          colorScheme: const ColorScheme.light(primary: AppColors.primary),
        ),
        child: child!,
      ),
    );
    if (picked != null) {
      setState(() => _timeReunion = picked);
    }
  }

  Future<void> _submit() async {
    if (!_formKey.currentState!.validate()) return;
    
    if (_dateReunion == null || _timeReunion == null) {
      ScaffoldMessenger.of(context).showSnackBar(
        const SnackBar(
          content: Text('Veuillez renseigner la date et l\'heure.'),
          backgroundColor: AppColors.nonConforme,
        ),
      );
      return;
    }

    setState(() => _isSubmitting = true);

    final dateTime = DateTime(
      _dateReunion!.year,
      _dateReunion!.month,
      _dateReunion!.day,
      _timeReunion!.hour,
      _timeReunion!.minute,
    );

    final body = <String, dynamic>{
      'missionId': widget.missionId,
      'dateReunion': dateTime.toUtc().toIso8601String(),
      'participants': _participantsController.text.trim(),
      'compteRendu': _compteRenduController.text.trim(),
    };

    try {
      await widget.repository.createReunion(body);
      if (mounted) {
        ScaffoldMessenger.of(context).showSnackBar(
          const SnackBar(
            content: Text('Réunion de travail ajoutée avec succès !'),
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
            content: Text('Erreur lors de l\'ajout : $e'),
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
        title: const Text('Nouvelle Réunion de Travail'),
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
              _sectionTitle('Date et Heure *'),
              Row(
                children: [
                  Expanded(
                    child: _buildDateTimePicker(
                      label: _dateReunion == null 
                        ? 'Date' 
                        : '${_dateReunion!.day.toString().padLeft(2, '0')}/${_dateReunion!.month.toString().padLeft(2, '0')}/${_dateReunion!.year}',
                      icon: Icons.calendar_today,
                      onTap: _pickDate,
                    ),
                  ),
                  const SizedBox(width: AppSpacing.m),
                  Expanded(
                    child: _buildDateTimePicker(
                      label: _timeReunion == null 
                        ? 'Heure' 
                        : '${_timeReunion!.hour.toString().padLeft(2, '0')}:${_timeReunion!.minute.toString().padLeft(2, '0')}',
                      icon: Icons.access_time,
                      onTap: _pickTime,
                    ),
                  ),
                ],
              ),
              const SizedBox(height: AppSpacing.m),
              _sectionTitle('Participants'),
              TextFormField(
                controller: _participantsController,
                maxLines: 2,
                decoration: _inputDecoration(hint: 'Noms des participants'),
              ),
              const SizedBox(height: AppSpacing.m),
              _sectionTitle('Compte rendu'),
              TextFormField(
                controller: _compteRenduController,
                maxLines: 5,
                decoration: _inputDecoration(hint: 'Résumé des discussions et décisions'),
              ),
              const SizedBox(height: AppSpacing.xl),
              PrimaryButton(
                label: 'Ajouter la réunion',
                icon: Icons.add,
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

  Widget _buildDateTimePicker({required String label, required IconData icon, required VoidCallback onTap}) {
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
            Icon(icon, size: 16, color: AppColors.primary),
            const SizedBox(width: 8),
            Expanded(
              child: Text(
                label,
                style: const TextStyle(fontSize: 13, fontWeight: FontWeight.w500, color: AppColors.textPrimary),
              ),
            ),
          ],
        ),
      ),
    );
  }
}
