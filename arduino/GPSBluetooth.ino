#include <TinyGPS++.h>
#include <SoftwareSerial.h>
#include <SPI.h>

// Bluetooth setup
// RXP pin = 0, TXP pin = 1

// GPS setup
static const int gpsRXPin = 3, gpsTXPin = 4;
TinyGPSPlus gps;
SoftwareSerial GPSSerial(gpsTXPin, gpsRXPin);

void setup() {
  GPSSerial.begin(9600);
  Serial.begin(9600);
}

String message = "";
String messageBuffer = "";
unsigned long last = 0;
unsigned long last2 = 0;
bool gpsFlag = false;

void loop() {
  // Collect incoming characters from Bluetooth. Messages will end with ";"
  while (Serial.available() > 0) {
    char data = (char)Serial.read();
    messageBuffer += data;
    if (data == ';') {
      messageBuffer.trim();
      message = messageBuffer;
      messageBuffer = "";
    }
    // Max size of message limited to 16
    if (messageBuffer.length() > 16) {
      messageBuffer = "";
    }
  }

  if (message.equals("start;")) {
    if (gps.date.month() == 0) {
      Serial.println(F("ERROR Start - GPS not ready"));
    } else {
        gpsFlag = true;
        Serial.println(F("Starting GPS"));
    }
  }

  if (message.equals("stop;")) {
      Serial.println(F("Stopping GPS"));
      gpsFlag = false;
  }

  message = "";

  // Dispatch incoming characters from GPS
  while (GPSSerial.available() > 0) {
    char gpsData = GPSSerial.read();
    gps.encode(gpsData);
  }

  // GPS data processing
  if (gpsFlag) {
    if (gps.location.isUpdated() && millis() - last2 > 1000) {
      String latitudeStr = String(gps.location.lat(), 6);
      String longitudeStr = String(gps.location.lng(), 6);
      String speedStr = String(gps.speed.mps(), 3);
      String gpsDataStr = latitudeStr + "," + longitudeStr + "," + speedStr;
      Serial.println(gpsDataStr);
      last2 = millis();
    }
  }

  // Error checking for GPS data reception
  if (millis() - last > 5000) {
    if (gps.charsProcessed() < 10) {
      Serial.println(F("ERROR GPS not working"));
    }
    last = millis();
  }
}