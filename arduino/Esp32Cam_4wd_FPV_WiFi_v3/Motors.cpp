#include "Motors.h"

Motors::Motors() {}

void Motors::begin() {
  pinMode(MOTOR_LEFT_FORWARD, OUTPUT);
  pinMode(MOTOR_LEFT_BACKWARD, OUTPUT);
  pinMode(MOTOR_RIGHT_FORWARD, OUTPUT);
  pinMode(MOTOR_RIGHT_BACKWARD, OUTPUT);
  disable();

  rotationTimeOn = 50;
  rotationTimeOff = 50;
  delayMotorsLeft.setTime(250);
  delayMotorsRight.setTime(250);
  delayRotation.setTime(50);
}

void Motors::applyMotorsCommands() {
  if (dataModel.pwmLeftForward > 0) {
    delayMotorsLeft.reset();
    Serial.println("Motors left on forward");
    analogWrite(MOTOR_LEFT_BACKWARD, 0);
    analogWrite(MOTOR_LEFT_FORWARD, dataModel.pwmLeftForward);
  } else if (dataModel.pwmLeftBackward > 0) {
    Serial.println("Motors left on backward");
    delayMotorsLeft.reset();
    analogWrite(MOTOR_LEFT_FORWARD, 0);
    analogWrite(MOTOR_LEFT_BACKWARD, dataModel.pwmLeftBackward);
  } else {
    Serial.println("Motors left off");
    delayMotorsLeft.lock();
    analogWrite(MOTOR_LEFT_BACKWARD, 0);
    analogWrite(MOTOR_LEFT_FORWARD, 0);
  }

  if (dataModel.pwmRightForward > 0) {
    Serial.println("Motors right on forward");
    delayMotorsRight.reset();
    analogWrite(MOTOR_RIGHT_BACKWARD, 0);
    analogWrite(MOTOR_RIGHT_FORWARD, dataModel.pwmRightForward);
  } else if (dataModel.pwmRightBackward > 0) {
    Serial.println("Motors right on backward");
    delayMotorsRight.reset();
    analogWrite(MOTOR_RIGHT_FORWARD, 0);
    analogWrite(MOTOR_RIGHT_BACKWARD, dataModel.pwmRightBackward);
  } else {
    Serial.println("Motors right off");
    delayMotorsRight.lock();
    analogWrite(MOTOR_RIGHT_BACKWARD, 0);
    analogWrite(MOTOR_RIGHT_FORWARD, 0);
  }
}

void Motors::putData(DataModel data) {
  if (data.rotationState == 1) pulsedRotation = true;
  else if (data.rotationState == 0) pulsedRotation = false;
  if (data.disconnect) {
    delayRotation.lock();
    disable();
    return;
  }
  if (data.rotationTimeOn > 0) rotationTimeOn = data.rotationTimeOn;
  if (data.rotationTimeOff > 0) rotationTimeOff = data.rotationTimeOff;
  if (pulsedRotation) {
    bool wasRotation = dataModel.isRotationLeft || dataModel.isRotationRight;
    dataModel = data;
    if (data.isRotationLeft || data.isRotationRight) {
      if (!wasRotation) {
        isRotation = true;
        rotationState = false;
        delayRotation.setTime(rotationTimeOn);
        delayRotation.reset();
      } else {
        delayMotorsLeft.reset();
        delayMotorsRight.reset();
        return;
      }
    } else {
      isRotation = false;
      delayRotation.lock();
    }
  }
  dataModel = data;
  applyMotorsCommands();
}

void Motors::compute() {
  if (dataModel.isRotationLeft || dataModel.isRotationRight) {
    if (delayRotation.gate()) {
      //Serial.print("data:");
      //Serial.println(dataModel.characters);
      rotationState = !rotationState;
      if (rotationState) {
        Serial.println("Motors all disable in rotation");
        analogWrite(MOTOR_LEFT_BACKWARD, 0);
        analogWrite(MOTOR_LEFT_FORWARD, 0);
        analogWrite(MOTOR_RIGHT_BACKWARD, 0);
        analogWrite(MOTOR_RIGHT_FORWARD, 0);
        delayRotation.setTime(rotationTimeOff);
      } else {
        Serial.println("Motors all enable in rotation");
        applyMotorsCommands();
        delayRotation.setTime(rotationTimeOn);
      }
      delayRotation.reset();
    }
  }
  if (delayMotorsLeft.gate()) {
    Serial.println("Motors left disable timeout");
    analogWrite(MOTOR_LEFT_BACKWARD, 0);
    analogWrite(MOTOR_LEFT_FORWARD, 0);
  }
  if (delayMotorsRight.gate()) {
    Serial.println("Motors right disable timeout");
    analogWrite(MOTOR_RIGHT_BACKWARD, 0);
    analogWrite(MOTOR_RIGHT_FORWARD, 0);
  }
}

void Motors::disable() {
  Serial.println("Motors disable");
  analogWrite(MOTOR_RIGHT_BACKWARD, 0);
  analogWrite(MOTOR_RIGHT_FORWARD, 0);
  analogWrite(MOTOR_LEFT_BACKWARD, 0);
  analogWrite(MOTOR_LEFT_FORWARD, 0);
  delayMotorsLeft.lock();
  delayMotorsRight.lock();
}
