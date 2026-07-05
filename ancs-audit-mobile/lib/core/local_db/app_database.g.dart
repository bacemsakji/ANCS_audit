// GENERATED CODE - DO NOT MODIFY BY HAND

part of 'app_database.dart';

// ignore_for_file: type=lint
class $LocalConstatTableTable extends LocalConstatTable
    with TableInfo<$LocalConstatTableTable, LocalConstat> {
  @override
  final GeneratedDatabase attachedDatabase;
  final String? _alias;
  $LocalConstatTableTable(this.attachedDatabase, [this._alias]);
  static const VerificationMeta _idMeta = const VerificationMeta('id');
  @override
  late final GeneratedColumn<String> id = GeneratedColumn<String>(
      'id', aliasedName, false,
      type: DriftSqlType.string, requiredDuringInsert: true);
  static const VerificationMeta _missionIdMeta =
      const VerificationMeta('missionId');
  @override
  late final GeneratedColumn<String> missionId = GeneratedColumn<String>(
      'mission_id', aliasedName, false,
      type: DriftSqlType.string, requiredDuringInsert: true);
  static const VerificationMeta _controleIdMeta =
      const VerificationMeta('controleId');
  @override
  late final GeneratedColumn<String> controleId = GeneratedColumn<String>(
      'controle_id', aliasedName, false,
      type: DriftSqlType.string, requiredDuringInsert: true);
  static const VerificationMeta _resultatMeta =
      const VerificationMeta('resultat');
  @override
  late final GeneratedColumn<String> resultat = GeneratedColumn<String>(
      'resultat', aliasedName, true,
      type: DriftSqlType.string, requiredDuringInsert: false);
  static const VerificationMeta _preuveUrlMeta =
      const VerificationMeta('preuveUrl');
  @override
  late final GeneratedColumn<String> preuveUrl = GeneratedColumn<String>(
      'preuve_url', aliasedName, true,
      type: DriftSqlType.string, requiredDuringInsert: false);
  static const VerificationMeta _commentaireMeta =
      const VerificationMeta('commentaire');
  @override
  late final GeneratedColumn<String> commentaire = GeneratedColumn<String>(
      'commentaire', aliasedName, true,
      type: DriftSqlType.string, requiredDuringInsert: false);
  static const VerificationMeta _dateConstatMeta =
      const VerificationMeta('dateConstat');
  @override
  late final GeneratedColumn<DateTime> dateConstat = GeneratedColumn<DateTime>(
      'date_constat', aliasedName, false,
      type: DriftSqlType.dateTime,
      requiredDuringInsert: false,
      defaultValue: currentDateAndTime);
  static const VerificationMeta _syncedMeta = const VerificationMeta('synced');
  @override
  late final GeneratedColumn<bool> synced = GeneratedColumn<bool>(
      'synced', aliasedName, false,
      type: DriftSqlType.bool,
      requiredDuringInsert: false,
      defaultValue: const Constant(false));
  @override
  List<GeneratedColumn> get $columns =>
      [id, missionId, controleId, resultat, preuveUrl, commentaire, dateConstat, synced];
  @override
  String get aliasedName => _alias ?? actualTableName;
  @override
  String get actualTableName => $name;
  static const String $name = 'local_constat_table';
  @override
  VerificationContext validateIntegrity(Insertable<LocalConstat> instance,
      {bool isInserting = false}) {
    final context = VerificationContext();
    final data = instance.toColumns(true);
    if (data.containsKey('id')) {
      context.handle(_idMeta, id.isAcceptableOrUnknown(data['id']!, _idMeta));
    } else if (isInserting) {
      context.missing(_idMeta);
    }
    if (data.containsKey('mission_id')) {
      context.handle(_missionIdMeta,
          missionId.isAcceptableOrUnknown(data['mission_id']!, _missionIdMeta));
    } else if (isInserting) {
      context.missing(_missionIdMeta);
    }
    if (data.containsKey('controle_id')) {
      context.handle(_controleIdMeta,
          controleId.isAcceptableOrUnknown(data['controle_id']!, _controleIdMeta));
    } else if (isInserting) {
      context.missing(_controleIdMeta);
    }
    if (data.containsKey('resultat')) {
      context.handle(_resultatMeta,
          resultat.isAcceptableOrUnknown(data['resultat']!, _resultatMeta));
    }
    if (data.containsKey('preuve_url')) {
      context.handle(_preuveUrlMeta,
          preuveUrl.isAcceptableOrUnknown(data['preuve_url']!, _preuveUrlMeta));
    }
    if (data.containsKey('commentaire')) {
      context.handle(_commentaireMeta,
          commentaire.isAcceptableOrUnknown(data['commentaire']!, _commentaireMeta));
    }
    if (data.containsKey('date_constat')) {
      context.handle(
          _dateConstatMeta,
          dateConstat.isAcceptableOrUnknown(
              data['date_constat']!, _dateConstatMeta));
    }
    if (data.containsKey('synced')) {
      context.handle(_syncedMeta,
          synced.isAcceptableOrUnknown(data['synced']!, _syncedMeta));
    }
    return context;
  }

  @override
  Set<GeneratedColumn> get $primaryKey => {id};
  @override
  LocalConstat map(Map<String, dynamic> data, {String? tablePrefix}) {
    final db = attachedDatabase.typeMapping;
    return LocalConstat(
      id: db.read(DriftSqlType.string, data[tablePrefix ?? 'id'])!,
      missionId: db.read(DriftSqlType.string, data[tablePrefix ?? 'mission_id'])!,
      controleId: db.read(DriftSqlType.string, data[tablePrefix ?? 'controle_id'])!,
      resultat: db.read(DriftSqlType.string, data[tablePrefix ?? 'resultat']),
      preuveUrl: db.read(DriftSqlType.string, data[tablePrefix ?? 'preuve_url']),
      commentaire: db.read(DriftSqlType.string, data[tablePrefix ?? 'commentaire']),
      dateConstat: db.read(DriftSqlType.dateTime, data[tablePrefix ?? 'date_constat'])!,
      synced: db.read(DriftSqlType.bool, data[tablePrefix ?? 'synced'])!,
    );
  }

  @override
  $LocalConstatTableTable createAlias(String alias) {
    return $LocalConstatTableTable(attachedDatabase, alias);
  }
}

