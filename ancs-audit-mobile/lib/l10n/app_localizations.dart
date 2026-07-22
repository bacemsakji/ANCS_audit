import 'dart:async';

import 'package:flutter/foundation.dart';
import 'package:flutter/widgets.dart';
import 'package:flutter_localizations/flutter_localizations.dart';
import 'package:intl/intl.dart' as intl;

import 'app_localizations_ar.dart';
import 'app_localizations_en.dart';
import 'app_localizations_fr.dart';

// ignore_for_file: type=lint

/// Callers can lookup localized strings with an instance of AppLocalizations
/// returned by `AppLocalizations.of(context)`.
///
/// Applications need to include `AppLocalizations.delegate()` in their app's
/// `localizationDelegates` list, and the locales they support in the app's
/// `supportedLocales` list. For example:
///
/// ```dart
/// import 'l10n/app_localizations.dart';
///
/// return MaterialApp(
///   localizationsDelegates: AppLocalizations.localizationsDelegates,
///   supportedLocales: AppLocalizations.supportedLocales,
///   home: MyApplicationHome(),
/// );
/// ```
///
/// ## Update pubspec.yaml
///
/// Please make sure to update your pubspec.yaml to include the following
/// packages:
///
/// ```yaml
/// dependencies:
///   # Internationalization support.
///   flutter_localizations:
///     sdk: flutter
///   intl: any # Use the pinned version from flutter_localizations
///
///   # Rest of dependencies
/// ```
///
/// ## iOS Applications
///
/// iOS applications define key application metadata, including supported
/// locales, in an Info.plist file that is built into the application bundle.
/// To configure the locales supported by your app, you’ll need to edit this
/// file.
///
/// First, open your project’s ios/Runner.xcworkspace Xcode workspace file.
/// Then, in the Project Navigator, open the Info.plist file under the Runner
/// project’s Runner folder.
///
/// Next, select the Information Property List item, select Add Item from the
/// Editor menu, then select Localizations from the pop-up menu.
///
/// Select and expand the newly-created Localizations item then, for each
/// locale your application supports, add a new item and select the locale
/// you wish to add from the pop-up menu in the Value field. This list should
/// be consistent with the languages listed in the AppLocalizations.supportedLocales
/// property.
abstract class AppLocalizations {
  AppLocalizations(String locale)
      : localeName = intl.Intl.canonicalizedLocale(locale.toString());

  final String localeName;

  static AppLocalizations? of(BuildContext context) {
    return Localizations.of<AppLocalizations>(context, AppLocalizations);
  }

  static const LocalizationsDelegate<AppLocalizations> delegate =
      _AppLocalizationsDelegate();

  /// A list of this localizations delegate along with the default localizations
  /// delegates.
  ///
  /// Returns a list of localizations delegates containing this delegate along with
  /// GlobalMaterialLocalizations.delegate, GlobalCupertinoLocalizations.delegate,
  /// and GlobalWidgetsLocalizations.delegate.
  ///
  /// Additional delegates can be added by appending to this list in
  /// MaterialApp. This list does not have to be used at all if a custom list
  /// of delegates is preferred or required.
  static const List<LocalizationsDelegate<dynamic>> localizationsDelegates =
      <LocalizationsDelegate<dynamic>>[
    delegate,
    GlobalMaterialLocalizations.delegate,
    GlobalCupertinoLocalizations.delegate,
    GlobalWidgetsLocalizations.delegate,
  ];

  /// A list of this localizations delegate's supported locales.
  static const List<Locale> supportedLocales = <Locale>[
    Locale('ar'),
    Locale('en'),
    Locale('fr')
  ];

  /// No description provided for @appTitle.
  ///
  /// In fr, this message translates to:
  /// **'ANCS Audit'**
  String get appTitle;

  /// No description provided for @login.
  ///
  /// In fr, this message translates to:
  /// **'Connexion'**
  String get login;

  /// No description provided for @username.
  ///
  /// In fr, this message translates to:
  /// **'Nom d\'utilisateur'**
  String get username;

  /// No description provided for @password.
  ///
  /// In fr, this message translates to:
  /// **'Mot de passe'**
  String get password;

  /// No description provided for @submit.
  ///
  /// In fr, this message translates to:
  /// **'Soumettre'**
  String get submit;

  /// No description provided for @cancel.
  ///
  /// In fr, this message translates to:
  /// **'Annuler'**
  String get cancel;

