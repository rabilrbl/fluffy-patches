import frida
import sys
import json
import time

results = {}

def on_message(message, data):
    if message['type'] == 'send':
        payload = message['payload']
        if payload.startswith("kEpRxMC:"):
            print("kEpRxMC dump received")
        elif payload.startswith("NULL_CLASS:"):
            try:
                obj = json.loads(payload.replace("NULL_CLASS: ", ""))
                cls = obj['cls']
                results[cls] = obj['fields']
                nulls = [k for k,v in obj['fields'].items() if v is None]
                print(f"⚠ {cls}: {len(nulls)} null strings - {nulls[:5]}")
            except Exception as e:
                print(f"Parse error: {e}")
        elif payload == "===SCAN_COMPLETE===":
            print("Scan complete")
        else:
            print(f"MSG: {payload[:200]}")
    elif message['type'] == 'error':
        print(f"ERROR: {message.get('description', '')[:200]}")

device = frida.get_usb_device()
try:
    device.kill("com.jio.jioplay.tv")
except:
    pass
time.sleep(1)

pid = device.spawn(["com.jio.jioplay.tv"])
print(f"Spawned PID: {pid}")

session = device.attach(pid)

with open("/tmp/pairip-dump/dump_immediate.js", "r") as f:
    script_code = f.read()

script = session.create_script(script_code)
script.on('message', on_message)
script.load()

# Resume the app
device.resume(pid)
print("App resumed, collecting...")

# Wait for results - but not too long
time.sleep(8)

# Save
print(f"\n=== SUMMARY ===")
print(f"Classes with null strings: {len(results)}")

with open("/tmp/pairip-dump/null_classes_dump.json", "w") as f:
    json.dump(results, f, indent=2, default=str)
print("Saved to /tmp/pairip-dump/null_classes_dump.json")

for cls, fields in results.items():
    nulls = [k for k, v in fields.items() if v is None]
    print(f"  {cls} ({len(nulls)} null): {nulls[:8]}")
