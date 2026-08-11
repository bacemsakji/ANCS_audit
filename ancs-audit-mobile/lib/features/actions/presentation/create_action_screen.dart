import 'package:flutter/material.dart';
import '../../../core/constants/app_colors.dart';
import '../../../core/constants/app_spacing.dart';
import '../../../core/widgets/app_buttons.dart';
import '../data/action_repository.dart';

class CreateActionScreen extends StatefulWidget {
  final ActionRepository repository;
  final String missionId;

  const CreateActionScreen({
    Key? key,
    required this.repository,
    required this.missionId,
  }) : super(key: key);

  @override
  State<CreateActionScreen> createState() => _CreateActionScreenState();
}

class _CreateActionScreenState extends State<CreateActionScreen> {
  final _formKey = GlobalKey<FormState>();

  List<dynamic> _constats = [];
  String? _selectedConstatId;
  String _selectedPriorite = 'MOYENNE';

  final _descriptionController = TextEditingController();
  final _responsableController = TextEditingController();
  DateTime? _echeance;

  bool _isLoadingData = true;
  bool _isSubmitting = false;
  String? _loadError;

  @override
  void initState() {
    super.initState();
    _loadData();
  }

  @override
  void dispose() {
    _descriptionController.dispose();
    _responsableController.dispose();
    super.dispose();
  }

  Future<void> _loadData() async {
    setState(() {
      _isLoadingData = true;
      _loadError = null;
    });
    try {
      final constats = await widget.repository.getConstatsByMission(widget.missionId);
      setState(() {
        // Optionnel : ne garder que les constats NON_CONFORME
        _constats = constats.where((c) => c['estConforme'] == false).toList();
        _isLoadingData = false;
      });
    } catch (e) {
      setState(() {
        _loadError = 'Impossible de charger les constats : $e';
        _isLoadingData = false;
      });
    }
  }

  Future<void> _pickDate() async {
    final now = DateTime.now();
    final picked = await showDatePicker(
      context: context,
      initialDate: _echeance ?? now,
      firstDate: now.subtract(const Duration(days: 30)),
      lastDate: now.add(const Duration(days: 3650)),
      builder: (context, child) => Theme(
        data: Theme.of(context).copyWith(
          colorScheme: const ColorScheme.light(primary: AppColors.primary),
        ),
        child: child!,
      ),
    );
    if (picked != null) {
      setState(() => _echeance = picked);
    }
  }

  Future<void> _submit() async {
    if (!_formKey.currentState!.validate()) return;
    
    if (_selectedConstatId == null) {
      ScaffoldMessenger.of(context).showSnackBar(
        const SnackBar(
          content: Text('Veuillez sélectionner un constat.'),
          backgroundColor: AppColors.nonConforme,
        ),
      );
      return;
    }

    setState(() => _isSubmitting = true);

    final body = <String, dynamic>{
      'constatId': _selectedConstatId,
      'description': _descriptionController.text.trim(),
      'priorite': _selectedPriorite,
      if (_responsableController.text.trim().isNotEmpty) 'responsable': _responsableController.text.trim(),
      if (_echeance != null) 'echeance': '${_echeance!.year.toString().padLeft(4, '0')}-${_echeance!.month.toString().padLeft(2, '0')}-${_echeance!.day.toString().padLeft(2, '0')}',
    };

    try {
      await widget.repository.createAction(body);
      if (mounted) {
        ScaffoldMessenger.of(context).showSnackBar(
          const SnackBar(
            content: Text('Action corrective créée avec succès !'),
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
        title: const Text('Nouvelle Action Corrective'),
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
            ElevatedButton(onPressed: _loadData, child: const Text('Réessayer')),
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
            _sectionTitle('Constat associé *'),
            if (_constats.isEmpty)
              const Padding(
                padding: EdgeInsets.only(bottom: AppSpacing.m),
                child: Text('Aucun constat non-conforme trouvé pour cette mission.', style: TextStyle(color: AppColors.nonConforme)),
              )
            else
              _buildDropdown(
                hint: 'Sélectionner le constat',
                value: _selectedConstatId,
                items: _constats.map((c) {
                  final id = c['id'].toString();
                  final description = c['description']?.toString() ?? 'Constat sans description';
                  return DropdownMenuItem(
                    value: id,
                    child: Text(
                      description,
                      maxLines: 2,
                      overflow: TextOverflow.ellipsis,
                    ),
                  );
                }).toList(),
                onChanged: (v) => setState(() => _selectedConstatId = v as String?),
                validator: (v) => v == null ? 'Champ obligatoire' : null,
              ),
            const SizedBox(height: AppSpacing.m),
            
            _sectionTitle('Description de l\'action *'),
            TextFormField(
              controller: _descriptionController,
              validator: (v) => v == null || v.trim().isEmpty ? 'Champ obligatoire' : null,
              maxLines: 4,
              decoration: _inputDecoration(hint: 'Que faut-il faire pour corriger la non-conformité ?'),
            ),
            const SizedBox(height: AppSpacing.m),

            Row(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                Expanded(
                  flex: 2,
                  child: Column(
                    crossAxisAlignment: CrossAxisAlignment.start,
                    children: [
                      _sectionTitle('Priorité *'),
                      _buildDropdown(
                        hint: 'Priorité',
                        value: _selectedPriorite,
                        items: ['FAIBLE', 'MOYENNE', 'HAUTE', 'CRITIQUE']
                            .map((p) => DropdownMenuItem(value: p, child: Text(p)))
                            .toList(),
                        onChanged: (v) => setState(() => _selectedPriorite = v as String),
                        validator: null,
                      ),
                    ],
                  ),
                ),
                const SizedBox(width: AppSpacing.m),
                Expanded(
                  flex: 3,
                  child: Column(
                    crossAxisAlignment: CrossAxisAlignment.start,
                    children: [
                      _sectionTitle('Échéance'),
                      _buildDateButton('Date limite', _echeance, _pickDate),
                    ],
                  ),
                ),
              ],
            ),
            const SizedBox(height: AppSpacing.m),

            _sectionTitle('Responsable'),
            TextFormField(
              controller: _responsableController,
              decoration: _inputDecoration(hint: 'Nom ou équipe responsable'),
            ),
            const SizedBox(height: AppSpacing.xl),

            PrimaryButton(
              label: 'Créer l\'action',
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
    required FormFieldValidator<String?>? validator,
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
