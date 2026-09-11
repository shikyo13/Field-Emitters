#!/usr/bin/env python3
"""Author Field Emitters' pixel materials and explicitly UV-mapped hardware.

All artwork is deterministic code-native pixel art. No stock-image pixels are used.
Run with Python 3 + Pillow. Diff/check mode verifies the actual shipping resources.
"""
from pathlib import Path
from copy import deepcopy
from io import BytesIO
import argparse, json, math, random
from PIL import Image, ImageDraw

ROOT=Path(__file__).resolve().parents[1]/'src/main/resources/assets/fieldemitters'
OUT={}
SIZE=64

def noise(x,y): return ((x*374761393+y*668265263)^((x+y)*1274126177))%7-3

def material(base, seed=0):
 im=Image.new('RGBA',(SIZE,SIZE)); px=im.load()
 for y in range(SIZE):
  for x in range(SIZE):
   n=noise(x+seed,y)+(1 if y%4==0 else 0)
   px[x,y]=tuple(max(0,min(255,c+n)) for c in base)+(255,)
 return im

def rect(d,box,fill,edge=None):d.rectangle(box,fill=fill,outline=edge)
def bevel(d,box,fill,bright,dark):
 x,y,a,b=box;rect(d,box,fill);d.line((x,y,a,y),fill=bright);d.line((x,y,x,b),fill=bright);d.line((x,b,a,b),fill=dark);d.line((a,y,a,b),fill=dark)
def screw(d,x,y):
 rect(d,(x-2,y-2,x+2,y+2),(16,23,31));d.line((x-1,y-2,x+1,y-2),fill=(151,163,174));rect(d,(x-1,y-1,x+1,y+1),(83,94,108));d.line((x-1,y+1,x+1,y-1),fill=(26,35,45))
def write_png(name,im):
 out=BytesIO();im.save(out,format='PNG');OUT['textures/'+name+'.png']=out.getvalue()
def write_json(name,obj):OUT[name]=(json.dumps(obj,indent=2)+'\n').encode()

# Broad panel treatment, inset access doors, machined edges: legible at ordinary distances.
alloy=material((48,59,72));d=ImageDraw.Draw(alloy)
bevel(d,(1,1,62,62),(44,54,68),(103,120,137),(17,24,34))
bevel(d,(7,9,56,54),(55,67,81),(83,99,116),(25,34,45))
d.line([(9,46),(19,46),(24,51),(54,51)],fill=(28,39,51),width=2)
for x,y in [(5,5),(58,5),(5,58),(58,58)]:screw(d,x,y)
for y in [15,19,23]:d.line((13,y,30,y),fill=(36,47,60));d.line((13,y+1,30,y+1),fill=(84,99,113))
rect(d,(39,15,50,17),(135,148,158));rect(d,(39,20,47,21),(82,97,110))
write_png('block/graphite',alloy)

ceramic=material((173,184,190),3);d=ImageDraw.Draw(ceramic)
bevel(d,(1,1,62,62),(160,176,187),(225,234,234),(72,91,110))
d.line([(8,1),(8,44),(17,53),(62,53)],fill=(101,123,140),width=2)
d.line([(10,2),(10,43),(18,51),(61,51)],fill=(194,208,216))
rect(d,(13,9,49,11),(206,217,222));rect(d,(15,16,33,18),(90,112,130))
rect(d,(15,22,26,23),(122,144,159));rect(d,(15,27,43,28),(136,155,169))
for x in [44,48,52]:rect(d,(x,36,x+1,44),(76,99,119))
for x,y in [(4,6),(58,47)]:screw(d,x,y)
write_png('block/ceramic_plate',ceramic)

titanium=material((111,130,145),8);d=ImageDraw.Draw(titanium)
for x in [1,3,59,62]:d.line((x,0,x,63),fill=(177,194,202) if x in [1,59] else (42,61,78))
for y in range(6,64,12):d.line((5,y,58,y),fill=(76,96,114));d.line((5,y+1,58,y+1),fill=(152,174,188))
write_png('block/titanium',titanium)

vent=material((21,30,40),4);d=ImageDraw.Draw(vent)
bevel(d,(1,1,62,62),(23,33,44),(90,108,122),(11,18,26))
for y in range(8,57,7):
 bevel(d,(7,y,56,y+3),(48,62,76),(99,116,128),(9,16,24))
