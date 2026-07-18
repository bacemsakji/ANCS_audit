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
      defaultConstraints:
          GeneratedColumn.constraintIsAlways('CHECK ("synced" IN (0, 1))'),
      defaultValue: const Constant(false));
  static const VerificationMeta _criticiteMeta =
      const VerificationMeta('criticite');
  @override
  late final GeneratedColumn<String> criticite = GeneratedColumn<String>(
      'criticite', aliasedName, true,
      type: DriftSqlType.string, requiredDuringInsert: false);
  static const VerificationMeta _preuveDescriptionMeta =
      const VerificationMeta('preuveDescription');
  @override
  late final GeneratedColumn<String> preuveDescription =
      GeneratedColumn<String>('preuve_description', aliasedName, true,
          type: DriftSqlType.string, requiredDuringInsert: false);
  static const VerificationMeta _recommandationMeta =
      const VerificationMeta('recommandation');
  @override
  late final GeneratedColumn<String> recommandation = GeneratedColumn<String>(
      'recommandation', aliasedName, true,
      type: DriftSqlType.string, requiredDuringInsert: false);
  static const VerificationMeta _composantesImpacteesMeta =
      const VerificationMeta('composantesImpactees');
  @override
  late final GeneratedColumn<String> composantesImpactees =
      GeneratedColumn<String>('composantes_impactees', aliasedName, true,
          type: DriftSqlType.string, requiredDuringInsert: false);
  @override
  List<GeneratedColumn> get $columns => [
        id,
        missionId,
        controleId,
        resultat,
        preuveUrl,
        commentaire,
        dateConstat,
        synced,
        criticite,
        preuveDescription,
        recommandation,
        composantesImpactees
      ];
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
      context.handle(
          _controleIdMeta,
          controleId.isAcceptableOrUnknown(
              data['controle_id']!, _controleIdMeta));
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
      context.handle(
          _commentaireMeta,
          commentaire.isAcceptableOrUnknown(
              data['commentaire']!, _commentaireMeta));
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
    if (data.containsKey('criticite')) {
      context.handle(_criticiteMeta,
          criticite.isAcceptableOrUnknown(data['criticite']!, _criticiteMeta));
    }
    if (data.containsKey('preuve_description')) {
      context.handle(
          _preuveDescriptionMeta,
          preuveDescription.isAcceptableOrUnknown(
              data['preuve_description']!, _preuveDescriptionMeta));
    }
    if (data.containsKey('recommandation')) {
      context.handle(
          _recommandationMeta,
          recommandation.isAcceptableOrUnknown(
              data['recommandation']!, _recommandationMeta));
    }
    if (data.containsKey('composantes_impactees')) {
      context.handle(
          _composantesImpacteesMeta,
          composantesImpactees.isAcceptableOrUnknown(
              data['composantes_impactees']!, _composantesImpacteesMeta));
    }
    return context;
  }

  @override
  Set<GeneratedColumn> get $primaryKey => {id};
  @override
  LocalConstat map(Map<String, dynamic> data, {String? tablePrefix}) {
    final effectivePrefix = tablePrefix != null ? '$tablePrefix.' : '';
    return LocalConstat(
      id: attachedDatabase.typeMapping
          .read(DriftSqlType.string, data['${effectivePrefix}id'])!,
      missionId: attachedDatabase.typeMapping
          .read(DriftSqlType.string, data['${effectivePrefix}mission_id'])!,
      controleId: attachedDatabase.typeMapping
          .read(DriftSqlType.string, data['${effectivePrefix}controle_id'])!,
      resultat: attachedDatabase.typeMapping
          .read(DriftSqlType.string, data['${effectivePrefix}resultat']),
      preuveUrl: attachedDatabase.typeMapping
          .read(DriftSqlType.string, data['${effectivePrefix}preuve_url']),
      commentaire: attachedDatabase.typeMapping
          .read(DriftSqlType.string, data['${effectivePrefix}commentaire']),
      dateConstat: attachedDatabase.typeMapping
          .read(DriftSqlType.dateTime, data['${effectivePrefix}date_constat'])!,
      synced: attachedDatabase.typeMapping
          .read(DriftSqlType.bool, data['${effectivePrefix}synced'])!,
      criticite: attachedDatabase.typeMapping
          .read(DriftSqlType.string, data['${effectivePrefix}criticite']),
      preuveDescription: attachedDatabase.typeMapping.read(
          DriftSqlType.string, data['${effectivePrefix}preuve_description']),
      recommandation: attachedDatabase.typeMapping
          .read(DriftSqlType.string, data['${effectivePrefix}recommandation']),
      composantesImpactees: attachedDatabase.typeMapping.read(
          DriftSqlType.string, data['${effectivePrefix}composantes_impactees']),
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

  /// Chemin local du fichier preuve avant synchronisation, URL serveur MinIO après.
  final String? preuveUrl;
  final String? commentaire;
  final DateTime dateConstat;
  final bool synced;
  final String? criticite;
  final String? preuveDescription;
  final String? recommandation;
  final String? composantesImpactees;
  const LocalConstat(
      {required this.id,
      required this.missionId,
      required this.controleId,
      this.resultat,
      this.preuveUrl,
      this.commentaire,
      required this.dateConstat,
      required this.synced,
      this.criticite,
      this.preuveDescription,
      this.recommandation,
      this.composantesImpactees});
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
    if (!nullToAbsent || criticite != null) {
      map['criticite'] = Variable<String>(criticite);
    }
    if (!nullToAbsent || preuveDescription != null) {
      map['preuve_description'] = Variable<String>(preuveDescription);
    }
    if (!nullToAbsent || recommandation != null) {
      map['recommandation'] = Variable<String>(recommandation);
    }
    if (!nullToAbsent || composantesImpactees != null) {
      map['composantes_impactees'] = Variable<String>(composantesImpactees);
    }
    return map;
  }

  LocalConstatTableCompanion toCompanion(bool nullToAbsent) {
    return LocalConstatTableCompanion(
      id: Value(id),
      missionId: Value(missionId),
      controleId: Value(controleId),
      resultat: resultat == null && nullToAbsent
          ? const Value.absent()
          : Value(resultat),
      preuveUrl: preuveUrl == null && nullToAbsent
          ? const Value.absent()
          : Value(preuveUrl),
      commentaire: commentaire == null && nullToAbsent
          ? const Value.absent()
          : Value(commentaire),
      dateConstat: Value(dateConstat),
      synced: Value(synced),
      criticite: criticite == null && nullToAbsent
          ? const Value.absent()
          : Value(criticite),
      preuveDescription: preuveDescription == null && nullToAbsent
          ? const Value.absent()
          : Value(preuveDescription),
      recommandation: recommandation == null && nullToAbsent
          ? const Value.absent()
          : Value(recommandation),
      composantesImpactees: composantesImpactees == null && nullToAbsent
          ? const Value.absent()
          : Value(composantesImpactees),
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
      criticite: serializer.fromJson<String?>(json['criticite']),
      preuveDescription:
          serializer.fromJson<String?>(json['preuveDescription']),
      recommandation: serializer.fromJson<String?>(json['recommandation']),
      composantesImpactees:
          serializer.fromJson<String?>(json['composantesImpactees']),
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
      'criticite': serializer.toJson<String?>(criticite),
      'preuveDescription': serializer.toJson<String?>(preuveDescription),
      'recommandation': serializer.toJson<String?>(recommandation),
      'composantesImpactees': serializer.toJson<String?>(composantesImpactees),
    };
  }

  LocalConstat copyWith(
          {String? id,
          String? missionId,
          String? controleId,
          Value<String?> resultat = const Value.absent(),
          Value<String?> preuveUrl = const Value.absent(),
          Value<String?> commentaire = const Value.absent(),
          DateTime? dateConstat,
          bool? synced,
          Value<String?> criticite = const Value.absent(),
          Value<String?> preuveDescription = const Value.absent(),
          Value<String?> recommandation = const Value.absent(),
          Value<String?> composantesImpactees = const Value.absent()}) =>
      LocalConstat(
        id: id ?? this.id,
        missionId: missionId ?? this.missionId,
        controleId: controleId ?? this.controleId,
        resultat: resultat.present ? resultat.value : this.resultat,
        preuveUrl: preuveUrl.present ? preuveUrl.value : this.preuveUrl,
        commentaire: commentaire.present ? commentaire.value : this.commentaire,
        dateConstat: dateConstat ?? this.dateConstat,
        synced: synced ?? this.synced,
        criticite: criticite.present ? criticite.value : this.criticite,
        preuveDescription: preuveDescription.present
            ? preuveDescription.value
            : this.preuveDescription,
        recommandation:
            recommandation.present ? recommandation.value : this.recommandation,
        composantesImpactees: composantesImpactees.present
            ? composantesImpactees.value
            : this.composantesImpactees,
      );
  LocalConstat copyWithCompanion(LocalConstatTableCompanion data) {
    return LocalConstat(
      id: data.id.present ? data.id.value : this.id,
      missionId: data.missionId.present ? data.missionId.value : this.missionId,
      controleId:
          data.controleId.present ? data.controleId.value : this.controleId,
      resultat: data.resultat.present ? data.resultat.value : this.resultat,
      preuveUrl: data.preuveUrl.present ? data.preuveUrl.value : this.preuveUrl,
      commentaire:
          data.commentaire.present ? data.commentaire.value : this.commentaire,
      dateConstat:
          data.dateConstat.present ? data.dateConstat.value : this.dateConstat,
      synced: data.synced.present ? data.synced.value : this.synced,
      criticite: data.criticite.present ? data.criticite.value : this.criticite,
      preuveDescription: data.preuveDescription.present
          ? data.preuveDescription.value
          : this.preuveDescription,
      recommandation: data.recommandation.present
          ? data.recommandation.value
          : this.recommandation,
      composantesImpactees: data.composantesImpactees.present
          ? data.composantesImpactees.value
          : this.composantesImpactees,
    );
  }

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
          ..write('synced: $synced, ')
          ..write('criticite: $criticite, ')
          ..write('preuveDescription: $preuveDescription, ')
          ..write('recommandation: $recommandation, ')
          ..write('composantesImpactees: $composantesImpactees')
          ..write(')'))
        .toString();
  }

  @override
  int get hashCode => Object.hash(
      id,
      missionId,
      controleId,
      resultat,
      preuveUrl,
      commentaire,
      dateConstat,
      synced,
      criticite,
      preuveDescription,
      recommandation,
      composantesImpactees);
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
          other.synced == this.synced &&
          other.criticite == this.criticite &&
          other.preuveDescription == this.preuveDescription &&
          other.recommandation == this.recommandation &&
          other.composantesImpactees == this.composantesImpactees);
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
  final Value<String?> criticite;
  final Value<String?> preuveDescription;
  final Value<String?> recommandation;
  final Value<String?> composantesImpactees;
  final Value<int> rowid;
  const LocalConstatTableCompanion({
    this.id = const Value.absent(),
    this.missionId = const Value.absent(),
    this.controleId = const Value.absent(),
    this.resultat = const Value.absent(),
    this.preuveUrl = const Value.absent(),
    this.commentaire = const Value.absent(),
    this.dateConstat = const Value.absent(),
    this.synced = const Value.absent(),
    this.criticite = const Value.absent(),
    this.preuveDescription = const Value.absent(),
    this.recommandation = const Value.absent(),
    this.composantesImpactees = const Value.absent(),
    this.rowid = const Value.absent(),
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
    this.criticite = const Value.absent(),
    this.preuveDescription = const Value.absent(),
    this.recommandation = const Value.absent(),
    this.composantesImpactees = const Value.absent(),
    this.rowid = const Value.absent(),
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
    Expression<String>? criticite,
    Expression<String>? preuveDescription,
    Expression<String>? recommandation,
    Expression<String>? composantesImpactees,
    Expression<int>? rowid,
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
      if (criticite != null) 'criticite': criticite,
      if (preuveDescription != null) 'preuve_description': preuveDescription,
      if (recommandation != null) 'recommandation': recommandation,
      if (composantesImpactees != null)
        'composantes_impactees': composantesImpactees,
      if (rowid != null) 'rowid': rowid,
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
      Value<bool>? synced,
      Value<String?>? criticite,
      Value<String?>? preuveDescription,
      Value<String?>? recommandation,
      Value<String?>? composantesImpactees,
      Value<int>? rowid}) {
    return LocalConstatTableCompanion(
      id: id ?? this.id,
      missionId: missionId ?? this.missionId,
      controleId: controleId ?? this.controleId,
      resultat: resultat ?? this.resultat,
      preuveUrl: preuveUrl ?? this.preuveUrl,
      commentaire: commentaire ?? this.commentaire,
      dateConstat: dateConstat ?? this.dateConstat,
      synced: synced ?? this.synced,
      criticite: criticite ?? this.criticite,
      preuveDescription: preuveDescription ?? this.preuveDescription,
      recommandation: recommandation ?? this.recommandation,
      composantesImpactees: composantesImpactees ?? this.composantesImpactees,
      rowid: rowid ?? this.rowid,
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
    if (criticite.present) {
      map['criticite'] = Variable<String>(criticite.value);
    }
    if (preuveDescription.present) {
      map['preuve_description'] = Variable<String>(preuveDescription.value);
    }
    if (recommandation.present) {
      map['recommandation'] = Variable<String>(recommandation.value);
    }
    if (composantesImpactees.present) {
      map['composantes_impactees'] =
          Variable<String>(composantesImpactees.value);
    }
    if (rowid.present) {
      map['rowid'] = Variable<int>(rowid.value);
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
          ..write('synced: $synced, ')
          ..write('criticite: $criticite, ')
          ..write('preuveDescription: $preuveDescription, ')
          ..write('recommandation: $recommandation, ')
          ..write('composantesImpactees: $composantesImpactees, ')
          ..write('rowid: $rowid')
          ..write(')'))
        .toString();
  }
}

