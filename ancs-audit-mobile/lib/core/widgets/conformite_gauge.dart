import 'package:flutter/material.dart';
import '../constants/app_colors.dart';
import '../constants/app_spacing.dart';

/// Widget affichant la jauge de conformité (taux en %) avec un dégradé de couleur.
class ConformiteGauge extends StatelessWidget {
  final double tauxConformite;
  final bool isArabic;

  const ConformiteGauge({
    Key? key,
    required this.tauxConformite,
    this.isArabic = false,
  }) : super(key: key);

  Color _getColor() {
    if (tauxConformite >= 80) return AppColors.conforme;
    if (tauxConformite >= 50) return AppColors.observation;
    return AppColors.nonConforme;
  }

  @override
  Widget build(BuildContext context) {
    final color = _getColor();
    final label = isArabic ? 'معدل الامتثال' : 'Taux de conformité';

    return Column(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: [
        Row(
          mainAxisAlignment: MainAxisAlignment.spaceBetween,
          children: [
            Text(
              label,
              style: Theme.of(context).textTheme.bodySmall?.copyWith(
                fontWeight: FontWeight.w500,
              ),
            ),
            Text(
              '${tauxConformite.toStringAsFixed(1)}%',
              style: TextStyle(
                fontSize: 16,
                fontWeight: FontWeight.bold,
                color: color,
              ),
            ),
          ],
        ),
        const SizedBox(height: AppSpacing.s),
        ClipRRect(
          borderRadius: BorderRadius.circular(4),
          child: LinearProgressIndicator(
            value: tauxConformite / 100,
            backgroundColor: AppColors.divider,
            valueColor: AlwaysStoppedAnimation<Color>(color),
            minHeight: 8,
          ),
        ),
      ],
    );
  }
}
