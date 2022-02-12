# DojoSignin
For improving the sign-in system used in our local club.

## NFC
Scans user's NFC tags and logs them in, using an ACR122U USB NFC scanner.

### Requirements
- Python 3
- [pyscard](https://pyscard.sourceforge.io/) (`pip install pyscard`)
    - [swig](http://www.swig.org/)
    - Microsoft Visual C++ 14.0 (if on windows)

Uses a modified version of [ACS-ACR122U-NFC-Reader](https://github.com/StevenTso/ACS-ACR122U-NFC-Reader/blob/master/NFCReader.py).

### Usage
`ReadNFC` is used to read the tags, and log them in. Just run it and start scanning tags to log them in. If you want to test the functionality without actually performing logins, run it with `test` as an argument, like `python ReadNFC.py test`

`WriteNFC` is used to assign a number to each tag when setting them up. Just run it, type the number you want, then scan the tag to write it.

Assigning users to tag numbers is done on the member website.

### Building
To build an executable, PyInstaller (`pip install pyinstaller`) is required.  
Simply run `pyinstaller ReadNFC.py` or `pyinstaller WriteNFC.py`.  
The resulting executable will be in the `dist` folder. All the other files in the folder are required, so make sure to copy those too.