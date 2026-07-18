import 'dart:io';

import 'package:drift/drift.dart';
import 'package:drift/native.dart';
import 'package:path/path.dart' as p;
import 'package:path_provider/path_provider.dart';

/// Ouvre une connexion SQLite native via Drift pour Android, iOS et Windows.
///
/// Utilise [NativeDatabase] avec un fichier physique dans le répertoire de
/// documents de l'application. sqlite3_flutter_libs fournit les binaires
/// SQLite — aucune installation système n'est requise.
QueryExecutor openConnection() {
  return LazyDatabase(() async {
    // Résoudre le répertoire persistant de l'application sur la plateforme cible.
    final dbFolder = await getApplicationDocumentsDirectory();
    final file = File(p.join(dbFolder.path, 'ancs_audit.sqlite'));
    return NativeDatabase(file);
  });
}
