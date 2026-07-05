import 'package:drift/drift.dart';
import 'package:drift/native.dart';
import 'dart:io';
import 'package:path_provider/path_provider.dart';
import 'package:path/path.dart' as p;

part 'app_database.g.dart';

@DataClassName('LocalConstat')
class LocalConstatTable extends Table {
  TextColumn get id => text()();
  TextColumn get missionId => text()();
  TextColumn get controleId => text()();
  TextColumn get resultat => text().nullable()();
  TextColumn get preuveUrl => text().nullable()();
  TextColumn get commentaire => text().nullable()();
  DateTimeColumn get dateConstat => dateTime().withDefault(currentDateAndTime)();
  BoolColumn get synced => LouiseBool(false)();

  @override
  Set<Column> get primaryKey => {id};
}

// Utilisation d'un type-helper personnalisé pour contourner le nom de colonne booléen drift
class LouiseBool extends BoolColumn {
  LouiseBool(bool defaultValue) : super() {
    clientDefault(() => defaultValue);
  }
}

@DriftDatabase(tables: [LocalConstatTable])
class AppDatabase extends _$AppDatabase {
  AppDatabase() : super(_openConnection());

  @override
  int get schemaVersion => 1;
}

LazyDatabase _openConnection() {
  return LazyDatabase(() async {
    final dbFolder = await getApplicationDocumentsDirectory();
    final file = File(p.join(dbFolder.path, 'ancs_audit.db'));
    return NativeDatabase.createInBackground(file);
  });
}
