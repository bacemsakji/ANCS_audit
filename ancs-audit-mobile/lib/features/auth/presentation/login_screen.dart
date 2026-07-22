import 'package:flutter/material.dart';
import 'package:flutter_bloc/flutter_bloc.dart';
import 'package:go_router/go_router.dart';
import '../bloc/auth_bloc.dart';
import '../bloc/auth_event.dart';
import '../bloc/auth_state.dart';
import '../../../core/constants/app_colors.dart';
import '../../../core/constants/app_spacing.dart';
import '../../../core/widgets/app_buttons.dart';
import '../../../core/widgets/app_text_field.dart';
import '../../../../l10n/app_localizations.dart';

class LoginScreen extends StatefulWidget {
  const LoginScreen({Key? key}) : super(key: key);

  @override
  State<LoginScreen> createState() => _LoginScreenState();
}

class _LoginScreenState extends State<LoginScreen> {
  final _formKey = GlobalKey<FormState>();
  final _emailController = TextEditingController();
  final _passwordController = TextEditingController();
  bool _obscurePassword = true;

  @override
  void dispose() {
    _emailController.dispose();
    _passwordController.dispose();
    super.dispose();
  }

  void _submit() {
    if (!_formKey.currentState!.validate()) return;
    context.read<AuthBloc>().add(LoginSubmitted(
      email: _emailController.text.trim(),
      password: _passwordController.text,
    ));
  }

  @override
  Widget build(BuildContext context) {
    return BlocListener<AuthBloc, AuthState>(
      listener: (context, state) {
        if (state is AuthAuthenticated) {
          context.go('/dashboard');
        } else if (state is AuthMfaRequired) {
          context.go('/login/2fa', extra: {
            'email': state.email,
            'mfaToken': state.mfaToken,
          });
        } else if (state is AuthError) {
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
        body: SafeArea(
          child: SingleChildScrollView(
            child: Column(
              children: [
                // En-tête institutionnel
                const SizedBox(height: 48),
                Padding(
                  padding: const EdgeInsets.symmetric(horizontal: AppSpacing.l),
                  child: Column(
                    children: [
                      Image.asset(
                        'assets/images/logo--ancs.png',
                        width: 72,
                        height: 72,
                      ),
                      const SizedBox(height: AppSpacing.m),
                      const Text(
                        'ANCS',
                        style: TextStyle(
                          color: Colors.white,
                          fontSize: 28,
                          fontWeight: FontWeight.bold,
                          letterSpacing: 4,
                          fontFamily: 'Inter',
                        ),
                      ),
                      const SizedBox(height: 4),
                      Text(
                        AppLocalizations.of(context)!.loginAgency,
                        style: TextStyle(
                          color: Colors.white.withValues(alpha: 0.85),
                          fontSize: 13,
                          letterSpacing: 0.5,
                        ),
                      ),
                      const SizedBox(height: 4),
                        Container(
                          padding: const EdgeInsets.symmetric(
                            horizontal: AppSpacing.s,
                            vertical: 2,
                          ),
                          decoration: BoxDecoration(
                            color: Colors.white.withValues(alpha: 0.12),
                            borderRadius: BorderRadius.circular(4),
                          ),
                          child: Text(
                            AppLocalizations.of(context)!.loginTitle,
                            style: TextStyle(
                              color: Colors.white.withValues(alpha: 0.75),
                              fontSize: 11,
                            ),
                          ),
                        ),
                    ],
                  ),
                ),
                const SizedBox(height: 36),

                // Panneau de formulaire sur fond blanc arrondi
                Container(
                  constraints: BoxConstraints(
                    minHeight: MediaQuery.of(context).size.height * 0.55,
                  ),
                  decoration: const BoxDecoration(
                    color: AppColors.background,
                    borderRadius: BorderRadiusDirectional.only(
                      topStart: Radius.circular(28),
                      topEnd: Radius.circular(28),
                    ),
                  ),
                  padding: const EdgeInsets.all(AppSpacing.l),
                  child: Form(
                    key: _formKey,
                    child: Column(
                      crossAxisAlignment: CrossAxisAlignment.start,
                      children: [
                        Text(
                          AppLocalizations.of(context)!.login,
                          style: Theme.of(context).textTheme.titleLarge,
                        ),
                        const SizedBox(height: 4),
                        Text(
                          AppLocalizations.of(context)!.loginSubtitle,
                          style: Theme.of(context).textTheme.bodySmall,
                        ),
                        const SizedBox(height: AppSpacing.l),

                        AppTextField(
                          controller: _emailController,
                          label: AppLocalizations.of(context)!.emailLabel,
                          keyboardType: TextInputType.emailAddress,
                          prefixIcon: const Icon(Icons.email_outlined, size: 20),
                          validator: (v) {
                            if (v == null || v.isEmpty) return AppLocalizations.of(context)!.requiredField;
                            if (!v.contains('@')) return AppLocalizations.of(context)!.invalidEmail;
                            return null;
                          },
                        ),
                        const SizedBox(height: AppSpacing.m),

                        AppTextField(
                          controller: _passwordController,
                          label: AppLocalizations.of(context)!.passwordLabel,
                          obscureText: _obscurePassword,
                          prefixIcon: const Icon(Icons.lock_outline, size: 20),
                          suffixIcon: IconButton(
                            icon: Icon(
                              _obscurePassword
                                  ? Icons.visibility_off_outlined
                                  : Icons.visibility_outlined,
                              size: 20,
                            ),
                            onPressed: () =>
                                setState(() => _obscurePassword = !_obscurePassword),
                          ),
                          validator: (v) {
                            if (v == null || v.isEmpty) return AppLocalizations.of(context)!.requiredField;
                            return null;
                          },
                        ),
                        const SizedBox(height: AppSpacing.l),

                        BlocBuilder<AuthBloc, AuthState>(
                          builder: (context, state) => PrimaryButton(
                            label: AppLocalizations.of(context)!.loginButton,
                            isLoading: state is AuthLoading,
                            onPressed: _submit,
                          ),
                        ),
                        const SizedBox(height: AppSpacing.l),

                        // Note de sécurité
                        Container(
                          padding: const EdgeInsets.all(AppSpacing.m),
                          decoration: BoxDecoration(
                            color: AppColors.primary.withValues(alpha: 0.05),
                            borderRadius: BorderRadius.circular(AppSpacing.borderRadiusM),
                            border: Border.all(color: AppColors.primary.withValues(alpha: 0.15)),
                          ),
                          child: Row(
                            crossAxisAlignment: CrossAxisAlignment.start,
                            children: [
                              const Icon(
                                Icons.info_outline,
                                size: 16,
                                color: AppColors.primary,
                              ),
                              const SizedBox(width: AppSpacing.s),
                              Expanded(
                                child: Text(
                                  AppLocalizations.of(context)!.mfaNotice,
                                  style: Theme.of(context).textTheme.bodySmall,
                                ),
                              ),
                            ],
                          ),
                        ),
                      ],
                    ),
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
