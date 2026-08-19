import 'package:flutter/material.dart';
import 'package:flutter/services.dart';
import 'package:flutter_bloc/flutter_bloc.dart';
import 'package:go_router/go_router.dart';
import '../bloc/auth_bloc.dart';
import '../bloc/auth_event.dart';
import '../bloc/auth_state.dart';
import '../../../core/constants/app_colors.dart';
import '../../../core/constants/app_spacing.dart';
import '../../../core/widgets/app_buttons.dart';

/// Écran de vérification TOTP (2FA) pour les administrateurs ANCS.
class TotpScreen extends StatefulWidget {
  final String email;
  final String mfaToken;

  const TotpScreen({
    Key? key,
    required this.email,
    required this.mfaToken,
  }) : super(key: key);

  @override
  State<TotpScreen> createState() => _TotpScreenState();
}

class _TotpScreenState extends State<TotpScreen> {
  final List<TextEditingController> _controllers =
      List.generate(6, (_) => TextEditingController());
  final List<FocusNode> _focusNodes = List.generate(6, (_) => FocusNode());

  @override
  void dispose() {
    for (final c in _controllers) c.dispose();
    for (final f in _focusNodes) f.dispose();
    super.dispose();
  }

  String get _code => _controllers.map((c) => c.text).join();

  void _submit() {
    if (_code.length != 6) return;
    context.read<AuthBloc>().add(
          TotpSubmitted(mfaToken: widget.mfaToken, code: _code),
        );
  }

  void _onChanged(String value, int index) {
    final cleanValue = value.replaceAll(RegExp(r'\D'), '');

    // Gestion du collé (paste code à 6 chiffres ou multi-chiffres)
    if (cleanValue.length > 1) {
      final startIdx = (cleanValue.length == 6) ? 0 : index;
      for (int j = 0; j < 6; j++) {
        final charIdx = j - startIdx;
        if (charIdx >= 0 && charIdx < cleanValue.length) {
          _controllers[j].text = cleanValue[charIdx];
        }
      }
      final nextFocus = (startIdx + cleanValue.length).clamp(0, 5);
      _focusNodes[nextFocus].requestFocus();
      if (_code.length == 6) _submit();
      return;
    }

    // Si la case contenait déjà un chiffre et qu'un nouveau est tapé, garder le dernier
    if (value.length > 1) {
      _controllers[index].text = value.substring(value.length - 1);
      _controllers[index].selection =
          TextSelection.collapsed(offset: _controllers[index].text.length);
    }

    // Auto-avance vers le champ suivant
    if (_controllers[index].text.isNotEmpty && index < 5) {
      _focusNodes[index + 1].requestFocus();
    }

    // Soumission automatique quand 6 chiffres sont saisis
    if (_code.length == 6) {
      _submit();
    }
  }

  void _handleKeyEvent(KeyEvent event, int index) {
    if (event is KeyDownEvent &&
        event.logicalKey == LogicalKeyboardKey.backspace) {
      if (_controllers[index].text.isEmpty && index > 0) {
        _controllers[index - 1].clear();
        _focusNodes[index - 1].requestFocus();
      }
    }
  }

