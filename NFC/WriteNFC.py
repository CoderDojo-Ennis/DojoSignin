import NFCReader

print ("Type 'exit' as the tag number to exit")
while(True):
    inputText = input("Enter tag number: ")
    if ("exit" in inputText):
        break
    try:
        number = int(inputText)
    except ValueError:
        print("That's not a number!")
        continue
    print ("Scan tag now")
    #Writes the ASCII of "coderdojotag" to the first 3 pages
    NFCReader.writeTag(4, "636F6465")
    NFCReader.writeTag(5, "72646F6A")
    NFCReader.writeTag(6, "6F746167")
    #Converts the number to a 8 digit hex string
    numHexString = "%0.8X" % number
    #Writes the inputted number to page 7
    NFCReader.writeTag(7, numHexString)
    print ("Successfully wrote tag")