#include <freertos/FreeRTOS.h>
#include <freertos/task.h>
#include "MyWiFiUdp.h"
#include <WiFi.h>
#include "esp_camera.h"
#include "Motors.h"
#include "DataProvider.h"
#include <Delay.h>

#define CAMERA_MODEL_AI_THINKER
#include "camera_pins.h"
#define LEDS_FRONT 2

const char *ssid = "Esp32_Cam_4wd";
const char *password = "12345678";
const int port = 1234;

MyWiFiUdp udp;
IPAddress ip(192, 168, 1, 1);
IPAddress netmask(255, 255, 255, 0);
IPAddress androidIp;
uint16_t androidPort;
Motors motors;
DataProvider dataProvider;
bool cameraEnabled = false;

void sendPacketData(uint8_t* buf, uint32_t len) {
  String s = "length " + String(len);
  uint8_t b[s.length()];
  for (int i = 0; i < s.length(); i++) b[i] = (uint8_t) s.charAt(i);
  udp.beginPacket(androidIp, androidPort);
  udp.write(b, s.length());
  udp.endPacket();

  int rest = len % MAXIMUM_UDP_PAYLOAD;
  int parcels = (len - rest) / MAXIMUM_UDP_PAYLOAD;

  for (int a = 0; a < parcels; a++) {
    udp.beginPacket(androidIp, androidPort);
    for (int b = 0; b < MAXIMUM_UDP_PAYLOAD; b++) {
      udp.write(buf[(a * MAXIMUM_UDP_PAYLOAD) + b]);
    }
    udp.endPacket();
  }

  if (rest) {
    udp.beginPacket(androidIp, androidPort);
    for (int a = 0; a < rest; a++) {
      udp.write(buf[(parcels * MAXIMUM_UDP_PAYLOAD) + a]);
    }
    udp.endPacket();
  }
}

void run2(void *arg) {
  camera_fb_t* fb = NULL;
  while (1) {
    if (androidPort == 0 || !cameraEnabled) {
      vTaskDelay(100);
      continue;
    }

    // camera capture
    fb = esp_camera_fb_get();
    if (!fb) {
      Serial.println("Camera capture failed!");
      esp_camera_fb_return(fb);
      continue;
    }

    sendPacketData(fb->buf, fb->len);
    esp_camera_fb_return(fb);
  }
}

void run(void *arg) {
  while (1) {
    // wait for client connection
    while (WiFi.softAPgetStationNum() == 0) {
      androidPort = 0;
      vTaskDelay(500);
      Serial.println("Wait for Wifi client...");
    }

    // receiver data from android
    int packetSize = udp.parsePacket();
    if (packetSize) {
      char packetBuffer[packetSize + 1];
      int size = udp.read(packetBuffer, packetSize + 1);
      packetBuffer[packetSize] = '\0';
      if (size) {
        androidIp = udp.remoteIP();
        androidPort = udp.remotePort();
        DataModel data = dataProvider.retrieve(packetBuffer);
        if (data.invalidCharacters) {
          Serial.print("invalid chars on:");
          Serial.println(packetBuffer);
        }
        motors.putData(data);
        if (data.pwmLedsFront >= 0) analogWrite(LEDS_FRONT, data.pwmLedsFront);
        if (data.cameraState == 1) cameraEnabled = true;
        else if (data.cameraState == 0) cameraEnabled = false;
      }
    }
    motors.compute();

    if (androidPort == 0) {
      motors.disable();
      vTaskDelay(500);
      Serial.println("Wait for UDP client...");
    }
  }
}

void setup() {
  Serial.begin(115200);
  while (!Serial && millis() < 1000);

  analogWriteResolution(10);
  pinMode(LEDS_FRONT, OUTPUT);
  digitalWrite(LEDS_FRONT, HIGH);
  motors.begin();

  // OV2640 camera module
  camera_config_t config;
  config.ledc_channel = LEDC_CHANNEL_0;
  config.ledc_timer = LEDC_TIMER_0;
  config.pin_d0 = Y2_GPIO_NUM;
  config.pin_d1 = Y3_GPIO_NUM;
  config.pin_d2 = Y4_GPIO_NUM;
  config.pin_d3 = Y5_GPIO_NUM;
  config.pin_d4 = Y6_GPIO_NUM;
  config.pin_d5 = Y7_GPIO_NUM;
  config.pin_d6 = Y8_GPIO_NUM;
  config.pin_d7 = Y9_GPIO_NUM;
  config.pin_xclk = XCLK_GPIO_NUM;
  config.pin_pclk = PCLK_GPIO_NUM;
  config.pin_vsync = VSYNC_GPIO_NUM;
  config.pin_href = HREF_GPIO_NUM;
  config.pin_sscb_sda = SIOD_GPIO_NUM;
  config.pin_sscb_scl = SIOC_GPIO_NUM;
  config.pin_pwdn = PWDN_GPIO_NUM;
  config.pin_reset = RESET_GPIO_NUM;
  config.xclk_freq_hz = 20000000;
  config.pixel_format = PIXFORMAT_JPEG;
  config.grab_mode = CAMERA_GRAB_LATEST; // CAMERA_GRAB_WHEN_EMPTY
  config.fb_location = CAMERA_FB_IN_PSRAM;
  config.jpeg_quality = 12;
  config.fb_count = 3; // 1 is slower fps
  config.frame_size = FRAMESIZE_SVGA; // 800x600 - 25fps
  // camera init
  esp_err_t err = esp_camera_init(&config);
  if (err != ESP_OK) {
    Serial.printf("Camera init failed with error 0x%x", err);
    return;
  }

  WiFi.softAPConfig(ip, ip, netmask);
  WiFi.softAP(ssid, password);

  udp.begin(port);
  Serial.println("Initializing tasks...");

  //xTaskCreatePinnedToCore(run, "ControllerTask", 2048, NULL, 4, NULL, APP_CPU_NUM);
  //xTaskCreatePinnedToCore(run2, "TaskOnProtocol", 2048, NULL, 8, NULL, PRO_CPU_NUM);
  xTaskCreate(&run, "ControllerTask", 4096, NULL, tskIDLE_PRIORITY, NULL);
  xTaskCreate(&run2, "CameraImageSenderTask", 4096, NULL, tskIDLE_PRIORITY, NULL);
  Serial.println("Ready");
}

void loop() {
  // not working
}