  /// No description provided for @dashboard.
  ///
  /// In fr, this message translates to:
  /// **'Tableau de bord'**
  String get dashboard;

  /// No description provided for @missions.
  ///
  /// In fr, this message translates to:
  /// **'Missions'**
  String get missions;

  /// No description provided for @reports.
  ///
  /// In fr, this message translates to:
  /// **'Rapports'**
  String get reports;

  /// No description provided for @actions.
  ///
  /// In fr, this message translates to:
  /// **'Actions'**
  String get actions;

  /// No description provided for @checklist.
  ///
  /// In fr, this message translates to:
  /// **'Liste de contrôle'**
  String get checklist;

  /// No description provided for @logout.
  ///
  /// In fr, this message translates to:
  /// **'Déconnexion'**
  String get logout;

  /// No description provided for @missionDetails.
  ///
  /// In fr, this message translates to:
  /// **'Détails de la Mission'**
  String get missionDetails;

  /// No description provided for @startAudit.
  ///
  /// In fr, this message translates to:
  /// **'Commencer l\'audit'**
  String get startAudit;

  /// No description provided for @organism.
  ///
  /// In fr, this message translates to:
  /// **'Organisme'**
  String get organism;

  /// No description provided for @referentiel.
  ///
  /// In fr, this message translates to:
  /// **'Référentiel'**
  String get referentiel;

  /// No description provided for @scope.
  ///
  /// In fr, this message translates to:
  /// **'Périmètre'**
  String get scope;

  /// No description provided for @dates.
  ///
  /// In fr, this message translates to:
  /// **'Dates'**
  String get dates;

  /// No description provided for @startDate.
  ///
  /// In fr, this message translates to:
  /// **'Début'**
  String get startDate;

  /// No description provided for @endDate.
  ///
  /// In fr, this message translates to:
  /// **'Fin'**
  String get endDate;

  /// No description provided for @auditor.
  ///
  /// In fr, this message translates to:
  /// **'Auditeur'**
  String get auditor;

  /// No description provided for @conform.
  ///
  /// In fr, this message translates to:
  /// **'Conforme'**
  String get conform;

  /// No description provided for @nonConform.
  ///
  /// In fr, this message translates to:
  /// **'Non conforme'**
  String get nonConform;

  /// No description provided for @observation.
  ///
  /// In fr, this message translates to:
  /// **'Observation'**
  String get observation;

  /// No description provided for @comment.
  ///
  /// In fr, this message translates to:
  /// **'Commentaire'**
  String get comment;

  /// No description provided for @save.
  ///
  /// In fr, this message translates to:
  /// **'Enregistrer'**
  String get save;

  /// No description provided for @notSynced.
  ///
  /// In fr, this message translates to:
  /// **'Non synchronisé'**
  String get notSynced;

  /// No description provided for @addProof.
  ///
  /// In fr, this message translates to:
  /// **'Ajouter une preuve'**
  String get addProof;

  /// No description provided for @changeProof.
  ///
  /// In fr, this message translates to:
  /// **'Changer la preuve'**
  String get changeProof;

  /// No description provided for @executiveSummary.
  ///
  /// In fr, this message translates to:
  /// **'Synthèse exécutive'**
  String get executiveSummary;

  /// No description provided for @generateDraft.
  ///
  /// In fr, this message translates to:
  /// **'Générer un brouillon (IA)'**
  String get generateDraft;

  /// No description provided for @exportFormat.
  ///
  /// In fr, this message translates to:
  /// **'Format d\'exportation'**
  String get exportFormat;

  /// No description provided for @pdf.
  ///
  /// In fr, this message translates to:
  /// **'PDF (Document officiel ANCS)'**
  String get pdf;

  /// No description provided for @docx.
  ///
  /// In fr, this message translates to:
  /// **'Word (DOCX — modifiable)'**
  String get docx;

  /// No description provided for @compileReport.
  ///
  /// In fr, this message translates to:
  /// **'Compiler le rapport final'**
  String get compileReport;

  /// No description provided for @downloadDocument.
  ///
  /// In fr, this message translates to:
  /// **'Télécharger le document'**
  String get downloadDocument;

  /// No description provided for @priority.
  ///
  /// In fr, this message translates to:
  /// **'Priorité'**
  String get priority;

  /// No description provided for @critical.
  ///
  /// In fr, this message translates to:
  /// **'Critique'**
  String get critical;

  /// No description provided for @high.
  ///
  /// In fr, this message translates to:
  /// **'Haute'**
  String get high;

