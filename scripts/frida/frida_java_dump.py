import frida
import json
import time

results = {}
kEpRxMC_data = {}

def on_message(message, data):
    if message['type'] == 'send':
        payload = message['payload']
        if payload.startswith("kEpRxMC: "):
            try:
                kEpRxMC_data.update(json.loads(payload.replace("kEpRxMC: ", "")))
                print(f"kEpRxMC: got {len(kEpRxMC_data)} fields")
            except: print(f"kEpRxMC parse error: {payload[:100]}")
        elif payload.startswith("NULL_CLASSES: "):
            try:
                obj = json.loads(payload.replace("NULL_CLASSES: ", ""))
                for cls, fields_list in obj.items():
                    results[cls] = fields_list
                print(f"Found {len(results)} classes with null strings")
            except: print(f"Parse error: {payload[:200]}")
        elif payload.startswith("kEpRxMC_ERR") or payload.startswith("ENUM_ERR"):
            print(f"Error: {payload}")
        else:
            print(f"MSG: {payload[:200]}")
    elif message['type'] == 'error':
        print(f"SCRIPT ERROR: {message.get('description', '')[:300]}")

device = frida.get_usb_device()

# Use attach to already-running process (launch app first, then attach quickly)
import subprocess
subprocess.run(["adb", "shell", "am", "start", "-n", "com.jio.jioplay.tv/com.jio.media.tv.ui.permission_onboarding.PermissionActivity"], 
               capture_output=True)
time.sleep(3)

# Get PID
result = subprocess.run(["adb", "shell", "pidof", "com.jio.jioplay.tv"], capture_output=True, text=True)
pid = int(result.stdout.strip())
print(f"Attaching to PID: {pid}")

try:
    session = device.attach(pid)
except Exception as e:
    print(f"Attach failed: {e}")
    sys.exit(1)

with open("/tmp/pairip-dump/dump_when_ready.js", "r") as f:
    script_code = f.read()

script = session.create_script(script_code)
script.on('message', on_message)
script.load()
print("Script loaded, waiting for dump...")

time.sleep(10)

print(f"\n=== SUMMARY ===")
print(f"Classes with null strings: {len(results)}")

with open("/tmp/pairip-dump/null_classes_from_frida.json", "w") as f:
    json.dump(results, f, indent=2)

if kEpRxMC_data:
    with open("/tmp/pairip-dump/kEpRxMC_from_frida.json", "w") as f:
        json.dump(kEpRxMC_data, f, indent=2)
    print(f"kEpRxMC fields: {len(kEpRxMC_data)}")

for cls, fields_list in results.items():
    print(f"  {cls}: {fields_list}")