abstract class _$AppDatabase extends GeneratedDatabase {
  _$AppDatabase(QueryExecutor e) : super(e);
  $AppDatabaseManager get managers => $AppDatabaseManager(this);
  late final $LocalConstatTableTable localConstatTable =
      $LocalConstatTableTable(this);
  @override
  Iterable<TableInfo<Table, Object?>> get allTables =>
      allSchemaEntities.whereType<TableInfo<Table, Object?>>();
  @override
  List<DatabaseSchemaEntity> get allSchemaEntities => [localConstatTable];
}

typedef $$LocalConstatTableTableCreateCompanionBuilder
    = LocalConstatTableCompanion Function({
  required String id,
  required String missionId,
  required String controleId,
  Value<String?> resultat,
  Value<String?> preuveUrl,
  Value<String?> commentaire,
  Value<DateTime> dateConstat,
  Value<bool> synced,
  Value<String?> criticite,
  Value<String?> preuveDescription,
  Value<String?> recommandation,
  Value<String?> composantesImpactees,
  Value<int> rowid,
});
typedef $$LocalConstatTableTableUpdateCompanionBuilder
    = LocalConstatTableCompanion Function({
  Value<String> id,
  Value<String> missionId,
  Value<String> controleId,
  Value<String?> resultat,
  Value<String?> preuveUrl,
  Value<String?> commentaire,
  Value<DateTime> dateConstat,
  Value<bool> synced,
  Value<String?> criticite,
  Value<String?> preuveDescription,
  Value<String?> recommandation,
  Value<String?> composantesImpactees,
  Value<int> rowid,
});