class LocalConstat extends DataClass implements Insertable<LocalConstat> {
  final String id;
  final String missionId;
  final String controleId;
  final String? resultat;
  final String? preuveUrl;
  final String? commentaire;
  final DateTime dateConstat;
  final bool synced;
  const LocalConstat(
      {required this.id,
      required this.missionId,
      required this.controleId,
      this.resultat,
      this.preuveUrl,
      this.commentaire,
      required this.dateConstat,
      required this.synced});
  @override
  Map<String, Expression> toColumns(bool nullToAbsent) {
    final map = <String, Expression>{};
    map['id'] = Variable<String>(id);
    map['mission_id'] = Variable<String>(missionId);
    map['controle_id'] = Variable<String>(controleId);
    if (!nullToAbsent || resultat != null) {
      map['resultat'] = Variable<String>(resultat);
    }
    if (!nullToAbsent || preuveUrl != null) {
      map['preuve_url'] = Variable<String>(preuveUrl);
    }
    if (!nullToAbsent || commentaire != null) {
      map['commentaire'] = Variable<String>(commentaire);
    }
    map['date_constat'] = Variable<DateTime>(dateConstat);
    map['synced'] = Variable<bool>(synced);
    return map;
  }

  LocalConstatTableCompanion toCompanion(bool nullToAbsent) {
    return LocalConstatTableCompanion(
      id: Value(id),
      missionId: Value(missionId),
      controleId: Value(controleId),
      resultat: resultat == null && nullToAbsent ? const Value.absent() : Value(resultat),
      preuveUrl: preuveUrl == null && nullToAbsent ? const Value.absent() : Value(preuveUrl),
      commentaire: commentaire == null && nullToAbsent ? const Value.absent() : Value(commentaire),
      dateConstat: Value(dateConstat),
      synced: Value(synced),
    );
  }

