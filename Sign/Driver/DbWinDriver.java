package com.datadisplay.driver.dbwin;

import com.datadisplay.driver.*;
import com.datadisplay.transport.serial.*;
import javax.xml.parsers.*;
import com.datadisplay.xml.*;
import com.datadisplay.utils.*;
import javax.swing.*;
import java.awt.*;
import java.io.*;
import java.util.*;
import java.text.*;
import org.w3c.dom.*;
import org.w3c.dom.Element;



/** @todo need to remove the justification and replace it with alignment.

/**
 * <p>Title: XSDS</p>
 * <p>Description: Display Management Software</p>
 * <p>Copyright: Copyright (c) 2004</p>
 * <p>Company: Data Display Ltd</p>
 * @author not attributable
 * @version 1.0
 */

public class DbWinDriver extends GenericDriver implements DbWinDriverMBean {
  public static final int BUFFER_SIZE = 2048;
  
  //	Used for locilization of the application text and data.
  protected ResourceBundle resources = ResourceBundle.getBundle("Properties.displaydrivers.DisplayDriverPropertiesGen",
            Locale.getDefault());

  protected int bufferSize;
  protected byte protocolBuffer[]; // used to build up the protocol string.
  protected int lineStart;
  protected StringBuffer textBuffer = new StringBuffer();
  protected StringBuffer colorBuffer = new StringBuffer();
  protected StringBuffer colorSection = new StringBuffer();
  protected String currentColor;
  protected boolean flashOn = false;
  protected boolean flashOff = false;
  protected boolean ColorOn = false;

  DecimalFormat fmt00 = new DecimalFormat("00");

  public DbWinDriver(String name) {
    super(name);
  }

  public DbWinDriver() {
  }

  protected void initialize() throws Exception {
    super.initialize();
    statusBuffer.protocolStrings.setProperty("pageReset",CommandDefs.PAGE_RESET);
   // Create a new empty protocolString
    protocolBuffer = new byte[BUFFER_SIZE];
    //transport = new SerialAdapter("COM1");
    //reloadMode = true;
    setColor("default");
    byte framing[] = new byte[2];
    framing[0] = CommandDefs.START_RESPONSE;
    framing[1] = CommandDefs.END_RESPONSE;
    protocolFraming = new String(framing);
    statusBuffer.geometry.cols = statusBuffer.geometry.width/6;
  }

 /*%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%
    buffer update and append functions
  %%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%*/

  protected void clearProtocolBuffer() {
    protocolBuffer = new byte[BUFFER_SIZE];
    bufferSize = 0;
  }

  protected void appendProtocolHeader() {
     appendDisplayAddress();
  }

  protected void appendDisplayAddress() {
    append(CommandDefs.START_ADDRESS);
    append((byte)Integer.parseInt(displayAddress,16));
    append(CommandDefs.END_ADDRESS);
  }

  protected void appendPageHeader(int pageNum, DisplayProperties pageProps) {
    // Append the page header
    appendPageTiming(pageNum,pageProps);;
    append(CommandDefs.TEXT_MODE);
    append(CommandDefs.PAGE_SEQUENCE);
    append(fmt00.format(pageNum));
    setEffect(pageProps.effect);
    setPageFont(pageProps.font);
    if(pageProps.pause == 0)pageProps.pause = 1;
    append((byte)(0x30 + pageProps.pause));
    append(CommandDefs.END_SEQUENCE);

    colorSection = new StringBuffer();
    flashOn = false;
    flashOff = false;
    ColorOn = false;
  }

  protected void appendPageFooter(int pageNum, DisplayProperties pageProps) {
    append(CommandDefs.END_TEXT);
    if (ColorOn)
    	appendColorSection(pageNum);
  }

  protected void appendProtocolFooter() {
    appendCheckSum();
  }

  protected void appendColorSection(int pageNum) {
    append(CommandDefs.COLOR_SECTION);
    append(CommandDefs.PAGE_SEQUENCE);
    append(fmt00.format(pageNum));
    append(colorSection.toString());
    append(CommandDefs.END_TEXT);
  }

