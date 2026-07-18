import 'package:drift/drift.dart';
import 'package:drift/web.dart';

/// Ouvre une connexion IndexedDB via Drift pour Flutter Web.
///
/// [WebDatabase] utilise l'API IndexedDB du navigateur pour persister les
/// données entre les sessions. Le nom 'ancs_audit' est la clé de la base
/// IndexedDB dans le stockage du navigateur.
QueryExecutor openConnection() {
  return WebDatabase('ancs_audit');
}
