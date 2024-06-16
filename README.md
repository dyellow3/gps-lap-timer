# GPS Lap Timer
This project combines an Arduino-based GPS device for data collection, with an Android app for communication and calculation of lap data. 

## Features
- **Bluetooth Connectivity:** The app shows a list of paired devices, allowing the user to connect to the GPS Arduino device.
- **Lap Tracking:** Once connected, users can start and stop lap tracking sessions.
- **Automatic Lap Detection:** Upon completion of lap tracking, the app processes the GPS data to detect lap boundaries and times. It uses the Google Maps API to visually display each lap.
- **History Management:** The app maintains a history of all previous laps, allowing users to review past lap data to get insight into performance improvement and progression.