  factory LocalConstat.fromJson(Map<String, dynamic> json,
      {ValueSerializer? serializer}) {
    serializer ??= driftRuntimeOptions.defaultSerializer;
    return LocalConstat(
      id: serializer.fromJson<String>(json['id']),
      missionId: serializer.fromJson<String>(json['missionId']),
      controleId: serializer.fromJson<String>(json['controleId']),
      resultat: serializer.fromJson<String?>(json['resultat']),
      preuveUrl: serializer.fromJson<String?>(json['preuveUrl']),
      commentaire: serializer.fromJson<String?>(json['commentaire']),
      dateConstat: serializer.fromJson<DateTime>(json['dateConstat']),
      synced: serializer.fromJson<bool>(json['synced']),
    );
  }
  @override
  Map<String, dynamic> toJson({ValueSerializer? serializer}) {
    serializer ??= driftRuntimeOptions.defaultSerializer;
    return <String, dynamic>{
      'id': serializer.toJson<String>(id),
      'missionId': serializer.toJson<String>(missionId),
      'controleId': serializer.toJson<String>(controleId),
      'resultat': serializer.toJson<String?>(resultat),
      'preuveUrl': serializer.toJson<String?>(preuveUrl),
      'commentaire': serializer.toJson<String?>(commentaire),
      'dateConstat': serializer.toJson<DateTime>(dateConstat),
      'synced': serializer.toJson<bool>(synced),
    };
  }

  LocalConstat copyWith(
          {String? id,
          String? missionId,
          String? controleId,
          String? resultat,
          String? preuveUrl,
          String? commentaire,
          DateTime? dateConstat,
          bool? synced}) =>
      LocalConstat(
        id: id ?? this.id,
        missionId: missionId ?? this.missionId,
        controleId: controleId ?? this.controleId,
        resultat: resultat ?? this.resultat,
        preuveUrl: preuveUrl ?? this.preuveUrl,
        commentaire: commentaire ?? this.commentaire,
        dateConstat: dateConstat ?? this.dateConstat,
        synced: synced ?? this.synced,
      );
  @override
  String toString() {
    return (StringBuffer('LocalConstat(')
          ..write('id: $id, ')
          ..write('missionId: $missionId, ')
          ..write('controleId: $controleId, ')
          ..write('resultat: $resultat, ')
          ..write('preuveUrl: $preuveUrl, ')
          ..write('commentaire: $commentaire, ')
          ..write('dateConstat: $dateConstat, ')
          ..write('synced: $synced')
          ..write(')'))
        .toString();
  }

  @override
  int get hashCode => Object.hash(
      id, missionId, controleId, resultat, preuveUrl, commentaire, dateConstat, synced);
  @override
  bool operator ==(Object other) =>
      identical(this, other) ||
      (other is LocalConstat &&
          other.id == this.id &&
          other.missionId == this.missionId &&
          other.controleId == this.controleId &&
          other.resultat == this.resultat &&
          other.preuveUrl == this.preuveUrl &&
          other.commentaire == this.commentaire &&
          other.dateConstat == this.dateConstat &&
          other.synced == this.synced);
}