class $$LocalConstatTableTableFilterComposer
    extends Composer<_$AppDatabase, $LocalConstatTableTable> {
  $$LocalConstatTableTableFilterComposer({
    required super.$db,
    required super.$table,
    super.joinBuilder,
    super.$addJoinBuilderToRootComposer,
    super.$removeJoinBuilderFromRootComposer,
  });
  ColumnFilters<String> get id => $composableBuilder(
      column: $table.id, builder: (column) => ColumnFilters(column));

  ColumnFilters<String> get missionId => $composableBuilder(
      column: $table.missionId, builder: (column) => ColumnFilters(column));

  ColumnFilters<String> get controleId => $composableBuilder(
      column: $table.controleId, builder: (column) => ColumnFilters(column));

  ColumnFilters<String> get resultat => $composableBuilder(
      column: $table.resultat, builder: (column) => ColumnFilters(column));

  ColumnFilters<String> get preuveUrl => $composableBuilder(
      column: $table.preuveUrl, builder: (column) => ColumnFilters(column));

  ColumnFilters<String> get commentaire => $composableBuilder(
      column: $table.commentaire, builder: (column) => ColumnFilters(column));

  ColumnFilters<DateTime> get dateConstat => $composableBuilder(
      column: $table.dateConstat, builder: (column) => ColumnFilters(column));

  ColumnFilters<bool> get synced => $composableBuilder(
      column: $table.synced, builder: (column) => ColumnFilters(column));

  ColumnFilters<String> get criticite => $composableBuilder(
      column: $table.criticite, builder: (column) => ColumnFilters(column));

  ColumnFilters<String> get preuveDescription => $composableBuilder(
      column: $table.preuveDescription,
      builder: (column) => ColumnFilters(column));

  ColumnFilters<String> get recommandation => $composableBuilder(
      column: $table.recommandation,
      builder: (column) => ColumnFilters(column));

  ColumnFilters<String> get composantesImpactees => $composableBuilder(
      column: $table.composantesImpactees,
      builder: (column) => ColumnFilters(column));
}

