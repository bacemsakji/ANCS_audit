/// Point d'entrée unique pour ouvrir la connexion à la base de données Drift.
///
/// L'export conditionnel sélectionne automatiquement le backend approprié :
///   - `dart.library.io`   → Android / iOS / Windows → NativeDatabase (SQLite)
///   - `dart.library.html` → Flutter Web             → WebDatabase (IndexedDB)
///
/// Aucun code applicatif n'a besoin de connaître la plateforme cible —
/// il lui suffit d'importer ce fichier et d'appeler [openConnection()].
export 'connection_helper_native.dart'
    if (dart.library.html) 'connection_helper_web.dart';
