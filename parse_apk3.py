from androguard.core.apk import APK
try:
    apk = APK("mojidict.apk")
    manifest_xml = apk.get_android_manifest_xml()
    
    for activity in manifest_xml.iter("activity"):
        name = activity.get("{http://schemas.android.com/apk/res/android}name")
        for intent_filter in activity.iter("intent-filter"):
            has_match = False
            
            for action in intent_filter.iter("action"):
                action_name = action.get("{http://schemas.android.com/apk/res/android}name")
                if action_name == "com.mojitec.action.MOJIDICT":
                    has_match = True
                    break
                    
            for data in intent_filter.iter("data"):
                scheme = data.get("{http://schemas.android.com/apk/res/android}scheme")
                if scheme == "mojisho":
                    has_match = True
                    break
            
            if has_match:
                print("Activity:", name)
                for action in intent_filter.iter("action"):
                    print("  Action:", action.get("{http://schemas.android.com/apk/res/android}name"))
                for data in intent_filter.iter("data"):
                    print("  Data: scheme=", data.get("{http://schemas.android.com/apk/res/android}scheme"), 
                          "host=", data.get("{http://schemas.android.com/apk/res/android}host"),
                          "pathPrefix=", data.get("{http://schemas.android.com/apk/res/android}pathPrefix"))
                print()
                
except Exception as e:
    print("Error:", e)