class $$LocalConstatTableTableOrderingComposer
    extends Composer<_$AppDatabase, $LocalConstatTableTable> {
  $$LocalConstatTableTableOrderingComposer({
    required super.$db,
    required super.$table,
    super.joinBuilder,
    super.$addJoinBuilderToRootComposer,
    super.$removeJoinBuilderFromRootComposer,
  });
  ColumnOrderings<String> get id => $composableBuilder(
      column: $table.id, builder: (column) => ColumnOrderings(column));

  ColumnOrderings<String> get missionId => $composableBuilder(
      column: $table.missionId, builder: (column) => ColumnOrderings(column));

  ColumnOrderings<String> get controleId => $composableBuilder(
      column: $table.controleId, builder: (column) => ColumnOrderings(column));

  ColumnOrderings<String> get resultat => $composableBuilder(
      column: $table.resultat, builder: (column) => ColumnOrderings(column));

  ColumnOrderings<String> get preuveUrl => $composableBuilder(
      column: $table.preuveUrl, builder: (column) => ColumnOrderings(column));

  ColumnOrderings<String> get commentaire => $composableBuilder(
      column: $table.commentaire, builder: (column) => ColumnOrderings(column));

  ColumnOrderings<DateTime> get dateConstat => $composableBuilder(
      column: $table.dateConstat, builder: (column) => ColumnOrderings(column));

  ColumnOrderings<bool> get synced => $composableBuilder(
      column: $table.synced, builder: (column) => ColumnOrderings(column));

  ColumnOrderings<String> get criticite => $composableBuilder(
      column: $table.criticite, builder: (column) => ColumnOrderings(column));

  ColumnOrderings<String> get preuveDescription => $composableBuilder(
      column: $table.preuveDescription,
      builder: (column) => ColumnOrderings(column));

  ColumnOrderings<String> get recommandation => $composableBuilder(
      column: $table.recommandation,
      builder: (column) => ColumnOrderings(column));

  ColumnOrderings<String> get composantesImpactees => $composableBuilder(
      column: $table.composantesImpactees,
      builder: (column) => ColumnOrderings(column));
}

