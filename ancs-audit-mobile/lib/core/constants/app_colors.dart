import 'package:flutter/material.dart';

class AppColors {
  // Palette institutionnelle ANCS
  static const Color primary = Color(0xFF1A3A5C);       // Bleu marine principal
  static const Color primaryLight = Color(0xFF2D5F8A);  // Éléments actifs
  static const Color accent = Color(0xFF2E86AB);        // Bleu ciel d'accent
  
  static const Color background = Color(0xFFF4F6F9);    // Fond général
  static const Color surface = Color(0xFFFFFFFF);       // Fond des cartes
  
  // Texte
  static const Color textPrimary = Color(0xFF1C2B3A);
  static const Color textSecondary = Color(0xFF5A6A7A);
  static const Color divider = Color(0xFFDDE3EA);

  // Indicateurs de conformité sobres
  static const Color conforme = Color(0xFF2E7D32);      // Vert
  static const Color nonConforme = Color(0xFFC62828);   // Rouge
  static const Color observation = Color(0xFFE65100);   // Orange

  // Priorités actions correctives
  static const Color prioriteCritique = Color(0xFFB71C1C);
  static const Color prioriteHaute = Color(0xFFE65100);
  static const Color prioriteMoyenne = Color(0xFFF9A825);
  static const Color prioriteFaible = Color(0xFF388E3C);

  // Fond translucide pour les badges
  static Color conformeBg = conforme.withOpacity(0.1);
  static Color nonConformeBg = nonConforme.withOpacity(0.1);
  static Color observationBg = observation.withOpacity(0.1);
}