for x,y in [(4,4),(59,4),(4,59),(59,59)]:screw(d,x,y)
write_png('block/heat_sink',vent)

rubber=material((23,29,37));d=ImageDraw.Draw(rubber)
for y in range(3,64,7):d.line((0,y,63,y),fill=(9,15,22),width=2);d.line((0,y+2,63,y+2),fill=(44,55,66))
for x in [5,58]:d.line((x,0,x,63),fill=(11,18,27),width=2)
write_png('item/grip',rubber)

# The display is its own UV surface; its neutral animated art takes network color in-game.
frames=[]
for frame in range(16):
 im=Image.new('RGBA',(64,64),(7,10,15,255));d=ImageDraw.Draw(im)
 bevel(d,(0,0,63,63),(10,17,23),(91,105,118),(2,6,10))
 d.rectangle((4,4,59,59),outline=(57,67,77));d.line((7,12,56,12),fill=(70,70,70))
 for x in range(8,32,3):d.line((x,7,x+1,7),fill=(170,170,170))
 for x,y in [(10,21),(46,21),(10,43),(46,43)]:rect(d,(x-2,y-2,x+2,y+2),(206,206,206))
 d.line([(10,21),(46,21),(46,43),(10,43),(10,21)],fill=(128,128,128),width=2)
 for yy in [26,32,38]:
  for xx in [17,26,35]:d.regular_polygon((xx,yy,4),6,rotation=30,outline=(54,54,54))
 scan=17+frame*2
 d.line((7,min(scan,47),56,min(scan,47)),fill=(230,230,230))
 for i in range(3):rect(d,(8+i*17,51,20+i*17,55),(185 if (frame//4+i)%3 else 88,)*3)
 frames.append(im)
strip=Image.new('RGBA',(64,64*16))
for i,im in enumerate(frames):strip.paste(im,(0,i*64))
write_png('item/tuner_display',strip);write_json('textures/item/tuner_display.png.mcmeta',{'animation':{'frametime':3,'interpolate':False}})
# Passive service readout remains dark when power is absent.
display_off=frames[0].copy();d=ImageDraw.Draw(display_off);rect(d,(5,5,58,58),(8,15,22));d.line((15,31,48,31),fill=(40,53,62));write_png('block/display_off',display_off)
write_png('block/service_display',strip);write_json('textures/block/service_display.png.mcmeta',{'animation':{'frametime':4,'interpolate':False}})

# Narrow fullbright masks are reserved for actual channels, not entire bright armor plates.
energy=[]
for frame in range(16):
 im=Image.new('RGBA',(64,64),(16,21,28,255));d=ImageDraw.Draw(im)
 bevel(d,(20,0,43,63),(36,36,36),(92,92,92),(9,9,9))
 rect(d,(27,0,36,63),(180,180,180));rect(d,(30,0,33,63),(238,238,238))
 y=frame*4;rect(d,(23,y,40,min(63,y+6)),(255,255,255));energy.append(im)
strip=Image.new('RGBA',(64,1024))
for i,im in enumerate(energy):strip.paste(im,(0,64*i))
write_png('block/energy_channel',strip);write_json('textures/block/energy_channel.png.mcmeta',{'animation':{'frametime':2,'interpolate':False}})
# Solid neutral mask used by dynamic hardware vertices, rather than a hardcoded cyan texture.
write_png('block/energy_off',Image.new('RGBA',(64,64),(19,29,39,255)))

MATERIALS={'graphite':'fieldemitters:block/graphite','ceramic':'fieldemitters:block/ceramic_plate','titanium':'fieldemitters:block/titanium','vent':'fieldemitters:block/heat_sink','grip':'fieldemitters:item/grip','energy':'fieldemitters:block/energy_off','display':'fieldemitters:block/display_off','particle':'fieldemitters:block/graphite'}

def box(name,a,b,tex='graphite',uv=None,front=False,tint=None):
 faces={}
 dx,dy,dz=[b[i]-a[i] for i in range(3)]
 for face in (['north'] if front else ['up','down','north','south','east','west']):
  w,h=(dx,dz) if face in ['up','down'] else (dx,dy) if face in ['north','south'] else (dz,dy)
  faces[face]={'texture':'#'+tex,'uv':uv or [0,0,min(16,w*2),min(16,h*2)]}
  if tint is not None:faces[face].update(tintindex=tint,neoforge_data={'block_light':15,'sky_light':15})
 return {'name':name,'from':a,'to':b,'faces':faces,**({'shade':False} if tint is not None else {})}
def rotate_part(part,side):
 # Rotate cardinal hardware and its named face around the emitter center without diagonal model angles.
 part=deepcopy(part)
 for _ in range(side):
  a,b=part['from'],part['to'];part['from']=[16-b[2],a[1],a[0]];part['to']=[16-a[2],b[1],b[0]]
  mapping={'north':'east','east':'south','south':'west','west':'north'};part['faces']={mapping.get(k,k):v for k,v in part['faces'].items()}
 return part

def four(part):return [rotate_part(part,i)for i in range(4)]
sections=[]
for s in range(5):
 parts=[]
 if s==0:
  parts += [box('foundation shoe',[1,0,1],[15,1.5,15]),box('machined footing rim',[1.5,1.5,1.5],[14.5,2.2,14.5],'titanium'),box('service pedestal',[3,2.2,3],[13,13,13]),box('ceramic shoulder',[3.5,13,3.5],[12.5,15,12.5],'ceramic'),box('neck socket',[5,15,5],[11,16,11],'titanium')]
  for x,z in [(1.8,1.8),(12.2,1.8),(1.8,12.2),(12.2,12.2)]:parts.append(box('foundation anchor',[x,1.5,z],[x+2,3.2,z+2],'titanium'))
  parts+=four(box('recessed display bezel',[4,5,2.6],[12,11.5,3],'titanium',uv=[0,0,16,16]))
  parts+=four(box('perimeter service display',[4.4,5.5,2.58],[11.6,11,2.6],'display',uv=[0,0,16,16],front=True,tint=0))
  parts+=four(box('pedestal heat exchanger',[5,2.8,2.8],[11,4.5,3],'vent',uv=[0,0,16,16],front=True))
 elif s in [1,2,3]:
  parts += [box('central shielded spine',[6,0,6],[10,16,10],'vent'),box('lower structural collar',[4,0,4],[12,1.2,12],'titanium'),box('upper structural collar',[4,14.8,4],[12,16,12],'titanium')]
  for x,z in [(4,4),(10,4),(4,10),(10,10)]:parts.append(box('corner load rail',[x,1.2,z],[x+2,14.8,z+2]))
  # Segmented white armor draws the eye upward; recessed gaps expose the darker heat exchangers.
  for side in range(4):
   parts.append(rotate_part(box('split ceramic armor left',[4.1,2,3.7],[6.4,12.8,4.5],'ceramic',uv=[0,0,16,16]),side))
   parts.append(rotate_part(box('split ceramic armor right',[9.6,2,3.7],[11.9,12.8,4.5],'ceramic',uv=[0,0,16,16]),side))
   parts.append(rotate_part(box('recessed projection channel',[6.65,1.5,4.12],[9.35,14.4,4.2],'energy',uv=[4,0,12,16],front=True,tint=0),side))
   parts.append(rotate_part(box('channel end cap',[6.25,13.5,3.9],[9.75,14.5,4.4],'titanium'),side))
 elif s==4:
  parts += [box('projector root',[5,0,5],[11,3,11],'titanium'),box('capacitor collar',[3.5,3,3.5],[12.5,5.5,12.5]),box('ceramic projector band',[3,5.5,3],[13,7.5,13],'ceramic'),box('recessed lens cradle',[5,7.5,5],[11,10,11],'vent')]
  for side in range(4):
   parts.append(rotate_part(box('raised crown blade',[4,8,3.4],[6,15.3,5.4],'ceramic',uv=[0,0,16,16]),side))
   parts.append(rotate_part(box('blade tip conductor',[4,14.3,3.3],[6,15.6,5.5],'titanium'),side))
   parts.append(rotate_part(box('crown power slot',[6.8,3.7,3.47],[9.2,5.1,3.49],'energy',uv=[4,0,12,16],front=True,tint=0),side))
 sections.append(parts)
 base={'parent':'minecraft:block/block','render_type':'minecraft:cutout','textures':MATERIALS,'elements':parts}
 write_json(f'models/block/emitter_{s}.json',base)
 write_json(f'models/block/emitter_{s}_active.json',{'parent':f'fieldemitters:block/emitter_{s}','textures':{'energy':'fieldemitters:block/energy_channel','display':'fieldemitters:block/service_display'}})
write_json('blockstates/field_emitter.json',{'variants':{f'section={s},active={str(active).lower()}':{'model':f'fieldemitters:block/emitter_{s}'+('_active'if active else '')}for s in range(5)for active in [False,True]}})
# Complete miniature, not a plain pedestal block. UVs stay exactly those of the full-size structure.
mini=[]
for s,parts in enumerate(sections):
 for part in deepcopy(parts):
  for k in ['from','to']:
   x,y,z=part[k];part[k]=[8+(x-8)*.72,(y+s*16)*.2,8+(z-8)*.72]
  mini.append(part)
write_json('models/item/field_emitter.json',{'parent':'minecraft:block/block','gui_light':'front','textures':{**MATERIALS,'energy':'fieldemitters:block/energy_channel','display':'fieldemitters:block/service_display'},'elements':mini,'display':{'gui':{'rotation':[12,225,0],'translation':[0,0,0],'scale':[1,1,1]},'ground':{'translation':[0,2,0],'scale':[.5,.5,.5]},'firstperson_righthand':{'rotation':[0,145,0],'translation':[-1,2,0],'scale':[.55,.55,.55]},'thirdperson_righthand':{'rotation':[75,35,0],'translation':[0,2,0],'scale':[.65,.65,.65]}}})

# A tuned handheld silhouette: palm grip, angled shoulders, protected screen, thumb controls.
parts=[box('rubberized grip',[5,0,6],[11,7,10],'grip',uv=[0,0,16,16]),box('grip heel',[4.5,0,5.5],[11.5,1.1,10.5],'titanium'),box('instrument chassis',[3,6,4],[13,15,11]),box('upper bezel',[3.4,14.5,3.7],[12.6,16,11],'ceramic'),box('left protective rail',[2.6,6.8,3.6],[4.2,14.7,10.5],'ceramic',uv=[0,0,16,16]),box('right protective rail',[11.8,6.8,3.6],[13.4,14.7,10.5],'ceramic',uv=[0,0,16,16]),box('display metal surround',[4.15,8.1,3.65],[11.85,14.35,4],'titanium',uv=[0,0,16,16]),box('active perimeter display',[4.55,8.45,3.59],[11.45,14.0,3.65],'display',uv=[0,0,16,16],front=True,tint=0),box('bottom controls recess',[4.2,6.4,3.7],[11.8,7.8,4],'grip'),box('antenna socket',[10.1,15.5,6],[12,16.8,8],'titanium'),box('short sensor antenna',[10.6,16.8,6.5],[11.5,19,7.4]),box('antenna tip',[10.4,18.5,6.3],[11.7,19.2,7.6],'ceramic')]
for i in range(3):parts.append(box('thumb control '+str(i),[4.7+i*2.25,6.7,3.3],[6.1+i*2.25,7.4,3.75],'titanium'))
for y in [1.8,3.3,4.8]:parts.append(box('grip finger ridge',[4.8,y,5.7],[11.2,y+.35,6.1],'grip'))
parts.append(box('rear battery access cover',[4.4,7,11],[11.6,13.8,11.2],'graphite',uv=[0,0,16,16]))
write_json('models/item/field_tuner.json',{'parent':'minecraft:block/block','gui_light':'front','textures':{**MATERIALS,'display':'fieldemitters:item/tuner_display'},'elements':parts,'display':{'gui':{'rotation':[15,205,0],'translation':[0,-.7,0],'scale':[.85,.85,.85]},'ground':{'translation':[0,2,0],'scale':[.4,.4,.4]},'firstperson_righthand':{'rotation':[0,155,5],'translation':[-1,2,0],'scale':[.43,.43,.43]},'firstperson_lefthand':{'rotation':[0,-155,-5],'translation':[-1,2,0],'scale':[.43,.43,.43]},'thirdperson_righthand':{'rotation':[75,15,0],'translation':[0,2,0],'scale':[.6,.6,.6]},'fixed':{'rotation':[0,180,0],'scale':[.8,.8,.8]}}})

if __name__=='__main__':
 parser=argparse.ArgumentParser();parser.add_argument('--check',action='store_true');args=parser.parse_args()
 for name,data in OUT.items():
  f=ROOT/name
  if args.check:
   if not f.exists() or f.read_bytes()!=data:raise SystemExit('Asset differs: '+name)
  else:f.parent.mkdir(parents=True,exist_ok=True);f.write_bytes(data)
 print('Hardware assets '+('verified'if args.check else 'generated'))