class $$LocalConstatTableTableAnnotationComposer
    extends Composer<_$AppDatabase, $LocalConstatTableTable> {
  $$LocalConstatTableTableAnnotationComposer({
    required super.$db,
    required super.$table,
    super.joinBuilder,
    super.$addJoinBuilderToRootComposer,
    super.$removeJoinBuilderFromRootComposer,
  });
  GeneratedColumn<String> get id =>
      $composableBuilder(column: $table.id, builder: (column) => column);

  GeneratedColumn<String> get missionId =>
      $composableBuilder(column: $table.missionId, builder: (column) => column);

  GeneratedColumn<String> get controleId => $composableBuilder(
      column: $table.controleId, builder: (column) => column);

  GeneratedColumn<String> get resultat =>
      $composableBuilder(column: $table.resultat, builder: (column) => column);

  GeneratedColumn<String> get preuveUrl =>
      $composableBuilder(column: $table.preuveUrl, builder: (column) => column);

  GeneratedColumn<String> get commentaire => $composableBuilder(
      column: $table.commentaire, builder: (column) => column);

  GeneratedColumn<DateTime> get dateConstat => $composableBuilder(
      column: $table.dateConstat, builder: (column) => column);

  GeneratedColumn<bool> get synced =>
      $composableBuilder(column: $table.synced, builder: (column) => column);

  GeneratedColumn<String> get criticite =>
      $composableBuilder(column: $table.criticite, builder: (column) => column);

  GeneratedColumn<String> get preuveDescription => $composableBuilder(
      column: $table.preuveDescription, builder: (column) => column);

  GeneratedColumn<String> get recommandation => $composableBuilder(
      column: $table.recommandation, builder: (column) => column);

  GeneratedColumn<String> get composantesImpactees => $composableBuilder(
      column: $table.composantesImpactees, builder: (column) => column);
}

