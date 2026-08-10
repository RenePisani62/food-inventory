# Changelog

# V 1.4.2
- New PantryPal Launcher Icon.
- Bluetooth barcode scanner workflow improvements.
- Automatic focus on barcode input field at startup.
- Automatic keyboard suppression for Bluetooth scanner workflows.
- Persistent scanner focus after barcode processing.
- Add/Delete mode status indicator on Home Screen.
- Toast notifications for successful item additions.
- Toast notifications for successful item removals.
- Portrait scanner support via dedicated PortraitCaptureActivity.
- Improved scanner behaviour on Samsung Note 10 devices.

- IMPROVED
- Unknown-item detection logic expanded.
- Unknown-item popup reliability improved.
- Unknown-item storage workflow simplified.
- Barcode-name memory cache workflow improved.
- Quick Scan scanner workflow improved.
- Bluetooth scanner compatibility improved.
- Navigation/taskbar visibility improvements on Home, Shopping List, Inventory and Product  Detail screens.
- FileProvider/CSV export stability improvements.
- Adaptive launcher icon support.

- FIXED
- Unknown products not triggering popup under certain conditions.
- Portrait scanner orientation issues.
- Scanner popup disappearing during orientation changes.
- Inventory screen scroll/layout crash.
- CSV export crash caused by FileProvider configuration.
- Scanner focus being lost after barcode processing.
- Quick Scan switch accidentally toggled by Bluetooth scanner input.
- Product quantity update confirmation missing on repeated scans.

## v1.4

### Added
- Added Bluetooth/manual barcode input support.
- Added Enter/Done key processing for scanner-style barcode entry.
- Added unknown product naming workflow with local barcode-name memory.
- Added dedicated Shopping List screen.
- Added interactive shopping list checkboxes.
- Added manual shopping list item entry.
- Added permanent product delete option.
- Added Bakery category with 5-day expiry default.
- Added Refrigerated: Fresh and Refrigerated: Long-life categories.

### Improved
- Improved Quick Scan workflow.
- Improved category-driven expiry and storage-location defaults.
- Improved shopping list usability and low-resolution layout.
- Improved zero-stock and out-of-stock shopping list behaviour.

v1.3.1
- Added manual/Bluetooth barcode input foundation
- Refactored barcode processing into reusable processBarcode()
- Improved scan workflow architecture
- Added layout spacing/polish
- Permanent delete

## v1.3
- Added Product Detail screen
- Added zero-stock retention
- Improved shopping list logic
- Added persistent intake defaults
- Improved low-resolution usability

## v1.2
- Added expiry notifications
- Added import/export support
- Added inventory editing
