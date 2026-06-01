from androguard.core.apk import APK
try:
    apk = APK("mojidict.apk")
    print("Package Name:", apk.get_package())
    
    manifest_xml = apk.get_android_manifest_xml()
    actions = set()
    schemes = set()
    
    for action in manifest_xml.iter("action"):
        actions.add(action.get("{http://schemas.android.com/apk/res/android}name"))
        
    for data in manifest_xml.iter("data"):
        scheme = data.get("{http://schemas.android.com/apk/res/android}scheme")
        if scheme:
            schemes.add(scheme)
            
    print("\nCustom Intent Actions found:")
    for a in sorted(actions):
        if a and ("moji" in a.lower() or "search" in a.lower() or "intent" in a.lower() or "text" in a.lower()):
            print("-", a)
            
    print("\nURL Schemes found:")
    for s in sorted(schemes):
        print("-", s)
        
except Exception as e:
    print("Error:", e)
