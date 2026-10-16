# Privacy Policy for Flight Checklist

**Last Updated: October 16, 2026**

This Privacy Policy applies to the **Flight Checklist** mobile application (hereinafter referred to as "the App").

---

## 1. Zero Data Collection Principle
The App is built with a strict "Zero External Telemetry" privacy architecture.
- We **do not collect, transmit, store, or share** any personally identifiable information (PII).
- We do not collect your name, email address, physical location, device identifiers, IP address, or usage analytics.

## 2. No Internet Permissions Required
The App does not request the `android.permission.INTERNET` permission in AndroidManifest.xml. It operates entirely offline, making external network communication physically impossible.

## 3. Local-Only Storage
All data entered or generated in the App—including checklist items, custom phases, templates, review notes, daily completion records, and settings—is stored exclusively on your device's private internal storage (`checklist_state.json`).

## 4. Optional Notification Permission
The App requests the `android.permission.POST_NOTIFICATIONS` permission solely to display active REQUIRED (REQ) checklist tasks in your device's notification bar when you explicitly enable this feature in Settings. This permission is strictly opt-in and operates 100% locally on your device with no external data transmission.

## 5. Third-Party Services and Advertising
The App contains:
- **No advertisements (No ad SDKs)**
- **No analytics or telemetry trackers (No Google Analytics, Firebase, etc.)**
- **No third-party SDKs that process personal data**

## 6. Data Retention and Deletion
You maintain complete control over your data. You can delete all application data at any time by:
1. Using the "Restore Default Phases" or "Reset All Phases" options in the App's settings, or
2. Clearing the application data via your Android device's **Settings > Apps > Flight Checklist > Storage > Clear Data**, or
3. Uninstalling the App.

## 7. Children's Privacy
Because the App does not collect any personal information whatsoever, it complies with the Children's Online Privacy Protection Act (COPPA) and GDPR standards regarding minors.

## 8. Contact
If you have any questions or feedback regarding this Privacy Policy, please open an issue on GitHub:
- **GitHub Repository**: [https://github.com/k11301440/flight-checklist](https://github.com/k11301440/flight-checklist)
