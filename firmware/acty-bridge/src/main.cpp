// Acty HU signal bridge — skeleton.
// For now it only announces itself using the "hello" frame from docs/PROTOCOL.md.
// Inputs, RPM and state frames arrive in tasks FW-1..FW-4.
#include <Arduino.h>

static const char *FW_VERSION = "0.1.0";

static void sendHello() {
  Serial.printf("{\"type\":\"hello\",\"v\":1,\"fw\":\"%s\",\"board\":\"esp32s3\",\"features\":[]}\n",
                FW_VERSION);
}

void setup() {
  Serial.begin(115200);
  sendHello();
}

void loop() {
  // Repeat the hello once a second so a freshly connected app or serial monitor sees it.
  sendHello();
  delay(1000);
}
