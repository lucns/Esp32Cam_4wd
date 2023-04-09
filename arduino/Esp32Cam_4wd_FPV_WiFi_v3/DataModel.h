#ifndef MOTORS_H
#define MOTORS_H

#include <Arduino.h>

class DataModel {
  public:
    DataModel();
    char* characters;
    void retrieve();
    int pwmLeftForward, pwmLeftBackward, pwmRightForward, pwmRightBackward;
    int pwmLedsFront, cameraState, rotationState;
    long rotationTimeOn, rotationTimeOff;
    bool disconnect, invalidCharacters, isRunForward, isRunBackward, isRotationLeft, isRotationRight;
};
#endif