class LocalConstatTableCompanion extends UpdateCompanion<LocalConstat> {
  final Value<String> id;
  final Value<String> missionId;
  final Value<String> controleId;
  final Value<String?> resultat;
  final Value<String?> preuveUrl;
  final Value<String?> commentaire;
  final Value<DateTime> dateConstat;
  final Value<bool> synced;
  const LocalConstatTableCompanion({
    this.id = const Value.absent(),
    this.missionId = const Value.absent(),
    this.controleId = const Value.absent(),
    this.resultat = const Value.absent(),
    this.preuveUrl = const Value.absent(),
    this.commentaire = const Value.absent(),
    this.dateConstat = const Value.absent(),
    this.synced = const Value.absent(),
  });
  LocalConstatTableCompanion.insert({
    required String id,
    required String missionId,
    required String controleId,
    this.resultat = const Value.absent(),
    this.preuveUrl = const Value.absent(),
    this.commentaire = const Value.absent(),
    this.dateConstat = const Value.absent(),
    this.synced = const Value.absent(),
  })  : id = Value(id),
        missionId = Value(missionId),
        controleId = Value(controleId);
  static Insertable<LocalConstat> custom({
    Expression<String>? id,
    Expression<String>? missionId,
    Expression<String>? controleId,
    Expression<String>? resultat,
    Expression<String>? preuveUrl,
    Expression<String>? commentaire,
    Expression<DateTime>? dateConstat,
    Expression<bool>? synced,
  }) {
    return RawValuesInsertable({
      if (id != null) 'id': id,
      if (missionId != null) 'mission_id': missionId,
      if (controleId != null) 'controle_id': controleId,
      if (resultat != null) 'resultat': resultat,
      if (preuveUrl != null) 'preuve_url': preuveUrl,
      if (commentaire != null) 'commentaire': commentaire,
      if (dateConstat != null) 'date_constat': dateConstat,
      if (synced != null) 'synced': synced,
    });
  }

  LocalConstatTableCompanion copyWith(
      {Value<String>? id,
      Value<String>? missionId,
      Value<String>? controleId,
      Value<String?>? resultat,
      Value<String?>? preuveUrl,
      Value<String?>? commentaire,
      Value<DateTime>? dateConstat,
      Value<bool>? synced}) {
    return LocalConstatTableCompanion(
      id: id ?? this.id,
      missionId: missionId ?? this.missionId,
      controleId: controleId ?? this.controleId,
      resultat: resultat ?? this.resultat,
      preuveUrl: preuveUrl ?? this.preuveUrl,
      commentaire: commentaire ?? this.commentaire,
      dateConstat: dateConstat ?? this.dateConstat,
      synced: synced ?? this.synced,
    );
  }

  @override
  Map<String, Expression> toColumns(bool nullToAbsent) {
    final map = <String, Expression>{};
    if (id.present) {
      map['id'] = Variable<String>(id.value);
    }
    if (missionId.present) {
      map['mission_id'] = Variable<String>(missionId.value);
    }
    if (controleId.present) {
      map['controle_id'] = Variable<String>(controleId.value);
    }
    if (resultat.present) {
      map['resultat'] = Variable<String>(resultat.value);
    }
    if (preuveUrl.present) {
      map['preuve_url'] = Variable<String>(preuveUrl.value);
    }
    if (commentaire.present) {
      map['commentaire'] = Variable<String>(commentaire.value);
    }
    if (dateConstat.present) {
      map['date_constat'] = Variable<DateTime>(dateConstat.value);
    }
    if (synced.present) {
      map['synced'] = Variable<bool>(synced.value);
    }
    return map;
  }

  @override
  String toString() {
    return (StringBuffer('LocalConstatTableCompanion(')
          ..write('id: $id, ')
          ..write('missionId: $missionId, ')
          ..write('controleId: $controleId, ')
          ..write('resultat: $resultat, ')
          ..write('preuveUrl: $preuveUrl, ')
          ..write('commentaire: $commentaire, ')
          ..write('dateConstat: $dateConstat, ')
          ..write('synced: $synced')
          ..write(')'))
        .toString();
  }
}

abstract class _$AppDatabase extends GeneratedDatabase {
  _$AppDatabase(QueryExecutor e) : super(e);
  late final $LocalConstatTableTable localConstatTable =
      $LocalConstatTableTable(this);
  @override
  Iterable<TableInfo<Table, Object?>> get allTables =>
      allSchemaEntities.whereType<TableInfo<Table, Object?>>();
  @override
  List<DatabaseSchemaEntity> get allSchemaEntities => [localConstatTable];
}