  protected void appendLineHeader(int lineNo, DisplayProperties lineProps) {
     lineStart = textBuffer.length();
     textBuffer = new StringBuffer();
     colorBuffer = new StringBuffer();
  }

  protected void appendLineFooter(int lineNo, DisplayProperties lineProps) {
    if(textBuffer.length() > statusBuffer.geometry.cols && !lineProps.effect.equals("scrollleft")) {
      textBuffer.setLength(statusBuffer.geometry.cols);
      colorBuffer.setLength(statusBuffer.geometry.cols);
    }
    else {
      int extra = statusBuffer.geometry.cols - textBuffer.length();
      if(lineProps.alignment.equals("left")) {
        fillRight(extra);
      }
      else if(lineProps.alignment.equals("right")) {
        fillLeft(extra);
      }
      else {
        int left = extra/2;
        fillLeft(left);
        fillRight(extra - left);
      }
    }

    append(textBuffer.toString());
    colorSection.append(colorBuffer);
  }

  protected void appendText(String string) {
    /** @todo need to check for control characters in the text. */
    int i = 0;
    if(flashOff && textBuffer.length() > 0) {
      if (textBuffer.charAt(textBuffer.length() - 1) == ' ') {
        textBuffer.setCharAt(textBuffer.length() - 1, '*');
        flashOff = false;
      }
    }

    if(flashOn)i = 1;
    else if(flashOff) {
      textBuffer.append('*');
      colorBuffer.append(currentColor);
      i = 1;
    }
    for(;i < string.length();i++) {
      textBuffer.append(string.charAt(i));
      colorBuffer.append(currentColor);
    }
    flashOn = false;
    flashOff = false;
  }


  protected void appendFlashOn() {
    if(!flashOff) {
      if(textBuffer.length() > 0 && textBuffer.charAt(textBuffer.length() -1) == ' ') {
        textBuffer.setCharAt(textBuffer.length() -1,'*');
      }
      else {
        flashOn = true;
        textBuffer.append('*');
        colorBuffer.append(currentColor);
      }
    }
    else { // This means the previous line was flashing  so we dont need to do anything

      flashOff = false;
    }
  }

  protected void appendFlashOff() {
    // Defer the switching off of flashing till we get the next line or block of text
    flashOff = true;
  }

  SimpleDateFormat dateFmt = new SimpleDateFormat("MMdd");
  SimpleDateFormat timeFmt = new SimpleDateFormat("HHmm");

  protected void appendPageTiming(int pageNum,  DisplayProperties props) {
    if(props.schedule == null)return;
    append(CommandDefs.TIMED_TEXT_MODE);
    append(CommandDefs.PAGE_SEQUENCE);
    append(fmt00.format(pageNum));
    for(int i =0;i<props.schedule.length;i++){
      TimingProperties sch = props.schedule[i];
      if(props.schedule[i] == null) {
        append("******************");
      }
      else {
        String startTimeString = timeFmt.format(sch.startTime);
        String stopTimeString = timeFmt.format(sch.stopTime);
        if(startTimeString.equals(stopTimeString)) {
           startTimeString = "****";
           stopTimeString = "****";
        }
        StringBuffer buf = new StringBuffer();
        if(sch.startDate != null)buf.append(dateFmt.format(sch.startDate));
        else buf.append("****");
        buf.append(startTimeString);
        if(sch.stopDate != null)buf.append(dateFmt.format(sch.stopDate));
        else buf.append("****");
        buf.append(stopTimeString);
        if(sch.days != null && !sch.days.equals("")) {
          buf.append(sch.days);
        }
        else buf.append("**");

        append(buf.toString());
      }
    }
    append(CommandDefs.END_TEXT);
  }


