#include "DataProvider.h"

DataProvider::DataProvider() {}

int DataProvider::charToInt(char c) {
  switch (c) {
    case '0': return 0;
    case '1': return 1;
    case '2': return 2;
    case '3': return 3;
    case '4': return 4;
    case '5': return 5;
    case '6': return 6;
    case '7': return 7;
    case '8': return 8;
    case '9': return 9;
    default:
      return -1;
  }
}

int DataProvider::elevate(int base, int expoent) {
  if (expoent == 0) return 1;
  int result = base;
  while (expoent > 1) {
    result = base * result;
    expoent--;
  }
  return result;
}

int DataProvider::getPwm(char c[], int i) {
  int index = i;
  int e = 0;
  while (c[index] != '\0') {
    e++;
    index++;
  }
  e--;
  int pwm = 0;
  while (c[i] != '\0') {
    pwm += charToInt(c[i]) * elevate(10, e);
    i++;
    e--;
  }
  return pwm;
}

DataModel DataProvider::retrieve(char data[]) { // lf1023 rb512 g123 h0 a
  //Serial.print("retrieve:");
  //Serial.println(data);
  int index = 0;
  int size = 1;
  while (data[index] != '\0') {
    if (data[index] == ' ') size++;
    index++;
  }
  char commands[size][8];
  index = 0;
  int characterPosition = 0;
  int arrayPosition = 0;
  while (data[index] != '\0') {
    if (data[index] == ' ') {
      commands[arrayPosition][characterPosition] = '\0';
      characterPosition = 0;
      arrayPosition++;
      index++;
      continue;
    }
    commands[arrayPosition][characterPosition] = data[index];
    characterPosition++;
    index++;
  }
  commands[arrayPosition][characterPosition] = '\0';
  arrayPosition = 0;
  characterPosition = 0;
  int pwm;
  char next;
  DataModel dataModel;
  dataModel.characters = data;
  while (arrayPosition < size) {
    characterPosition = 0;
    while (commands[arrayPosition][characterPosition] != '\0') {
      if (isDigit(commands[arrayPosition][characterPosition])) break;

      switch (commands[arrayPosition][characterPosition]) {
        case 'l':
          characterPosition++;
          if (commands[arrayPosition][characterPosition] == 'f') {
            characterPosition++;
            pwm = getPwm(commands[arrayPosition], characterPosition);
            //Serial.print("left forward pwm:");
            //Serial.println(pwm);
            dataModel.pwmLeftBackward = 0;
            dataModel.pwmLeftForward = pwm;
          } else if (commands[arrayPosition][characterPosition] == 'b') {
            characterPosition++;
            pwm = getPwm(commands[arrayPosition], characterPosition);
            //Serial.print("left backward pwm:");
            //Serial.println(pwm);
            dataModel.pwmLeftBackward = pwm;
            dataModel.pwmLeftForward = 0;
          } else if (commands[arrayPosition][characterPosition] == 'd') { // disable
            //Serial.println("left disable");
            dataModel.pwmLeftBackward = 0;
            dataModel.pwmLeftForward = 0;
          }
          break;
        case 'r':
          characterPosition++;
          if (commands[arrayPosition][characterPosition] == 'f') {
            characterPosition++;
            pwm = getPwm(commands[arrayPosition], characterPosition);
            //Serial.print("right forward pwm:");
            //Serial.println(pwm);
            dataModel.pwmRightBackward = 0;
            dataModel.pwmRightForward = pwm;
          } else if (commands[arrayPosition][characterPosition] == 'b') {
            characterPosition++;
            pwm = getPwm(commands[arrayPosition], characterPosition);
            //Serial.print("right backward pwm:");
            //Serial.println(pwm);
            dataModel.pwmRightBackward = pwm;
            dataModel.pwmRightForward = 0;
          } else if (commands[arrayPosition][characterPosition] == 'd') { // disable
            //Serial.println("right disable");
            dataModel.pwmRightBackward = 0;
            dataModel.pwmRightForward = 0;
          }
          break;
        case 'g':
          characterPosition++;
          pwm = getPwm(commands[arrayPosition], characterPosition);
          //Serial.print("front leds pwm:");
          //Serial.println(pwm);
          dataModel.pwmLedsFront = pwm;
          break;
        case 't':
          characterPosition++;
          next = commands[arrayPosition][characterPosition];
          characterPosition++;
          pwm = getPwm(commands[arrayPosition], characterPosition);
          //Serial.print("front leds pwm:");
          //Serial.println(pwm);
          if (next == 'n') dataModel.rotationTimeOn = pwm;
          else if (next == 'f') dataModel.rotationTimeOff = pwm;
          break;
        case 'c':
          characterPosition++;
          if (commands[arrayPosition][characterPosition] == '1') {
            dataModel.cameraState = 1;
            //Serial.println("camera enabled");
          } else if (commands[arrayPosition][characterPosition] == '0') {
            dataModel.cameraState = 0;
            //Serial.println("camera disabled");
          }
          break;
        case 'p':
          characterPosition++;
          if (commands[arrayPosition][characterPosition] == '1') {
            dataModel.rotationState = 1;
            //Serial.println("pulsed rotation enabled");
          } else if (commands[arrayPosition][characterPosition] == '0') {
            dataModel.rotationState = 0;
            //Serial.println("pulsed rotation disabled");
          }
          break;
        case 'z':
          //Serial.println("disconnected by user");
          dataModel.disconnect = true;
          break;
        default:
          dataModel.invalidCharacters = true;
          //Serial.print("computeData default: ");
          //Serial.println(commands[arrayPosition][characterPosition]);
          break;
      }
      characterPosition++;
    }
    arrayPosition++;
  }
  dataModel.retrieve();
  return dataModel;
}
