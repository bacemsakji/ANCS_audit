import 'package:flutter/material.dart';
import '../constants/app_colors.dart';
import '../constants/app_spacing.dart';

/// Bannière de statut connectivité affichée en haut de l'écran
/// quand l'appareil passe en mode hors-ligne.
class ConnectivityBanner extends StatelessWidget {
  final bool isOnline;

  const ConnectivityBanner({Key? key, required this.isOnline})
      : super(key: key);

  @override
  Widget build(BuildContext context) {
    if (isOnline) return const SizedBox.shrink();

    return Container(
      width: double.infinity,
      color: AppColors.offlineWarning,
      padding: const EdgeInsets.symmetric(
        horizontal: AppSpacing.m,
        vertical: AppSpacing.xs,
      ),
      child: Row(
        mainAxisAlignment: MainAxisAlignment.center,
        children: [
          const Icon(Icons.wifi_off, color: Colors.white, size: 14),
          const SizedBox(width: AppSpacing.xs),
          Text(
            'Mode hors-ligne — données sauvegardées localement',
            style: Theme.of(context).textTheme.bodySmall?.copyWith(
                  color: Colors.white,
                  fontWeight: FontWeight.w500,
                ),
          ),
        ],
      ),
    );
  }
}
