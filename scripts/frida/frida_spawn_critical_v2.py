import frida
import json
import time

results = {}

def on_message(message, data):
    if message['type'] == 'send':
        payload = message['payload']
        if payload.startswith("===DONE==="):
            count = payload.replace("===DONE===", "")
            print(f"\nDumped {count} classes")
        else:
            try:
                obj = json.loads(payload)
                cls = obj['cls']
                results[cls] = obj['fields']
                nulls = [k for k,v in obj['fields'].items() if v is None]
                val_count = len(obj['fields']) - len(nulls)
                print(f"  {cls}: {len(obj['fields'])} fields ({val_count} valued, {len(nulls)} null)")
            except:
                print(f"  RAW: {payload[:100]}")
    elif message['type'] == 'error':
        desc = message.get('description', '')
        print(f"  ERR: {desc[:200]}")

device = frida.get_usb_device()

try:
    device.kill("com.jio.jioplay.tv")
except: pass
time.sleep(1)

pid = device.spawn(["com.jio.jioplay.tv"])
print(f"Spawned PID: {pid} (suspended)")

session = device.attach(pid)

with open("/tmp/pairip-dump/dump_critical_delayed.js", "r") as f:
    script_code = f.read()

script = session.create_script(script_code)
script.on('message', on_message)
script.load()
print("Script loaded (with setTimeout)")

device.resume(pid)
print("Resumed app, waiting for Java VM + dump...")

time.sleep(10)

# Save
with open("/tmp/pairip-dump/critical_classes_dump.json", "w") as f:
    json.dump(results, f, indent=2, default=str)

null_classes = {k: [fk for fk,fv in v.items() if fv is None] for k,v in results.items() if any(fv is None for fv in v.values())}
with open("/tmp/pairip-dump/critical_null_classes.json", "w") as f:
    json.dump(null_classes, f, indent=2)

print(f"\n=== FINAL: {len(results)} classes dumped, {len(null_classes)} with nulls ===")
for cls, nulls in sorted(null_classes.items(), key=lambda x: -len(x[1])):
    print(f"  {cls}: {len(nulls)} null fields")