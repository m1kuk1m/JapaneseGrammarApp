from androguard.core.apk import APK
try:
    apk = APK("mojidict.apk")
    manifest_xml = apk.get_android_manifest_xml()
    
    for activity in manifest_xml.iter("activity"):
        name = activity.get("{http://schemas.android.com/apk/res/android}name")
        if "FacadeActivity" in name:
            for intent_filter in activity.iter("intent-filter"):
                print("Activity:", name)
                for action in intent_filter.iter("action"):
                    print("  Action:", action.get("{http://schemas.android.com/apk/res/android}name"))
                for data in intent_filter.iter("data"):
                    print("  Data:")
                    for k, v in data.items():
                        print("   ", k, "=", v)
                print()
                
except Exception as e:
    print("Error:", e)
