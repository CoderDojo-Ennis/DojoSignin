/*************************************************** 
  This is an example sketch for our optical Fingerprint sensor

  Designed specifically to work with the Adafruit BMP085 Breakout 
  ----> http://www.adafruit.com/products/751

  These displays use TTL Serial to communicate, 2 pins are required to 
  interface
  Adafruit invests time and resources providing this open source code, 
  please support Adafruit and open-source hardware by purchasing 
  products from Adafruit!

  Written by Limor Fried/Ladyada for Adafruit Industries.  
  BSD license, all text above must be included in any redistribution
 ****************************************************/


#include <Adafruit_Fingerprint.h>
#include <SoftwareSerial.h>
#include "SoftwareSerial.h"
String ssid ="";

String password="";

SoftwareSerial esp(6, 7);

String data;

String server = "member.coderdojoennis.com"; 

String uri = "/api/MemberAttendance";


String id;

int getFingerprintIDez();

// pin #2 is IN from sensor (GREEN wire)
// pin #3 is OUT from arduino  (WHITE wire)
SoftwareSerial mySerial(2, 3);
Adafruit_Fingerprint finger = Adafruit_Fingerprint(&mySerial);

// On Leonardo/Micro or others with hardware serial, use those! #0 is green wire, #1 is white
//Adafruit_Fingerprint finger = Adafruit_Fingerprint(&Serial1);

void setup()  
{
  while (!Serial);  // For Yun/Leo/Micro/Zero/...
  
  Serial.begin(9600);
  Serial.println("Adafruit finger detect test");

  // set the data rate for the sensor serial port
  finger.begin(57600);
  
  if (finger.verifyPassword()) {
    Serial.println("Found fingerprint sensor!");
  } else {
    Serial.println("Did not find fingerprint sensor :(");
    while (1);
  }
  Serial.println("Waiting for valid finger...");
  esp.begin(9600);

  Serial.begin(9600);

  reset();

  connectWifi();
}
void reset() {

  esp.println("AT+RST");

  delay(1000);

  if(esp.find("OK") ) Serial.println("Module Reset");

}
void connectWifi() {

  String cmd = "AT+CWJAP=\"" +ssid+"\",\"" + password + "\"";

  esp.println(cmd);

  delay(4000);

  if(esp.find("OK")) {

  Serial.println("Connected!");

}else {
   connectWifi();
   Serial.println("Cannot connect to wifi"); }
}


void loop()                     // run over and over again
{
  getFingerprintIDez();
  delay(50);            //don't ned to run this at full speed.
}

uint8_t getFingerprintID() {
  uint8_t p = finger.getImage();
  switch (p) {
    case FINGERPRINT_OK:
      Serial.println("Image taken");
      break;
    case FINGERPRINT_NOFINGER:
      Serial.println("No finger detected");
      return p;
    case FINGERPRINT_PACKETRECIEVEERR:
      Serial.println("Communication error");
      return p;
    case FINGERPRINT_IMAGEFAIL:
      Serial.println("Imaging error");
      return p;
    default:
      Serial.println("Unknown error");
      return p;
  }

  // OK success!

  p = finger.image2Tz();
  switch (p) {
    case FINGERPRINT_OK:
      Serial.println("Image converted");
      break;
    case FINGERPRINT_IMAGEMESS:
      Serial.println("Image too messy");
      return p;
    case FINGERPRINT_PACKETRECIEVEERR:
      Serial.println("Communication error");
      return p;
    case FINGERPRINT_FEATUREFAIL:
      Serial.println("Could not find fingerprint features");
      return p;
    case FINGERPRINT_INVALIDIMAGE:
      Serial.println("Could not find fingerprint features");
      return p;
    default:
      Serial.println("Unknown error");
      return p;
  }
  
  // OK converted!
  p = finger.fingerFastSearch();
  if (p == FINGERPRINT_OK) {
    Serial.println("Found a print match!");
  } else if (p == FINGERPRINT_PACKETRECIEVEERR) {
    Serial.println("Communication error");
    return p;
  } else if (p == FINGERPRINT_NOTFOUND) {
    Serial.println("Did not find a match");
    return p;
  } else {
    Serial.println("Unknown error");
    return p;
  }   
  
  // found a match!
  Serial.print("Found ID #"); Serial.print(finger.fingerID); 
  Serial.print(" with confidence of "); Serial.println(finger.confidence); 
  id= String(finger.fingerID);
  data = "Fingerprint?id=" + id + "&testing=true";
  httppost();
  delay(1000);
  
}

void httppost () {

  esp.println("AT+CIPSTART=\"TCP\",\"" + server + "\",80");

  if( esp.find("OK")) {

    Serial.println("TCP connection ready");

  } 
  delay(1000);

  String postRequest =

  "POST " + uri + " HTTP/1.0\r\n" +

  "Host: " + server + "\r\n" +

  "Accept: *" + "/" + "*\r\n" +

  "Content-Length: " + data.length() + "\r\n" +

  "Content-Type: application/json; charset=utf-8" +

  "\r\n" + data;

  String sendCmd = "AT+CIPSEND=";

  esp.print(sendCmd);

  esp.println(postRequest.length() );

  delay(500);

  if(esp.find(">")) { Serial.println("Sending.."); esp.print(postRequest);}

  if( esp.find("SEND OK")) { Serial.println("Packet sent");}

  while (esp.available()) {


  }

  esp.println("AT+CIPCLOSE");
 }



// returns -1 if failed, otherwise returns ID #
int getFingerprintIDez() {
  uint8_t p = finger.getImage();
  if (p != FINGERPRINT_OK)  return -1;

  p = finger.image2Tz();
  if (p != FINGERPRINT_OK)  return -1;

  p = finger.fingerFastSearch();
  if (p != FINGERPRINT_OK)  return -1;
  
  // found a match!
  Serial.print("Found ID #"); Serial.print(finger.fingerID); 
  Serial.print(" with confidence of "); Serial.println(finger.confidence);
  id= String(finger.fingerID);
  data = "Fingerprint?id=" + id + "&testing=true";
  httppost();
  delay(1000);
  return finger.fingerID; 
}
