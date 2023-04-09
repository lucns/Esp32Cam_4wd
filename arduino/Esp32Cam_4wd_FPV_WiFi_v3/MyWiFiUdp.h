#ifndef _MYWIFIUDP_H_
#define _MYWIFIUDP_H_

#include <Arduino.h>
#include <Udp.h>
#include <cbuf.h>

//#define MAXIMUM_UDP_PAYLOAD 65507 // IPv4 maximum payload size 63,97kb with 28 header bytes from UDP protocol
//#define MAXIMUM_UDP_PAYLOAD 8192 // cause rebooting...
//#define MAXIMUM_UDP_PAYLOAD 1460
#define MAXIMUM_UDP_PAYLOAD 32768

class MyWiFiUdp : public UDP {
  private:
    int udp_server;
    IPAddress multicast_ip;
    IPAddress remote_ip;
    uint16_t server_port;
    uint16_t remote_port;
    char * tx_buffer;
    size_t tx_buffer_len;
    cbuf * rx_buffer;
  public:
    MyWiFiUdp();
    ~MyWiFiUdp();
    uint8_t begin(IPAddress a, uint16_t p);
    uint8_t begin(uint16_t p);
    uint8_t beginMulticast(IPAddress a, uint16_t p);
    void stop();
    int beginMulticastPacket();
    int beginPacket();
    int beginPacket(IPAddress ip, uint16_t port);
    int beginPacket(const char *host, uint16_t port);
    int endPacket();
    size_t write(uint8_t);
    size_t write(const uint8_t *buffer, size_t size);
    int parsePacket();
    int available();
    int read();
    int read(unsigned char* buffer, size_t len);
    int read(char* buffer, size_t len);
    int peek();
    void flush();
    IPAddress remoteIP();
    uint16_t remotePort();
};

#endif /* _MYWIFIUDP_H_ */
