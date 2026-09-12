import sys,time,json
from pathlib import Path
sys.path.insert(0,'Minecraft-Control/tools');from mc import Client
c=Client('Field-Emitters/run-menu');out=Path('Field-Emitters/docs/evidence/review-2026-09-12')
recipes=[('emitter',1,['copper_ingot','amethyst_shard','copper_ingot','blue_stained_glass','redstone_block','blue_stained_glass','iron_ingot','iron_ingot','iron_ingot']),('rail',4,['iron_ingot','copper_ingot','iron_ingot','amethyst_shard','redstone','amethyst_shard','iron_ingot','copper_ingot','iron_ingot']),('tuner',1,[None,'amethyst_shard',None,'copper_ingot','blue_stained_glass_pane','copper_ingot','iron_ingot','redstone','iron_ingot'])]
results=[]
for name,count,items in recipes:
 try:c.call('mc.build.command',{'command':'clear @s'})
 except RuntimeError as e:
  if 'No items were found' not in str(e):raise
 for i,item in enumerate(items):
  if item:c.call('mc.build.command',{'command':f'item replace entity @s inventory.{i} with minecraft:{item} 1'})
 time.sleep(.15)
 for i,item in enumerate(items):
  if item:
   c.call('mc.container.click',{'slot':10+i});c.call('mc.container.click',{'slot':1+i})
 time.sleep(.15);s=c.call('mc.container.state');result=s['slots'][0];assert result['item']=='fieldemitters:field_'+name and result['count']==count,result
 c.call('mc.container.click',{'slot':0,'clickType':'QUICK_MOVE'});time.sleep(.15)
 s=c.call('mc.container.state');assert all(slot['empty'] for slot in s['slots'][1:10]);results.append({'recipe':name,'result':result,'ingredients_consumed':True,'glass_variant':'blue stained glass where applicable'})
 print(name,'PASS')
(out/'crafting.json').write_text(json.dumps(results,indent=2));c.call('mc.container.close')
