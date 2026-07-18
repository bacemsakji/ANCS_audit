import 'package:drift/drift.dart';

import 'connection_helper.dart';

part 'app_database.g.dart';

/// Table des constats enregistrés localement lorsque l'auditeur est hors-ligne.
///
/// Un constat est stocké ici dès que [synced] == false, et marqué [synced] == true
/// par [SyncService.triggerSync()] après un téléversement réussi vers le backend.
@DataClassName('LocalConstat')
class LocalConstatTable extends Table {
  TextColumn get id => text()();
  TextColumn get missionId => text()();
  TextColumn get controleId => text()();
  TextColumn get resultat => text().nullable()();
  /// Chemin local du fichier preuve avant synchronisation, URL serveur MinIO après.
  TextColumn get preuveUrl => text().nullable()();
  TextColumn get commentaire => text().nullable()();
  DateTimeColumn get dateConstat => dateTime().withDefault(currentDateAndTime)();
  BoolColumn get synced => boolean().withDefault(const Constant(false))();
  TextColumn get criticite => text().nullable()();
  TextColumn get preuveDescription => text().nullable()();
  TextColumn get recommandation => text().nullable()();
  TextColumn get composantesImpactees => text().nullable()();

  @override
  Set<Column> get primaryKey => {id};
}

/// Base de données locale Drift.
///
/// La connexion physique est sélectionnée au moment de la compilation :
///   - Android / iOS / Windows → SQLite (NativeDatabase) via connection_helper_native.dart
///   - Flutter Web             → IndexedDB (WebDatabase) via connection_helper_web.dart
///
/// Le sélecteur est [connection_helper.dart] via export conditionnel Dart.
@DriftDatabase(tables: [LocalConstatTable])
class AppDatabase extends _$AppDatabase {
  AppDatabase() : super(openConnection());

  @override
  int get schemaVersion => 2;

  @override
  MigrationStrategy get migration => MigrationStrategy(
        onCreate: (m) async {
          await m.createAll();
        },
        onUpgrade: (m, from, to) async {
          if (from < 2) {
            await m.addColumn(localConstatTable, localConstatTable.criticite);
            await m.addColumn(localConstatTable, localConstatTable.preuveDescription);
            await m.addColumn(localConstatTable, localConstatTable.recommandation);
            await m.addColumn(localConstatTable, localConstatTable.composantesImpactees);
          }
        },
      );
}
