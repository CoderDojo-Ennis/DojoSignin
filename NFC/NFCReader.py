#! /usr/bin/env python
import re#, argparse
from smartcard.System import readers
from smartcard.scard import *
import datetime, sys

#ACS ACR122U NFC Reader
#Suprisingly, to get data from the tag, it is a handshake protocol
#You send it a command to get data back
#This command below is based on the "API Driver Manual of ACR122U NFC Contactless Smart Card Reader"
COMMAND = [0xFF, 0xCA, 0x00, 0x00, 0x00] #handshake cmd needed to initiate data transfer

# get all the available readers
r = readers()
print("Available readers:", r)

def stringParser(dataCurr):
#--------------String Parser--------------#
    #([85, 203, 230, 191], 144, 0) -> [85, 203, 230, 191]
    if isinstance(dataCurr, tuple):
        temp = dataCurr[0]
        code = dataCurr[1]
    #[85, 203, 230, 191] -> [85, 203, 230, 191]
    else:
        temp = dataCurr
        code = 0

    dataCurr = ''

    #[85, 203, 230, 191] -> bfe6cb55 (int to hex reversed)
    for val in temp:
        # dataCurr += (hex(int(val))).lstrip('0x') # += bf
        dataCurr += format(val, '#04x')[2:] # += bf

    #bfe6cb55 -> BFE6CB55
    dataCurr = dataCurr.upper()

    #if return is successful
    if (code == 144):
        return dataCurr

#Reads one page of the tag and returns the hex string
def readTag(page):
    readingLoop = 1
    while(readingLoop):
        try:
            #connection = reader.createConnection()
            status_connection = connection.connect()
            connection.transmit(COMMAND)
            #Read command [FF, B0, 00, page, #bytes]
            resp = connection.transmit([0xFF, 0xB0, 0x00, int(page), 0x04])
            dataCurr = stringParser(resp)

            #only allows new tags to be worked so no duplicates
            if(dataCurr is not None):
                return (dataCurr)
                break
            else:
                print ("Something went wrong on page " + str(page) + " Please try again")
                break
        except Exception as e: 
            if(waiting_for_beacon ==1):
                continue
            else:
                readingLoop=0
                print (str(e))
                break
    return None

#Writes one page of the tag - a 4 byte hex string
def writeTag(page, value):
    if type(value) != str:
        print ("Value input should be a string")
        exit()
    while(1):
        if len(value) == 8:
            try:
                #connection = reader.createConnection()
                status_connection = connection.connect()
                connection.transmit(COMMAND)
                WRITE_COMMAND = [0xFF, 0xD6, 0x00, int(page), 0x04, int(value[0:2], 16), int(value[2:4], 16), int(value[4:6], 16), int(value[6:8], 16)]
                # Let's write a page Page 9 is usually 00000000
                resp = connection.transmit(WRITE_COMMAND)
                if resp[1] == 144:
                    print("Wrote " + value + " to page " + str(page))
                    break
            except Exception as e:
                continue
        else:
            print ("Must have a full 4 byte write value")
            break

#Read multiple pages
def readTagPages(startPage, numPages):
    readingLoop = 1
    while(readingLoop):
        totalData = ""
        try:
            #connection = reader.createConnection()
            status_connection = connection.connect()
            for i in range(numPages):
                connection.transmit(COMMAND)
                #Read command [FF, B0, 00, page, #bytes]
                resp = connection.transmit([0xFF, 0xB0, 0x00, startPage + i, 0x04])
                dataCurr = stringParser(resp)

                #only allows new tags to be worked so no duplicates
                if(dataCurr is not None):
                    totalData += dataCurr
                else:
                    #print ("Something went wrong on page " + str(startPage + i) + " Please try again")
                    break
            return totalData
        except Exception as e: 
            if(waiting_for_beacon ==1):
                continue
            else:
                readingLoop=0
                print (str(e))
                break
    return None
    
