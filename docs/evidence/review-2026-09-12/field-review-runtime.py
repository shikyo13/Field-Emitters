import sys,json,time
from pathlib import Path
sys.path.insert(0,'Minecraft-Control/tools');from mc import Client
c=Client('Field-Emitters/run-menu');out=Path('Field-Emitters/docs/evidence/review-2026-09-12')
for name,args in [('mc.build.command',{'command':'tp @s 2 -48 7'}),('mc.build.command',{'command':'fielddemo verify'}),('mc.build.command',{'command':'tp @s 29 -48 11'}),('mc.build.command',{'command':'fielddemo verifyrails'})]:
 result=c.call(name,args);print(args,result)
(out/'baseline-verification-log.txt').write_text(Path('Field-Emitters/run-menu/logs/latest.log').read_text())
ts=c.rpc('tools/list')['tools'];(out/'controller-tool-inventory.json').write_text(json.dumps(ts,indent=2))
print([(t['name'],t['description']) for t in ts if any(w in t['name'] for w in ['craft','inventory','mine','break','record','capture'])])
