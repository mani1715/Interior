"""Explicit opt-in, two-call regional recolour experiment; no database writes or retries."""
import argparse
import json
import os
from pathlib import Path
import urllib.request
import urllib.error
import uuid

parser = argparse.ArgumentParser()
parser.add_argument('--live', action='store_true')
args = parser.parse_args()
if not args.live:
    raise SystemExit('Pass --live only with authorization for two provider calls (10 credits).')
root = Path(__file__).resolve().parents[1]
values = {}
for line in (root / '.env.local').read_text().splitlines():
    if '=' in line and not line.lstrip().startswith('#'):
        name, value = line.split('=', 1)
        values[name.strip()] = value.strip().strip('\"\'')
key = os.environ.get('STABILITY_API_KEY') or values.get('STABILITY_API_KEY')
if not key:
    raise SystemExit('Missing Stability credential')
output = root / 'apps/api/target/regional-recolor-2026-11-06'
output.mkdir(parents=True, exist_ok=True)
photo = (root / 'apps/web/public/images/approved/wardrobe.png').read_bytes()
results = []
for name, selection, prompt in [
    ('upper-yellow', 'upper overhead wardrobe cabinet doors', 'Bright yellow painted cabinet doors. Preserve existing panel seams, lighting and texture.'),
    ('lower-black', 'lower tall wardrobe cabinet doors below the overhead cabinets', 'Deep matte black painted cabinet doors. Preserve existing panel seams, handles, lighting and texture.'),
]:
    boundary = uuid.uuid4().hex
    body = bytearray()
    def field(k, v):
        body.extend(f'--{boundary}\r\nContent-Disposition: form-data; name="{k}"\r\n\r\n{v}\r\n'.encode())
    body.extend(f'--{boundary}\r\nContent-Disposition: form-data; name="image"; filename="input.png"\r\nContent-Type: image/png\r\n\r\n'.encode())
    body.extend(photo)
    body.extend(b'\r\n')
    for k, v in {'prompt': prompt, 'select_prompt': selection, 'output_format': 'png', 'grow_mask': '0', 'seed': '12345'}.items():
        field(k, v)
    body.extend(f'--{boundary}--\r\n'.encode())
    request = urllib.request.Request('https://api.stability.ai/v2beta/stable-image/edit/search-and-recolor', data=bytes(body), headers={
        'Authorization': f'Bearer {key}', 'Content-Type': f'multipart/form-data; boundary={boundary}', 'Accept': 'image/*',
    })
    try:
        with urllib.request.urlopen(request, timeout=120) as response:
            if not response.headers.get('Content-Type', '').startswith('image/'):
                raise SystemExit('Provider did not return an image; no further calls made.')
            photo = response.read()
            (output / f'{name}.png').write_bytes(photo)
            results.append({'step': name, 'select_prompt': selection, 'prompt': prompt, 'status': response.status})
            print(json.dumps(results[-1]), flush=True)
    except urllib.error.HTTPError as error:
        results.append({'step': name, 'http_status': error.code, 'failed': True})
        print(json.dumps(results[-1]), flush=True)
        break
    except urllib.error.URLError:
        results.append({'step': name, 'failed': True, 'error': 'Provider network connection failed; no retry performed.'})
        print(json.dumps(results[-1]), flush=True)
        break
(output / 'results.json').write_text(json.dumps(results, indent=2))
if len(results) != 2 or any(r.get('failed') for r in results):
    raise SystemExit(1)
