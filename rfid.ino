//www.elegoo.com
//2016.06.13
#define red 22
#define green 23
long x;
int y;
/*
 * --------------------------------------------------------------------------------------------------------------------
 * Example to change UID of changeable MIFARE card.
 * --------------------------------------------------------------------------------------------------------------------
 * This is a MFRC522 library example; for further details and other examples see: https://github.com/miguelbalboa/rfid
 * 
 * This sample shows how to set the UID on a UID changeable MIFARE card.
 * NOTE: for more informations read the README.rst
 * 
 * @author Tom Clement
 * @license Released into the public domain.
 *
 * Typical pin layout used:
 * -----------------------------------------------------------------------------------------
 *             MFRC522      Arduino       Arduino   Arduino    Arduino          Arduino
 *             Reader/PCD   Uno           Mega      Nano v3    Leonardo/Micro   Pro Micro
 * Signal      Pin          Pin           Pin       Pin        Pin              Pin
 * -----------------------------------------------------------------------------------------
 * RST/Reset   RST          9             5         D9         RESET/ICSP-5     RST
 * SPI SS      SDA(SS)      10            53        D10        10               10
 * SPI MOSI    MOSI         11 / ICSP-4   51        D11        ICSP-4           16
 * SPI MISO    MISO         12 / ICSP-1   50        D12        ICSP-1           14
 * SPI SCK     SCK          13 / ICSP-3   52        D13        ICSP-3           15
 */

#include <SPI.h>
#include <MFRC522.h>

#define RST_PIN   5     // Configurable, see typical pin layout above
#define SS_PIN    53    // Configurable, see typical pin layout above

MFRC522 mfrc522(SS_PIN, RST_PIN);   // Create MFRC522 instance

/* Set your new UID here! */
#define NEW_UID {0xDE, 0xAD, 0xBE, 0xEF}

MFRC522::MIFARE_Key key;

void setup() {
  pinMode(23, OUTPUT);
  pinMode(22, OUTPUT);
  Serial.begin(9600); 
   
  SPI.begin();         // Init SPI bus
  mfrc522.PCD_Init();  // Init MFRC522 card
  Serial.println(F("init"));
  
  
  for (byte i = 0; i < 6; i++) {
    key.keyByte[i] = 0xFF;
  }
}

// Setting the UID can be as simple as this:
//void loop() {
//  byte newUid[] = NEW_UID;
//  if ( mfrc522.MIFARE_SetUid(newUid, (byte)4, true) ) {
//    Serial.println("Wrote new UID to card.");
//  }
//  delay(1000);
//}


void loop() {
  
  
  if ( ! mfrc522.PICC_IsNewCardPresent() || ! mfrc522.PICC_ReadCardSerial() ) {
    delay(50);
    return;
  }
  
  
  
  
     x = mfrc522.uid.uidByte[3];
    Serial.println(x);
     
    check();
    
   
    
  } 
  

void check() {
  y=x;
  if(y=117) {
   redgo();
  
}else if (y=213) {
  greengo();
}
}

void greengo() {
  digitalWrite(23, HIGH);
  digitalWrite(22, LOW);
}
void redgo() {
  digitalWrite(22, HIGH);
  digitalWrite(23, LOW);
}

   
 