  protected void fillLeft(int amount) {
    /** @todo need to check for control characters in the text. */
    for(int i = 0;i < amount;i++) {
      textBuffer.insert(0," ");
      colorBuffer.insert(0,currentColor);
    }
  }

  protected void fillRight(int amount) {
    /** @todo need to check for control characters in the text. */
    for(int i = 0;i < amount;i++) {
      textBuffer.append(" ");
      colorBuffer.append(currentColor);
    }
  }

  protected void appendEscape(String string) {
    appendText(string);
  }

  protected void appendCommand(String string) {
     append(string);
  }

  protected void appendCheckSum() {
   // Generate the checksum
    append(CommandDefs.CHECKSUM_START);
    append((byte)0xAA);
    append((byte)0xAA);
    append(CommandDefs.CHECKSUM_END);
  }


  protected void setEffect(String effect) {
    append(statusBuffer.effects.getProperty(effect));
  }

  protected void setPageFont(String font) {
    FontDescription fDesc = (FontDescription)statusBuffer.fonts.get(font);
    append(fDesc.protocolString);
  }


  protected void setColor(String color) {
    ColorDescription cDesc = (ColorDescription)statusBuffer.colors.get(color);
    currentColor = cDesc.protocolString;
    ColorOn = true;
 }
 protected void setFont(String font){
   // We dont support font setting within a page
 }

  protected void setAlignment(String alignment) {
    append(CommandDefs.SETJUSTIFICATION);
    append(statusBuffer.aligns.getProperty(alignment));
  }

  protected void append(String var) {
    for(int i = 0;i < var.length();i++) {
      append((byte)var.charAt(i));
    }

  }

  protected void append(byte chr) {
    protocolBuffer[bufferSize++] = chr;

  }

 /** This Driver does not support display modes */
  protected boolean isValidDisplayMode(String value) {
    return true;
  }

  protected void sendToDisplay() throws Exception {
    displayResponse = null;
    byte buf[] = new byte[bufferSize];
    for(int i = 0;i < bufferSize;i++) {
      buf[i] = (byte)protocolBuffer[i];
    }
    displayResponse = transport.sendToDisplay(buf,protocolFraming,
                                              Integer.parseInt(statusBuffer.comms.getProperty("timeout")));
  }

  protected void imageToProtocol(int index){
  }


  protected boolean isAckResponse() {
    if(blindMode)return true;
    if(displayResponse == null || displayResponse.length != 4) {
      responseError = resources.getString("NO_ACK");
      return false;
    }
    if(displayResponse[0] == 1 && displayResponse[2] == 6 &&
       displayResponse[3] == 3)return true;
    responseError = resources.getString("NEG_ACK");
    return false;
  }


  protected void parseGetResponse(MibObject obj) {
  }
}



class CommandDefs {
  // Packet format commands
  public static final byte START_ADDRESS   = 0x0F;
  public static final byte ADDRESS_BYTE    = 0x20;
  public static final byte END_ADDRESS     = 0x0E;
  public static final byte TEXT_MODE       = 0x02;
  public static final byte TIMED_TEXT_MODE = 0x04;
  public static final byte PAGE_SEQUENCE   = 0x5C;
  public static final byte END_SEQUENCE    = 0x3A;
  public static final byte END_TEXT        = 0x03;
  public static final byte COLOR_SECTION   = 0x11;
  public static final byte CHECKSUM_START  = 0x08;
  public static final byte CHECKSUM_END    = 0x09;
  public static final byte START_RESPONSE  = 0x01;
  public static final byte END_RESPONSE    = 0x03;
  // Page Commands
  public static final String PAGE_STORE = "\\V";
  public static final String DELETE_PAGE = "\\S-";
  public static final String WIPE_PAGE = "\\SW";
  public static final String PAGE_RESET = "\u0010\u0003";
  public static final String SETEFFEXT = "\\X";
  public static final String SETJUSTIFICATION  = "\\J";
  public static final String PAUSE = "\\H";

  public static final String PAGE_SEPERATOR = ":";
}