class $$LocalConstatTableTableTableManager extends RootTableManager<
    _$AppDatabase,
    $LocalConstatTableTable,
    LocalConstat,
    $$LocalConstatTableTableFilterComposer,
    $$LocalConstatTableTableOrderingComposer,
    $$LocalConstatTableTableAnnotationComposer,
    $$LocalConstatTableTableCreateCompanionBuilder,
    $$LocalConstatTableTableUpdateCompanionBuilder,
    (
      LocalConstat,
      BaseReferences<_$AppDatabase, $LocalConstatTableTable, LocalConstat>
    ),
    LocalConstat,
    PrefetchHooks Function()> {
  $$LocalConstatTableTableTableManager(
      _$AppDatabase db, $LocalConstatTableTable table)
      : super(TableManagerState(
          db: db,
          table: table,
          createFilteringComposer: () =>
              $$LocalConstatTableTableFilterComposer($db: db, $table: table),
          createOrderingComposer: () =>
              $$LocalConstatTableTableOrderingComposer($db: db, $table: table),
          createComputedFieldComposer: () =>
              $$LocalConstatTableTableAnnotationComposer(
                  $db: db, $table: table),
          updateCompanionCallback: ({
            Value<String> id = const Value.absent(),
            Value<String> missionId = const Value.absent(),
            Value<String> controleId = const Value.absent(),
            Value<String?> resultat = const Value.absent(),
            Value<String?> preuveUrl = const Value.absent(),
            Value<String?> commentaire = const Value.absent(),
            Value<DateTime> dateConstat = const Value.absent(),
            Value<bool> synced = const Value.absent(),
            Value<String?> criticite = const Value.absent(),
            Value<String?> preuveDescription = const Value.absent(),
            Value<String?> recommandation = const Value.absent(),
            Value<String?> composantesImpactees = const Value.absent(),
            Value<int> rowid = const Value.absent(),
          }) =>
              LocalConstatTableCompanion(
            id: id,
            missionId: missionId,
            controleId: controleId,
            resultat: resultat,
            preuveUrl: preuveUrl,
            commentaire: commentaire,
            dateConstat: dateConstat,
            synced: synced,
            criticite: criticite,
            preuveDescription: preuveDescription,
            recommandation: recommandation,
            composantesImpactees: composantesImpactees,
            rowid: rowid,
          ),
          createCompanionCallback: ({
            required String id,
            required String missionId,
            required String controleId,
            Value<String?> resultat = const Value.absent(),
            Value<String?> preuveUrl = const Value.absent(),
            Value<String?> commentaire = const Value.absent(),
            Value<DateTime> dateConstat = const Value.absent(),
            Value<bool> synced = const Value.absent(),
            Value<String?> criticite = const Value.absent(),
            Value<String?> preuveDescription = const Value.absent(),
            Value<String?> recommandation = const Value.absent(),
            Value<String?> composantesImpactees = const Value.absent(),
            Value<int> rowid = const Value.absent(),
          }) =>
              LocalConstatTableCompanion.insert(
            id: id,
            missionId: missionId,
            controleId: controleId,
            resultat: resultat,
            preuveUrl: preuveUrl,
            commentaire: commentaire,
            dateConstat: dateConstat,
            synced: synced,
            criticite: criticite,
            preuveDescription: preuveDescription,
            recommandation: recommandation,
            composantesImpactees: composantesImpactees,
            rowid: rowid,
          ),
          withReferenceMapper: (p0) => p0
              .map((e) => (e.readTable(table), BaseReferences(db, table, e)))
              .toList(),
          prefetchHooksCallback: null,
        ));
}

typedef $$LocalConstatTableTableProcessedTableManager = ProcessedTableManager<
    _$AppDatabase,
    $LocalConstatTableTable,
    LocalConstat,
    $$LocalConstatTableTableFilterComposer,
    $$LocalConstatTableTableOrderingComposer,
    $$LocalConstatTableTableAnnotationComposer,
    $$LocalConstatTableTableCreateCompanionBuilder,
    $$LocalConstatTableTableUpdateCompanionBuilder,
    (
      LocalConstat,
      BaseReferences<_$AppDatabase, $LocalConstatTableTable, LocalConstat>
    ),
    LocalConstat,
    PrefetchHooks Function()>;

class $AppDatabaseManager {
  final _$AppDatabase _db;
  $AppDatabaseManager(this._db);
  $$LocalConstatTableTableTableManager get localConstatTable =>
      $$LocalConstatTableTableTableManager(_db, _db.localConstatTable);
}
