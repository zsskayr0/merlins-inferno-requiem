"""Bake Cinder's BB5 mesh rig into GeckoLib bones and shared server/client poses.

The source remains editable in Models/cinder. Minecraft uses sampled local bone
transforms for both rendering and multipart hitboxes, so bite/heat checks follow
the actual animated body, including its coiled land pose.
"""
import base64
import json
import math
from pathlib import Path
import sys
import numpy as np

SOURCE = Path(sys.argv[1]) if len(sys.argv)>1 else Path(__file__).resolve().parents[2]/'src/Models/cinder/cinder.bbmodel'
OUT = Path(__file__).resolve().parents[1]/'src/main/resources/assets/merlins_inferno'
data=json.loads(SOURCE.read_text(encoding='utf-8'))
groups={g['uuid']:g for g in data['groups']}
elements={e['uuid']:e for e in data['elements']}
ordered=[];parents={};membership={}
def walk(node,parent=-1):
    index=len(ordered);ordered.append(groups[node['uuid']]);parents[index]=parent
    for c in node['children']:
        if isinstance(c,dict):walk(c,index)
        else:membership[c]=index
for root in data['outliner']:walk(root)
indices={g['uuid']:i for i,g in enumerate(ordered)}
names={i:f"{g['name']}_{i}" for i,g in enumerate(ordered)}
# Anchor the snout near the entity origin, with its normal pose at foot level.
offset=np.array([0.,-5.75,35.])
mesh={name:[] for name in names.values()}
for identifier,e in elements.items():
    assert max(abs(v) for v in e.get('rotation',[0,0,0]))<1e-8
    points={k:(np.array(v)+e['origin']+offset)/16 for k,v in e['vertices'].items()}
    for f in e['faces'].values():
        vs=f['vertices'];assert len(vs)==3
        pts=np.array([points[v] for v in vs]);n=np.cross(pts[1]-pts[0],pts[2]-pts[0]);n/=max(np.linalg.norm(n),1e-12)
        uv=np.array([f['uv'][v] for v in vs])/128
        mesh[names[membership[identifier]]].append(np.round(np.r_[n,pts.ravel(),uv.ravel()],6).tolist())
bones=[]
for i,g in enumerate(ordered):
    assert g['rotation']==[0,0,0]
    pivot=np.array(g['origin'])+offset
    b={'name':names[i],'pivot':[-pivot[0],pivot[1],pivot[2]]}
    if parents[i]>=0:b['parent']=names[parents[i]]
    bones.append(b)
geo={'format_version':'1.12.0','minecraft:geometry':[{'description':{'identifier':'geometry.cinder',
     'texture_width':128,'texture_height':128,'visible_bounds_width':36,'visible_bounds_height':16,
     'visible_bounds_offset':[0,3,0]},'bones':bones}]}
rig={'scale':1.35,'bones':[{'name':names[i],'parent':parents[i],'pivot':(np.array(g['origin'])+offset).tolist()} for i,g in enumerate(ordered)],
     'parts':[next(i for i,g in enumerate(ordered) if g['name']=='head')]+
             [i for i,g in enumerate(ordered) if g['name'].startswith(('neck_','spine_'))],
     'clips':{}}
exported={}
for animation in data['animations']:
    length=animation['length'];count=round(length*20)+1;times=np.linspace(0,length,count)
    active=[];samples=[];export_bones={}
    for identifier,an in animation['animators'].items():
        bi=indices[identifier];active.append(bi);values=np.zeros((count,6));channels={}
        for channel,start,sign in [('position',0,[-1,1,1]),('rotation',3,[-1,-1,1])]:
            keys=sorted([k for k in an['keyframes'] if k['channel']==channel],key=lambda k:k['time'])
            if not keys:continue
            ts=[k['time'] for k in keys];v=np.array([[k['data_points'][0][axis] for axis in 'xyz'] for k in keys])
            for axis in range(3):values[:,start+axis]=np.interp(times,ts,v[:,axis])
            channels[channel]={f'{t:g}':np.round(vector*np.array(sign),6).tolist() for t,vector in zip(ts,v)}
        samples.append(values)
        export_bones[names[bi]]=channels
    values=np.stack(samples,axis=1)
    rig['clips'][animation['name']]={'length':length,'loop':animation['loop']=='loop','bones':active,
        'frames':np.round(values,6).reshape(count,-1).tolist()}
    exported['animation.cinder.'+animation['name']]={'loop':True if animation['loop']=='loop' else 'hold_on_last_frame' if animation['loop']=='hold' else False,
        'animation_length':length,'bones':export_bones}
    assert np.isfinite(values).all()
    if animation['loop']=='loop':assert np.array_equal(values[0],values[-1])
def write(path,contents):
    path=OUT/path;path.parent.mkdir(parents=True,exist_ok=True)
    path.write_text(json.dumps(contents,separators=(',',':')),encoding='utf-8')
write(Path('geo/cinder.geo.json'),geo)
write(Path('animations/cinder.animation.json'),{'format_version':'1.8.0','animations':exported})
write(Path('cinder/cinder.meshes.json'),mesh)
write(Path('cinder/cinder.rig.json'),rig)
texture=OUT/'textures/entity/cinder/cinder.png';texture.parent.mkdir(parents=True,exist_ok=True)
texture.write_bytes(base64.b64decode(data['textures'][0]['source'].split(',')[1]))
assert len(membership)==len(elements)==230
assert len(rig['parts'])==32
print(f"Baked {len(bones)} bones, {sum(map(len,mesh.values()))} triangles, {len(exported)} clips and {len(rig['parts'])} animated hitboxes.")
