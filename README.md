# AgapelTech Retail Management System

A robust Android-based retail management solution designed for efficiency, accuracy, and ease of use in retail environments.

## 🚀 Features
- **Dashboard Analytics**: Real-time sales, expenses, and profit tracking with visual charts (MPAndroidChart).
- **Inventory Management**: Full CRUD operations with low-stock alerts and category filtering.
- **Barcode Scanning**: Integrated CameraX and ML Kit for rapid product lookup and sales.
- **Sales & Credit Tracking**: Record transactions, manage customer debt, and send WhatsApp reminders.
- **Offline Sync**: Local SQLite storage with automatic Firebase cloud synchronization.
- **Report Generation**: Export daily/weekly sales reports to PDF and Excel formats.
- **Role-Based Access**: Secure Manager and Staff modes.

## 🆕 What's New in v1.0.6
- **Stability Polish**: Further refinements to background sync logic to ensure zero data loss on unstable networks.
- **Resource Optimization**: Reduced APK size through optimized Proguard/R8 rules.
- **UI Enhancements**: Smoother transitions between fragments and improved dark mode contrast.
- **Android 15 Ready**: Optimized Edge-to-Edge experience across all app sections.

### Version History
- **v1.0.5**: Initial Android 15 support and fundamental stability fixes.

## 🛠️ Technology Stack
- **Language**: Java
- **Database**: SQLite (Local) & Firebase Realtime Database (Cloud)
- **Scanning**: Google ML Kit & Android CameraX
- **Charts**: MPAndroidChart
- **PDF/Excel**: iText / CSV Export

## 📦 Installation
### For Developers
1. Clone the repository: `git clone https://github.com/your-username/AgapelTech.git`
2. Open in Android Studio.
3. Add your `google-services.json` in the `app/` directory.
4. Build and Run.

### For Users (Direct Install)
1. Download the `AgapelTech-v1.0.6.apk` from the **Releases** section.
2. Transfer the APK to your Android device (or download it directly).
3. Open the file on your phone.
4. If prompted, enable "Install from Unknown Sources" in your settings.
5. Follow the on-screen instructions to complete the installation.

## 📄 License
This project is proprietary. All rights reserved.