  /// No description provided for @medium.
  ///
  /// In fr, this message translates to:
  /// **'Moyenne'**
  String get medium;

  /// No description provided for @low.
  ///
  /// In fr, this message translates to:
  /// **'Faible'**
  String get low;

  /// No description provided for @status.
  ///
  /// In fr, this message translates to:
  /// **'Statut'**
  String get status;

  /// No description provided for @todo.
  ///
  /// In fr, this message translates to:
  /// **'À faire'**
  String get todo;

  /// No description provided for @inProgress.
  ///
  /// In fr, this message translates to:
  /// **'En cours'**
  String get inProgress;

  /// No description provided for @overdue.
  ///
  /// In fr, this message translates to:
  /// **'En retard'**
  String get overdue;

  /// No description provided for @closed.
  ///
  /// In fr, this message translates to:
  /// **'Clôturée'**
  String get closed;

  /// No description provided for @responsible.
  ///
  /// In fr, this message translates to:
  /// **'Responsable'**
  String get responsible;

  /// No description provided for @deadline.
  ///
  /// In fr, this message translates to:
  /// **'Échéance'**
  String get deadline;

  /// No description provided for @myActions.
  ///
  /// In fr, this message translates to:
  /// **'Mes Actions Correctives (RSSI)'**
  String get myActions;

  /// No description provided for @missionActions.
  ///
  /// In fr, this message translates to:
  /// **'Actions correctives de la mission'**
  String get missionActions;

  /// No description provided for @noActions.
  ///
  /// In fr, this message translates to:
  /// **'Aucune action corrective planifiée'**
  String get noActions;

  /// No description provided for @start.
  ///
  /// In fr, this message translates to:
  /// **'Commencer'**
  String get start;

  /// No description provided for @close.
  ///
  /// In fr, this message translates to:
  /// **'Clôturer'**
  String get close;

  /// No description provided for @adminDashboard.
  ///
  /// In fr, this message translates to:
  /// **'Tableau de Bord — Administration'**
  String get adminDashboard;

  /// No description provided for @rssiDashboard.
  ///
  /// In fr, this message translates to:
  /// **'Mon Tableau de Bord'**
  String get rssiDashboard;

  /// No description provided for @ongoingMissions.
  ///
  /// In fr, this message translates to:
  /// **'Missions en cours'**
  String get ongoingMissions;

  /// No description provided for @plannedMissions.
  ///
  /// In fr, this message translates to:
  /// **'Missions planifiées'**
  String get plannedMissions;

  /// No description provided for @organizations.
  ///
  /// In fr, this message translates to:
  /// **'Organismes'**
  String get organizations;

  /// No description provided for @activeAuditors.
  ///
  /// In fr, this message translates to:
  /// **'Auditeurs actifs'**
  String get activeAuditors;

  /// No description provided for @globalCompliance.
  ///
  /// In fr, this message translates to:
  /// **'Conformité globale'**
  String get globalCompliance;

  /// No description provided for @certificationAlerts.
  ///
  /// In fr, this message translates to:
  /// **'Alertes de certification'**
  String get certificationAlerts;

  /// No description provided for @recentMissions.
  ///
  /// In fr, this message translates to:
  /// **'Missions récentes'**
  String get recentMissions;

  /// No description provided for @lastAudit.
  ///
  /// In fr, this message translates to:
  /// **'Dernier audit'**
  String get lastAudit;

  /// No description provided for @completedMissions.
  ///
  /// In fr, this message translates to:
  /// **'mission(s) réalisée(s)'**
  String get completedMissions;

  /// No description provided for @correctiveActions.
  ///
  /// In fr, this message translates to:
  /// **'Suivi des actions correctives'**
  String get correctiveActions;

  /// No description provided for @priorityActions.
  ///
  /// In fr, this message translates to:
  /// **'Actions prioritaires'**
  String get priorityActions;

  /// No description provided for @dataUnavailable.
  ///
  /// In fr, this message translates to:
  /// **'Données non disponibles'**
  String get dataUnavailable;

  /// No description provided for @retry.
  ///
  /// In fr, this message translates to:
  /// **'Réessayer'**
  String get retry;

  /// No description provided for @error.
  ///
  /// In fr, this message translates to:
  /// **'Erreur'**
  String get error;

  /// No description provided for @loading.
  ///
  /// In fr, this message translates to:
  /// **'Chargement...'**
  String get loading;

  /// No description provided for @success.
  ///
  /// In fr, this message translates to:
  /// **'Succès'**
  String get success;

