import 'package:flutter/material.dart';
import '../constants/app_colors.dart';
import '../constants/app_spacing.dart';

class StatusBadge extends StatelessWidget {
  final String status;
  final bool isArabic;

  const StatusBadge({
    Key? key,
    required this.status,
    this.isArabic = false,
  }) : super(key: key);

  @override
  Widget build(BuildContext context) {
    Color color;
    Color bgColor;
    String label;

    switch (status.toUpperCase()) {
      case 'CONFORME':
        color = AppColors.conforme;
        bgColor = AppColors.conformeBg;
        label = isArabic ? 'مطابق' : 'Conforme';
        break;
      case 'NON_CONFORME':
        color = AppColors.nonConforme;
        bgColor = AppColors.nonConformeBg;
        label = isArabic ? 'غير مطابق' : 'Non conforme';
        break;
      case 'OBSERVATION':
        color = AppColors.observation;
        bgColor = AppColors.observationBg;
        label = isArabic ? 'ملاحظة' : 'Observation';
        break;
      default:
        color = AppColors.textSecondary;
        bgColor = AppColors.divider;
        label = status;
    }

    return Container(
      padding: const EdgeInsets.symmetric(
          horizontal: AppSpacing.s, vertical: AppSpacing.xs),
      decoration: BoxDecoration(
        color: bgColor,
        borderRadius: BorderRadius.circular(AppSpacing.borderRadiusS),
        border: Border.all(color: color.withValues(alpha: 0.3), width: 1),
      ),
      child: Text(
        label,
        style: TextStyle(
          color: color,
          fontSize: 12,
          fontWeight: FontWeight.w600,
        ),
      ),
    );
  }
}
