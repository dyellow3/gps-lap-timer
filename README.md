# GPS Lap Timer
An Android app that uses your phone's GPS to automatically track laps, providing insight into performance for racing and other circuit-based activites.

<p align="center">
    <img src="./images/in-app%20view.png" alt="In-App View" width="400">
</p>

## Features
- **GPS Tracking:** Utilizes the phone's GPS for location tracking.
- **Automatic Lap Detection:** Automatically detects lap completions and calculates lap times and lap-to-lap time variations.
- **History Management:** Saves tracking sessions for later review and analysis, giving insight to performance progression.
- **Map Visualization**: Uses Google Maps API to visually display laps.
- **Settings**: Allows users to adjust parameters used for the lap detection algorithm.

## Main Components
- **AddFragment**: Handles GPS tracking, data logging, and file saving.
- **MapFragment**: Visualizes tracked laps on a Google Map and displays lap statistics.
- **HistoryFragment**: Displays past sessions.
- **Settings Fragment**: Allows for adjustment of parameters.
- **LapDetection**: Contains the algorithm for detecting laps and calculating lap times.
- **MapDrawing**: Contains various functions that interact and draw on the Google Map.

## Requirements
- Android device with ideally 1hz GPS capability
- A Google Cloud API Key with the following 3 APIs activated: Maps SDK for Android, Maps JavaScript API, Geocoding API. **Put this API Key into the secrets.properties file**.