  /// No description provided for @offlineMode.
  ///
  /// In fr, this message translates to:
  /// **'Mode hors-ligne'**
  String get offlineMode;

  /// No description provided for @onlineMode.
  ///
  /// In fr, this message translates to:
  /// **'Mode en ligne'**
  String get onlineMode;

  /// No description provided for @syncPending.
  ///
  /// In fr, this message translates to:
  /// **'Synchronisation en attente'**
  String get syncPending;

  /// No description provided for @loginTitle.
  ///
  /// In fr, this message translates to:
  /// **'Portail d\'Audit ANCS'**
  String get loginTitle;

  /// No description provided for @loginSubtitle.
  ///
  /// In fr, this message translates to:
  /// **'Réglementation Décret-loi 2023-17'**
  String get loginSubtitle;

  /// No description provided for @emailLabel.
  ///
  /// In fr, this message translates to:
  /// **'Adresse e-mail'**
  String get emailLabel;

  /// No description provided for @passwordLabel.
  ///
  /// In fr, this message translates to:
  /// **'Mot de passe'**
  String get passwordLabel;

  /// No description provided for @loginButton.
  ///
  /// In fr, this message translates to:
  /// **'Se connecter'**
  String get loginButton;

  /// No description provided for @mfaVerifyTitle.
  ///
  /// In fr, this message translates to:
  /// **'Vérification de sécurité (2FA)'**
  String get mfaVerifyTitle;

  /// No description provided for @mfaVerifyButton.
  ///
  /// In fr, this message translates to:
  /// **'Valider'**
  String get mfaVerifyButton;

  /// No description provided for @syncStatusOnline.
  ///
  /// In fr, this message translates to:
  /// **'En ligne'**
  String get syncStatusOnline;

  /// No description provided for @syncStatusOffline.
  ///
  /// In fr, this message translates to:
  /// **'Hors-ligne'**
  String get syncStatusOffline;

  /// No description provided for @generateIaSummaryButton.
  ///
  /// In fr, this message translates to:
  /// **'Générer un brouillon (IA)'**
  String get generateIaSummaryButton;

  /// No description provided for @iaDraftBadge.
  ///
  /// In fr, this message translates to:
  /// **'Généré par IA — à relire'**
  String get iaDraftBadge;

  /// No description provided for @iaOfflineError.
  ///
  /// In fr, this message translates to:
  /// **'Fonction IA indisponible hors-ligne'**
  String get iaOfflineError;

  /// No description provided for @validateAndInsertButton.
  ///
  /// In fr, this message translates to:
  /// **'Valider et insérer dans le rapport'**
  String get validateAndInsertButton;

  /// No description provided for @loginAgency.
  ///
  /// In fr, this message translates to:
  /// **'Agence Nationale de Cybersécurité'**
  String get loginAgency;

  /// No description provided for @requiredField.
  ///
  /// In fr, this message translates to:
  /// **'Champ obligatoire'**
  String get requiredField;

  /// No description provided for @invalidEmail.
  ///
  /// In fr, this message translates to:
  /// **'Adresse e-mail invalide'**
  String get invalidEmail;

  /// No description provided for @mfaNotice.
  ///
  /// In fr, this message translates to:
  /// **'Les administrateurs ANCS doivent compléter la vérification à deux facteurs avant d\'accéder au portail d\'audit.'**
  String get mfaNotice;
}

class _AppLocalizationsDelegate
    extends LocalizationsDelegate<AppLocalizations> {
  const _AppLocalizationsDelegate();

  @override
  Future<AppLocalizations> load(Locale locale) {
    return SynchronousFuture<AppLocalizations>(lookupAppLocalizations(locale));
  }

  @override
  bool isSupported(Locale locale) =>
      <String>['ar', 'en', 'fr'].contains(locale.languageCode);

  @override
  bool shouldReload(_AppLocalizationsDelegate old) => false;
}

AppLocalizations lookupAppLocalizations(Locale locale) {
  // Lookup logic when only language code is specified.
  switch (locale.languageCode) {
    case 'ar':
      return AppLocalizationsAr();
    case 'en':
      return AppLocalizationsEn();
    case 'fr':
      return AppLocalizationsFr();
  }

  throw FlutterError(
      'AppLocalizations.delegate failed to load unsupported locale "$locale". This is likely '
      'an issue with the localizations generation tool. Please file an issue '
      'on GitHub with a reproducible sample app and the gen-l10n configuration '
      'that was used.');
}
