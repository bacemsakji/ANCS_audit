import 'package:flutter/material.dart';
import 'app_colors.dart';

class AppTypography {
  static const String fontLatin = 'Inter';
  static const String fontArabic = 'NotoSansArabic';

  static TextStyle getStyle({
    required double fontSize,
    required FontWeight fontWeight,
    required Color color,
    required bool isArabic,
  }) {
    return TextStyle(
      fontFamily: isArabic ? fontArabic : fontLatin,
      fontSize: fontSize,
      fontWeight: fontWeight,
      color: color,
      height: isArabic ? 1.4 : 1.2,
    );
  }

  static TextStyle heading1({required bool isArabic, Color color = AppColors.textPrimary}) =>
      getStyle(fontSize: 24, fontWeight: FontWeight.bold, color: color, isArabic: isArabic);

  static TextStyle heading2({required bool isArabic, Color color = AppColors.textPrimary}) =>
      getStyle(fontSize: 18, fontWeight: FontWeight.bold, color: color, isArabic: isArabic);

  static TextStyle heading3({required bool isArabic, Color color = AppColors.textPrimary}) =>
      getStyle(fontSize: 16, fontWeight: FontWeight.w600, color: color, isArabic: isArabic);

  static TextStyle bodyLarge({required bool isArabic, Color color = AppColors.textPrimary}) =>
      getStyle(fontSize: 14, fontWeight: FontWeight.normal, color: color, isArabic: isArabic);

  static TextStyle bodySmall({required bool isArabic, Color color = AppColors.textSecondary}) =>
      getStyle(fontSize: 12, fontWeight: FontWeight.normal, color: color, isArabic: isArabic);

  static TextStyle label({required bool isArabic, Color color = AppColors.textPrimary}) =>
      getStyle(fontSize: 12, fontWeight: FontWeight.w500, color: color, isArabic: isArabic);
}
