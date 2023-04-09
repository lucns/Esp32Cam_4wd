#include "DataModel.h"
#include <Arduino.h>

class DataProvider {
  private:
    int charToInt(char c);
    int elevate(int base, int expoent);
    int getPwm(char c[], int i);
  public:
    DataProvider();
    DataModel retrieve(char data[]);
};