  @override
  Widget build(BuildContext context) {
    final screenHeight = MediaQuery.of(context).size.height;

    return BlocListener<AuthBloc, AuthState>(
      listener: (context, state) {
        if (state is AuthAuthenticated) {
          context.go('/dashboard');
        } else if (state is AuthError) {
          for (final c in _controllers) c.clear();
          _focusNodes[0].requestFocus();
          ScaffoldMessenger.of(context).showSnackBar(
            SnackBar(
              content: Text(state.message),
              backgroundColor: AppColors.nonConforme,
            ),
          );
        }
      },
      child: Scaffold(
        backgroundColor: AppColors.primary,
        appBar: AppBar(
          backgroundColor: AppColors.primary,
          foregroundColor: Colors.white,
          elevation: 0,
          leading: IconButton(
            icon: const Icon(Icons.arrow_back),
            onPressed: () => context.go('/login'),
          ),
        ),
        body: SafeArea(
          child: SingleChildScrollView(
            child: Column(
              children: [
                // En-tête
                Padding(
                  padding:
                      const EdgeInsets.symmetric(horizontal: AppSpacing.l),
                  child: Column(
                    children: [
                      const Icon(Icons.shield_outlined,
                          color: Colors.white, size: 44),
                      const SizedBox(height: AppSpacing.s),
                      const Text(
                        'Vérification de sécurité',
                        style: TextStyle(
                          color: Colors.white,
                          fontSize: 22,
                          fontWeight: FontWeight.bold,
                        ),
                      ),
                      const SizedBox(height: 6),
                      Text(
                        'Saisissez le code à 6 chiffres envoyé à votre application d\'authentification.',
                        textAlign: TextAlign.center,
                        style: TextStyle(
                          color: Colors.white.withValues(alpha: 0.8),
                          fontSize: 13,
                          height: 1.4,
                        ),
                      ),
                    ],
                  ),
                ),
                const SizedBox(height: 24),

                // Panneau blanc avec contrainte minimale de hauteur
                Container(
                  constraints: BoxConstraints(
                    minHeight: screenHeight * 0.6,
                  ),
                  decoration: const BoxDecoration(
                    color: AppColors.background,
                    borderRadius: BorderRadius.only(
                      topLeft: Radius.circular(28),
                      topRight: Radius.circular(28),
                    ),
                  ),
                  padding: const EdgeInsets.all(AppSpacing.l),
                  child: Column(
                    children: [
                      const SizedBox(height: AppSpacing.s),
                      Text(
                        widget.email,
                        style: const TextStyle(
                          color: AppColors.primary,
                          fontWeight: FontWeight.w600,
                          fontSize: 14,
                        ),
                      ),
                      const SizedBox(height: AppSpacing.l),

                      // Grille OTP 6 cases fluides
                      Row(
                        mainAxisAlignment: MainAxisAlignment.center,
                        children: List.generate(6, (i) {
                          return Container(
                            width: 42,
                            height: 52,
                            margin:
                                const EdgeInsets.symmetric(horizontal: 3),
                            child: KeyboardListener(
                              focusNode: FocusNode(),
                              onKeyEvent: (event) => _handleKeyEvent(event, i),
                              child: TextField(
                                controller: _controllers[i],
                                focusNode: _focusNodes[i],
                                textAlign: TextAlign.center,
                                keyboardType: TextInputType.number,
                                inputFormatters: [
                                  FilteringTextInputFormatter.digitsOnly,
                                ],
                                style: const TextStyle(
                                  fontSize: 20,
                                  fontWeight: FontWeight.bold,
                                  color: AppColors.primary,
                                ),
                                decoration: InputDecoration(
                                  counterText: '',
                                  filled: true,
                                  fillColor: AppColors.surface,
                                  contentPadding: EdgeInsets.zero,
                                  border: OutlineInputBorder(
                                    borderRadius: BorderRadius.circular(10),
                                    borderSide: const BorderSide(
                                        color: AppColors.divider),
                                  ),
                                  focusedBorder: OutlineInputBorder(
                                    borderRadius: BorderRadius.circular(10),
                                    borderSide: const BorderSide(
                                      color: AppColors.primary,
                                      width: 2,
                                    ),
                                  ),
                                ),
                                onChanged: (value) => _onChanged(value, i),
                              ),
                            ),
                          );
                        }),
                      ),
                      const SizedBox(height: AppSpacing.xl),

                      BlocBuilder<AuthBloc, AuthState>(
                        builder: (context, state) => PrimaryButton(
                          label: 'Valider le code',
                          isLoading: state is AuthLoading,
                          icon: Icons.check_circle_outline,
                          onPressed: _submit,
                        ),
                      ),
                      const SizedBox(height: AppSpacing.l),
                    ],
                  ),
                ),
              ],
            ),
          ),
        ),
      ),
    );
  }
}

