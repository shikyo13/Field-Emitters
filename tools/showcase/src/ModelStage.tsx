import React, {useEffect, useMemo, useState} from 'react';
import {ThreeCanvas} from '@remotion/three';
import {continueRender, delayRender, cancelRender, staticFile, useCurrentFrame} from 'remotion';
import * as THREE from 'three';
import data from './generated/models.json';

type Vec3 = [number, number, number];
type Face = {texture: string; uv: number[]; tintindex?: number};
type Element = {from: Vec3; to: Vec3; faces: Record<string, Face>; shade?: boolean; rotation?: {origin: Vec3; axis: 'x' | 'y' | 'z'; angle: number}};
type Model = {elements: Element[]; textures: Record<string, string>};
type Animation = {frames: number; frametime: number};
const {models, animations} = data as unknown as {models: Record<string, Model>; animations: Record<string, Animation>};
const directions = ['east', 'west', 'up', 'down', 'south', 'north'];

// The game tints tintindex 0 faces with the field color; 0x52E5FF is the default preset in EmitterEntity.
export const FIELD_COLOR = '#52e5ff';
export type ModelKind = 'post' | 'rail' | 'tuner';

function useTextures() {
  const [handle] = useState(() => delayRender('Load original Field Emitters textures'));
  const [textures, setTextures] = useState<Record<string, THREE.Texture> | null>(null);
  useEffect(() => {
    let cancelled = false;
    const paths = [...new Set(Object.values(models).flatMap(m => Object.values(m.textures)))];
    Promise.all(paths.map(async id => {
      const map = await new THREE.TextureLoader().loadAsync(staticFile(`textures/${id.replace('fieldemitters:', '')}.png`));
      map.magFilter = THREE.NearestFilter;
      map.minFilter = THREE.NearestFilter;
      map.generateMipmaps = false;
      map.colorSpace = THREE.SRGBColorSpace;
      return [id, map] as const;
    })).then(entries => {
      if (!cancelled) setTextures(Object.fromEntries(entries));
    }).catch(cancelRender);
    return () => {cancelled = true;};
  }, []);
  useEffect(() => {if (textures) continueRender(handle);}, [textures, handle]);
  return textures;
}

const Box: React.FC<{element: Element; model: Model; textures: Record<string, THREE.Texture>}> = ({element, model, textures}) => {
  const frame = useCurrentFrame();
  const {geometry, materials} = useMemo(() => {
    const geo = new THREE.BoxGeometry(...element.to.map((n, i) => (n - element.from[i]) / 16) as Vec3);
    const uv = geo.attributes.uv;
    const mats = directions.map((direction, faceIndex) => {
      const face = element.faces[direction];
      if (!face) return new THREE.MeshBasicMaterial({transparent: true, opacity: 0, depthWrite: false});
      const id = model.textures[face.texture.replace('#', '')];
      const map = textures[id].clone();
      const [u0, v0, u1, v1] = face.uv;
      const corners = [[u0 / 16, 1 - v0 / 16], [u1 / 16, 1 - v0 / 16], [u0 / 16, 1 - v1 / 16], [u1 / 16, 1 - v1 / 16]];
      for (let v = 0; v < 4; v++) uv.setXY(faceIndex * 4 + v, corners[v][0], corners[v][1]);
      const tinted = face.tintindex === 0;
      const luminous = tinted || element.shade === false;
      map.needsUpdate = true;
      const material = new THREE.MeshStandardMaterial({
        map, color: tinted ? FIELD_COLOR : '#ffffff', roughness: 0.78, metalness: 0.08, alphaTest: 0.1,
        emissive: luminous ? (tinted ? FIELD_COLOR : '#ffffff') : '#000000', emissiveMap: luminous ? map : undefined,
        emissiveIntensity: luminous ? 0.42 : 0,
      });
      material.userData.texture = id;
      return material;
    });
    uv.needsUpdate = true;
    return {geometry: geo, materials: mats};
  }, [element, model, textures]);
  useEffect(() => () => {geometry.dispose(); materials.forEach(m => {m.map?.dispose(); m.dispose();});}, [geometry, materials]);
  // Animated textures are vertical strips of square frames; the .mcmeta frametime is in game ticks.
  materials.forEach(mat => {
    const bitmap = mat.map?.image as {width: number; height: number} | undefined;
    if (mat.map && bitmap && bitmap.height > bitmap.width) {
      const frames = bitmap.height / bitmap.width;
      const frametime = animations[mat.userData.texture]?.frametime ?? 1;
      mat.map.repeat.y = 1 / frames;
      mat.map.offset.y = 1 - (Math.floor(frame / 30 * 20 / frametime) % frames + 1) / frames;
    }
  });
  const center = element.from.map((n, i) => (n + element.to[i]) / 32) as Vec3;
  if (!element.rotation) return <mesh geometry={geometry} material={materials} position={center} />;
  const pivot = element.rotation.origin.map(n => n / 16) as Vec3;
  const rotation: Vec3 = [0, 0, 0];
  rotation['xyz'.indexOf(element.rotation.axis)] = THREE.MathUtils.degToRad(element.rotation.angle);
  return <group position={pivot} rotation={rotation}><mesh geometry={geometry} material={materials} position={center.map((n, i) => n - pivot[i]) as Vec3} /></group>;
};

const MeshModel: React.FC<{name: string; textures: Record<string, THREE.Texture>}> = ({name, textures}) => {
  const model = models[name];
  return <>{model.elements.map((element, index) => <Box key={index} element={element} model={model} textures={textures} />)}</>;
};

export const ModelStage: React.FC<{kind?: ModelKind; width: number; height: number; zoom?: number; angle?: number; turn?: number; pixelRatio?: number; tilt?: number}> = ({kind = 'post', width, height, zoom = 150, angle = 0.38, turn = 0.003, pixelRatio = 1, tilt = 0.16}) => {
  const frame = useCurrentFrame();
  const textures = useTextures();
  return <ThreeCanvas width={width} height={height} orthographic camera={{position: [0, 0, 12], zoom, near: 0.1, far: 50}} gl={{alpha: true, antialias: true}} dpr={pixelRatio}>
    <ambientLight intensity={1.5} />
    <directionalLight position={[-3, 5, 5]} intensity={2.6} color="#fff1dc" />
    <directionalLight position={[4, 0, -3]} intensity={1.8} color="#8f7dff" />
    {textures && <group rotation={[tilt, Math.PI + angle + frame * turn, 0]}>
      {kind === 'post' ? <group position={[-0.5, -2.5, -0.5]}>
        {[0, 1, 2, 3, 4].map(section => <group key={section} position={[0, section, 0]}><MeshModel name={`emitter${section}`} textures={textures} /></group>)}
      </group> : kind === 'rail' ? <group position={[-0.5, -0.5, -0.89]}><MeshModel name="rail" textures={textures} /></group>
        : <group position={[-0.5, -0.6, -0.45]}><MeshModel name="tuner" textures={textures} /></group>}
    </group>}
  </ThreeCanvas>;
};
