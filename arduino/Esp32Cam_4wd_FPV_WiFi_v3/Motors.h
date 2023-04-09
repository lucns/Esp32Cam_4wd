#include <Arduino.h>
#include <Delay.h>
#include "DataModel.h";

#define MOTOR_LEFT_FORWARD 12
#define MOTOR_LEFT_BACKWARD 13
#define MOTOR_RIGHT_FORWARD 14
#define MOTOR_RIGHT_BACKWARD 15

class Motors {
  private:
    Delay delayMotorsLeft, delayMotorsRight, delayRotation;
    DataModel dataModel;
    bool isRotation, rotationState, pulsedRotation;
    void applyMotorsCommands();
    long rotationTimeOn, rotationTimeOff;
  public:
    Motors();
    void begin();
    void putData(DataModel dataModel);
    void compute();
    void disable();
};
