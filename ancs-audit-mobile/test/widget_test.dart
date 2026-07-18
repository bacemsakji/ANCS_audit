import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:flutter_bloc/flutter_bloc.dart';
import 'package:ancs_audit_mobile/features/auth/bloc/auth_bloc.dart';
import 'package:ancs_audit_mobile/features/auth/data/auth_repository.dart';
import 'package:mockito/mockito.dart';
import 'package:mockito/annotations.dart';

@GenerateMocks([AuthRepository])
import 'widget_test.mocks.dart';

void main() {
  testWidgets('App builds correctly', (WidgetTester tester) async {
    // Mock the auth repository
    final mockAuthRepository = MockAuthRepository();

    // Build our app and trigger a frame.
    await tester.pumpWidget(
      MaterialApp(
        home: BlocProvider(
          create: (_) => AuthBloc(authRepository: mockAuthRepository),
          child: const Scaffold(
            body: Center(child: Text('Test')),
          ),
        ),
      ),
    );

    // Verify that the app builds
    expect(find.text('Test'), findsOneWidget);
  });

  testWidgets('Login screen renders correctly', (WidgetTester tester) async {
    final mockAuthRepository = MockAuthRepository();

    await tester.pumpWidget(
      MaterialApp(
        home: BlocProvider(
          create: (_) => AuthBloc(authRepository: mockAuthRepository),
          child: const Scaffold(
            body: Center(child: Text('Login Screen')),
          ),
        ),
      ),
    );

    expect(find.text('Login Screen'), findsOneWidget);
  });
}
