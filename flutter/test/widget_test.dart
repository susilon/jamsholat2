import 'package:flutter_test/flutter_test.dart';

import 'package:jamsholat/main.dart';

void main() {
  testWidgets('Jam Sholat app loads with default mosque name', (WidgetTester tester) async {
    await tester.pumpWidget(const JamSholatApp());

    expect(find.text('Nama Masjid'), findsOneWidget);
  });
}
