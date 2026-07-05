import 'package:flutter/material.dart';
import '../constants/app_colors.dart';
import '../constants/app_typography.dart';

class AppTheme {
  static ThemeData buildTheme(bool isArabic) {
    final baseTextStyle = TextStyle(
      fontFamily: isArabic ? AppTypography.fontArabic : AppTypography.fontLatin,
    );

    return ThemeData(
      useMaterial3: true,
      colorScheme: ColorScheme.fromSeed(
        seedColor: AppColors.primary,
        primary: AppColors.primary,
        secondary: AppColors.accent,
        background: AppColors.background,
      ),
      scaffoldBackgroundColor: AppColors.background,
      appBarTheme: const AppBarTheme(
        backgroundColor: AppColors.primary,
        foregroundColor: Colors.white,
        elevation: 0,
        centerTitle: true,
      ),
      cardTheme: CardTheme(
        color: AppColors.surface,
        elevation: 2,
        shadowColor: Colors.black.withOpacity(0.06),
        shape: RoundedRectangleBorder(
          borderRadius: BorderRadius.circular(12),
        ),
      ),
      dividerTheme: const DividerThemeData(
        color: AppColors.divider,
        thickness: 1,
      ),
      textTheme: TextTheme(
        titleLarge: baseTextStyle.copyWith(fontSize: 20, fontWeight: FontWeight.bold, color: AppColors.textPrimary),
        titleMedium: baseTextStyle.copyWith(fontSize: 16, fontWeight: FontWeight.w600, color: AppColors.textPrimary),
        bodyLarge: baseTextStyle.copyWith(fontSize: 14, color: AppColors.textPrimary),
        bodySmall: baseTextStyle.copyWith(fontSize: 12, color: AppColors.textSecondary),
      ),
    );
  }
}
