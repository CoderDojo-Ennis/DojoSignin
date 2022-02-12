import sys
from urllib.request import Request, urlopen
from urllib.error import HTTPError, URLError
import json
#import re
import NFCReader

#def cleanhtml(raw_html):
#    cleanr = re.compile('<.*?>')
#    cleantext = re.sub(cleanr, '', raw_html)
#    return cleantext

test = "false"
if (len(sys.argv) > 1):
    if ("test" in sys.argv[1]):
        print("WARNING: Testing mode is on. No logins will be recorded.")
        test = "true"

#try:
#    NFCReader.SetBuzzerOnRead(False)
#except:
#    print ("Buzzer modification won't work. Install the registry package")
print ("Ready to scan")
lastScan = None
while (True):
    data = NFCReader.readFourPages(4)
    if (data is None):
        continue
    if (len(data) != 32):
        continue
    header = data[:24]
    number = data[24:]
    #Check the "coderdojotag" header
    if (header != "636F646572646F6A6F746167"):
        print ("Error: Invalid tag header " + header)
        continue
    if (number == lastScan):
        continue
    lastScan = number
    memberID = int(number, 16)
    NFCReader.beep()
    print ("Scanned. Logging in...")
    url = "https://member.coderdojoennis.com/api/MemberAttendance/Fingerprint?id=" + str(memberID) + "&testing=" + test
    request = Request(url, None, {}, None, False, "POST")
    try:
        jsonText = urlopen(request).read().decode()
    except HTTPError:
        print ("No profile is registered to this tag!")
        continue
    except URLError:
        print ("Failed to connect! Is the Internet working?")
        continue
    print(json.loads(jsonText)["memberMessage"] + "\n")
    #print("\n")
    #print(cleanhtml(json.loads(jsonText)["memberMessage"]).replace(".", ".\n").replace("This is your", "\nThis is your").replace("Your last", "\nYour last"))