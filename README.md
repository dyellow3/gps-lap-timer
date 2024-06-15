A GPS lap timer consisting of
- An Arduino-based GPS device for data collection 
- An Android app for communication and calcultions of lap data

Features
- Bluetooth Connectivity: The app shows a list of paired devices, allowing the user to connect to the GPS Arduino device.
- Lap Tracking: Users can start and stop lap tracking through the app. When tracking is started, the app sends a "start" command to the Arduino, which begins collecting and sending GPS data.
- Automatic Lap Detection: The app processes the incoming GPS data, automatically detecting lap boundaries and times. It uses the Google Maps API to visually display each lap.
- History Management: The app maintains a history of all previous laps, allowing users to review past performance.