#Reads 4 pages with 1 command. Should be faster, and it's convenient if you only need 4 pages, as 4 is the max that can be done at once.
def readFourPages(startPage):
    readingLoop = 1
    while(readingLoop):
        try:
            status_connection = connection.connect()
            #if (firstScan):
            #send the command to disable the beeps
            connection.transmit([0xFF, 0x00, 0x52, 0x00, 0x00])
            #firstScan = False
            connection.transmit(COMMAND)
            #Read command [FF, B0, 00, page, #bytes]
            resp = connection.transmit([0xFF, 0xB0, 0x00, startPage, 0x10])
            dataCurr = stringParser(resp)

            #only allows new tags to be worked so no duplicates
            if(dataCurr is not None):
                return dataCurr
            else:
                print ("Something went wrong while reading. Please try again")
                break
        except Exception as e: 
            if(waiting_for_beacon ==1):
                continue
            else:
                readingLoop=0
                print (str(e))
                break
    return None

def beep():
    connection.transmit([0xFF, 0x00, 0x40, 0x01, 0x04, 0x02, 0x00, 0x01, 0x01])

#Controls if the buzzer goes off when a tag is presented.
#def SetBuzzerOnRead(status):
#    out = (0xFF if status else 0x00)
#    hresult, hcontext = SCardEstablishContext(SCARD_SCOPE_USER)
#    if hresult != SCARD_S_SUCCESS:
#        raise error('SCardEstablishContext(): ' + SCardGetErrorMessage(hresult))
#    hresult, nfcreaders = SCardListReaders(hcontext, [])
#    if hresult != SCARD_S_SUCCESS:
#        raise error('SCardListReaders(): ' + SCardGetErrorMessage(hresult))
#    r = nfcreaders[0]
#    hresult, hcard, dwActiveProtocol = SCardConnect(hcontext, r, SCARD_SHARE_DIRECT, 0)
#    if hresult != SCARD_S_SUCCESS:
#        raise error('SCardConnect(): ' + SCardGetErrorMessage(hresult))
#    hresult, response = SCardControl(hcard,SCARD_CTL_CODE(3500),[0xFF, 0x00, 0x52, out, 00])
#    if hresult != SCARD_S_SUCCESS:
#        raise error('SCardControl(): ' + SCardGetErrorMessage(hresult))
#    SCardDisconnect(hcard, SCARD_LEAVE_CARD)
#    SCardReleaseContext(hcontext)
#    print (response)

if (len(r) == 0):
    print ("No NFC reader is plugged in!")
    sys.exit()

firstScan = True
reader = r[0]
connection = reader.createConnection()
waiting_for_beacon = 1
print ("Connected to " + str(reader))
'''
if __name__ == "__main__":
    parser = argparse.ArgumentParser(description='Read / write NFC tags')
    usingreader_group = parser.add_argument_group('usingreader')
    usingreader_group.add_argument('--usingreader', nargs=1, metavar='READER_ID', help='Reader to use [0-X], default is 0')
    wait_group = parser.add_argument_group('wait')
    wait_group.add_argument('--wait', nargs=1, metavar='0|1', help='Wait for beacon before returns [0|1], default is 1')
    read_group = parser.add_argument_group('read')
    read_group.add_argument('--read', nargs='+', help='Pages to read. Can be a x-x range, or list of pages')
    write_group = parser.add_argument_group('write')
    write_group.add_argument('--write', nargs=2, metavar=('PAGE', 'DATA'), help='Page number and hex value to write.')


    args = parser.parse_args()

    #Choosing which reader to use
    if args.usingreader:
        usingreader = args.usingreader[0]
        if (int(usingreader) >= 0 and int(usingreader) <= len(r)-1):
            reader = r[int(usingreader)]
        else:
            reader = r[0]
    else:
        reader = r[0]

    print ("Using:", reader)

    #Disabling wait for answer if wait == 0
    if args.wait:
        wait = args.wait[0]
        if (int(wait) == 0 ):
            waiting_for_beacon = 0
        else:
            waiting_for_beacon = 1
    else:
        waiting_for_beacon = 1

    print ("Using:", reader)

    #Only going to write one page at a time
    if args.write:
        page = args.write[0]
        data = args.write[1]
        if len(data) == 8:
            writeTag(int(page), data)
        else:
            raise argparse.ArgumentTypeError("Must have a full 4 byte write value")
    
    #Page numbers are sent as ints, not hex, to the reader
    if args.read:
        for page in args.read:
            if "-" in page:
                start = int(page.split("-")[0])
                end = int(page.split("-")[1])
                for new_page in xrange(start, end+1):
                    readTag(new_page)
            else:
                readTag(page)
'''