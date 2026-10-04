# MyExpenses — Native Print & Backup Fix

This is the same offline MyExpenses web app, packaged with a small native Android bridge.

## Fixed
- **Backup:** Android `ACTION_CREATE_DOCUMENT` opens the system Save dialog. No storage permission is required. Choose **Downloads** (or another folder) and tap Save.
- **Print:** Android `PrintManager` opens the system print dialog instead of `window.print()`.
- Existing local SQL.js data, categories, dark mode, reports and restore remain in the web app.

## GitHub build
1. Upload all files/folders, including `.github`.
2. Open **Actions → Build MyExpenses APK → Run workflow**.
3. Open the completed run and download **MyExpenses-debug-apk**.

The APK is a debug APK. Uninstall the old package first if Android reports a signature conflict.
