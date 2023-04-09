#include "DataModel.h"

DataModel::DataModel() {
  pwmLedsFront = -1;
  cameraState = -1;
  rotationState = -1;
  pwmLeftForward = 0;
  pwmLeftBackward = 0;
  pwmRightForward = 0;
  pwmRightBackward = 0;
  rotationTimeOn = 0;
  rotationTimeOff = 0;
  isRunForward = false;
  isRunBackward = false;
  isRotationRight = false;
  isRotationLeft = false;
  disconnect = false;
}

void DataModel::retrieve() {
  if (pwmLeftForward > 0 && pwmRightBackward > 0) {
    isRotationRight = true;
    //Serial.println("turn right");
  } else if (pwmLeftBackward > 0 && pwmRightForward > 0) {
    isRotationLeft = true;
    //Serial.println("turn left");
  } else if (pwmLeftForward > 0 && pwmRightForward > 0) {
    isRunForward = true;
    //Serial.println("run forward");
  } else if (pwmLeftBackward > 0 && pwmRightBackward > 0) {
    isRunBackward = true;
    //Serial.println("run backward");
  }

  /*
    Serial.print("pwmLeftForward:");
    Serial.print(pwmLeftForward);
    Serial.print(" pwmLeftBackward:");
    Serial.print(pwmLeftBackward);
    Serial.print(" pwmRightForward:");
    Serial.print(pwmRightForward);
    Serial.print(" pwmRightBackward:");
    Serial.print(pwmRightBackward);
    Serial.print(" inRotation:");
    Serial.println((isRotationRight || isRotationLeft) ? "true" : "false");
  */

  if (cameraState == 0) Serial.println("Camera disabled");
  else if (cameraState == 1) Serial.println("Camera enabled");
  if (rotationState == 0) Serial.println("Pulsed rotation disabled");
  else if (rotationState == 1) Serial.println("Pulsed rotation enabled");
  if (rotationTimeOn > 0) {
    Serial.print("Pulsed rotation on time:");
    Serial.println(rotationTimeOn);
  }
  if (rotationTimeOff > 0) {
    Serial.print("Pulsed rotation off time:");
    Serial.println(rotationTimeOff);
  }
  if (pwmLedsFront >= 0) {
    Serial.print("Leds front pwm:");
    Serial.println(pwmLedsFront);
  }
  if (disconnect) Serial.println("Disconnected by user");
  //Serial.print("data:");
  //Serial.println(characters);
}
