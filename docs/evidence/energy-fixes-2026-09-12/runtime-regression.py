import sys,time,json,re,shutil
from pathlib import Path
sys.path.insert(0,'Minecraft-Control/tools');from mc import Client
c=Client('Field-Emitters/run-menu');out=Path('Field-Emitters/docs/evidence/energy-fixes-2026-09-12');out.mkdir(exist_ok=True)
p=Path('Field-Emitters/run-menu/saves/Field Emitters Demo/serverconfig/fieldemitters-server.toml');original=p.read_text();results={}
def cmd(s):return c.call('mc.build.command',{'command':s})
def snap(x,z):return c.call('mc.build.inspect',{'from':{'x':x,'y':-49,'z':z},'to':{'x':x,'y':-49,'z':z},'nbt':True})['blocks'][0]
def leave():
 c.call('mc.keyboard.press',{'key':'escape'});c.call('mc.screen.widget.click',{'message':'Save and Quit to Title'});time.sleep(1)
def join():
 c.call('mc.screen.widget.click',{'message':'Singleplayer'});time.sleep(.2);c.call('mc.screen.click.at',{'x':170,'y':68});c.call('mc.screen.widget.click',{'message':'Play Selected World'});time.sleep(2)
def configure(cost,cap,rate):
 s=original.replace('demoRedstonePower = true','demoRedstonePower = false')
 for key,v in [('energyPerCellTick',cost),('emitterCapacity',cap),('emitterTransferRate',rate)]:s=re.sub(rf'{key} = \d+',f'{key} = {v}',s)
 p.write_text(s)
def post(x,z):
 for i in range(5):cmd(f'setblock {x} {-49+i} {z} fieldemitters:field_emitter[section={i}]')
try:
 cmd('tp @s 2 -48 7');results['default_post_verifier']=cmd('fielddemo verify')
 cmd('tp @s 29 -48 11');results['default_rail_verifier']=cmd('fielddemo verifyrails')
 leave();configure(0,100000,10000);join()
 cmd('tp @s 55 -47 15');post(70,24);time.sleep(.6)
 results['zero_cost_rail']=snap(50,20);results['zero_cost_lone']=snap(70,24)
 assert 'Powered:1b' in results['zero_cost_rail']['nbt'] and 'Demand:0' in results['zero_cost_rail']['nbt']
 assert 'Energy:0' in results['zero_cost_rail']['nbt']
 assert 'Powered:0b' in results['zero_cost_lone']['nbt']
 leave();configure(3,1000000000,1000000000);join()
 post(70,10)
 for x in [50,60,70]:cmd(f'data merge block {x} -49 10 {{Energy:1000000000}}')
 time.sleep(.7);results['large_network']=[snap(x,10) for x in [50,60,70]]
 assert all('Powered:1b' in b['nbt'] for b in results['large_network'])
 assert 'Demand:135' in results['large_network'][0]['nbt']
 results['passed']=True
 leave();p.write_text(original);c.call('mc.screen.widget.click',{'message':'Quit Game'})
finally:
 p.write_text(original);(out/'runtime.json').write_text(json.dumps(results,indent=2)+'\n')
print(json.dumps({'passed':results.get('passed',False),'default_post_verifier':results.get('default_post_verifier'),'default_rail_verifier':results.get('default_rail_verifier')}))
