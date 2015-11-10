### 0.9.31:
* Fix crashes on Android < 4.2 (this time for real)

### 0.9.30.9:
* Fix late notifications on some (Samsung) devices

### 0.9.30.8:
* Fix crashes on Android < 4.2

### 0.9.30.7:
* Fix crashes on Android < 4.2

### 0.9.30.5:
* Make app compatible with Marshmallow

### 0.9.30.4:
* Fix crashes
* Fix missing menu on certain LG devices

### 0.9.30.3:
* Fix notification issues

### 0.9.30.2:
* Fix crash on ancient Android versions

### 0.9.30.1:
* Fix crash

### 0.9.30:
* Added option to specify an end date for drug schedules
* Better Wear notifications
* New translation: Turkish (Kudret Emre)
* Fixed bugs and crashes

### 0.9.29:
* Disabled "Cloud backup" option
* Fixed crash related to snoozing refill reminders

### 0.9.28.2:
* Fixed crash when editing a medication
* Fixed translations for French, Japanese, Polish and Finnish

### 0.9.28.1:
* Fixed French translation

### 0.9.28:
* Added French and Polish translation
* Some bugs fixed

### 0.9.27:
* Fix crash when pressing the menu key on certain (LG) devices

### 0.9.26.1:
* Minor bug fixes

### 0.9.26:
* Fix "Remind tomorrow" bug
* Bug fixes

### 0.9.25.3:
* Create backup before database upgrade

### 0.9.25.2:
* Fix crash on certain (Samsung) devices

### 0.9.25.1:
* UI design changes using new Material theme
* Improved app loading time for large databases
* Bug fixes

### 0.9.24:
* New translations: Finnish, Japanese, Ukrainian
* Notifications should now be compatible with Android Wear

### 0.9.23:
* Add option to create/restore backups (requires permission
  to read/write external storage!)
* Swipe/clear notifications on pre-JellyBean to 'take all'
* Add 'Eye dropper' icon
* Bug fixes

### 0.9.22: (not released)

### 0.9.21.3:
* Fixes bug where doses were marked as taken even
though supply was at zero

### 0.9.21.2:
* Fixes bug where times were erroneously suffixed 
  with +<number> in the dose log

### 0.9.21.1:
* Fix date calculation error for 'Every N days' mode

### 0.9.21:
* Added option for smart-sorting based on importance entries
* After an update, notifications will be refreshed immediately
* The currently active doses are highlighted
* More compat layout for non-active medications
* Several smaller bugs fixed


### 0.9.20:
* Medication names are now scrambled in notifications too
* The app now honors system-wide rotation settings
* Fixed autostart issues on some HTC devices
* Fixed some minor layout issues

### 0.9.19:
* Fixed database errors when upgrading from previous versions
* Fixed issue of missing notifications when changing date, time or
  timezone.

### 0.9.18.1
* Added missing German translation

### 0.9.18:
* Added help overlays
* Added option to show pretty fractions
* The "take all" button will now mark all doses mentioned
  in the current notification as taken. If supplies are
  insufficient, they are marked as skipped.
* Fixed launcher icons on ldpi devices

### 0.9.17.2:
* Fixed bug in 21 days on / 7 days off repeat mode

### 0.9.17.1:
* Fixed notification title bug

### 0.9.17:
* Added Greek translation
* Added capsule and inhaler icon
* Extended notifications for >= JellyBean
* Begin and end of a time specification can no longer be equal
* App now behaves as expected when switching between time zones
* When adding drugs, doses in the past no longer show up as missed
* Added option to reset time specs

### 0.9.16.1:
* Doses of inactive drugs are no longer marked as taken when 
  using the "take all" button.

### 0.9.16:
* Bug fixes

### 0.9.15.3:
* Fixed crash when opening menu in main view

### 0.9.15.2:
* Black theme is back

### 0.9.15.1:
* Unified theme - ActionBarSherlock is used for Android < 3.0
* Added option to skip dose dialog
* Added option to mark all due doses as taken

### 0.9.14:
* Bug fixes

### 0.9.13:
* Bug fixes

### 0.9.12:
* Main view no longer scrolls to the top after a dose was taken

### 0.9.11-1:
* Fixed crash in dose history

### 0.9.11:
* New feature: dose history
* Minor bugs fixed

### 0.9.10:
* Setting the refill reminder threshold to "0" effectively disables it.
* Disabled light theme on Android 3.x
* Bug fixes

### 0.9.9-1:
* Fixed crash on Android 3.2

### 0.9.9:
* Fixed bug that prevented setting the time intervals correctly when using 12-hour clock
* Updated Italian translation
* Times without a dose are now displayed as "-"

### 0.9.8:
* New language: Italian
* App now uses backup frameworks such as 
  Google's "Backup & Restore"
* Updated icons
* Minor bugs fixed

### 0.9.7-2:
* Fixed a crash in "Preferences"

### 0.9.7:
* LED color can now be customized (on devices that support it)
* Added a "Quiet hours" option to mute notification sound in a specific time interval
* The supply can now be edited by long-pressing the supply info in the main view
* Fixed a bug that caused notifications to be displayed at the wrong time
