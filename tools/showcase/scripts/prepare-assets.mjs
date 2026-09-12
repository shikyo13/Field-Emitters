import {mkdir, readFile, writeFile, copyFile, access} from 'node:fs/promises';
import {createHash} from 'node:crypto';
import {fileURLToPath} from 'node:url';
import path from 'node:path';

const project = path.resolve(path.dirname(fileURLToPath(import.meta.url)), '..');
const repo = path.resolve(project, '../..');
const assets = path.join(repo, 'src/main/resources/assets/fieldemitters');
const output = path.join(project, 'public');
const sha256 = (bytes) => createHash('sha256').update(bytes).digest('hex');
const records = [];
await Promise.all([
  mkdir(path.join(output, 'textures'), {recursive: true}),
  mkdir(path.join(output, 'screenshots'), {recursive: true}),
  mkdir(path.join(project, 'src/generated'), {recursive: true}),
  mkdir(path.join(repo, 'docs/media'), {recursive: true}),
]);

async function copy(source, target) {
  await mkdir(path.dirname(target), {recursive: true});
  await copyFile(source, target);
  const bytes = await readFile(source);
  records.push({source: path.relative(repo, source), sha256: sha256(bytes)});
  return bytes;
}

// Resolve the model's parent chain the way the game does, keeping the child's texture variables.
async function model(id) {
  const source = path.join(assets, 'models', `${id}.json`);
  const bytes = await readFile(source);
  records.push({source: path.relative(repo, source), sha256: sha256(bytes)});
  const own = JSON.parse(bytes);
  const parent = own.parent?.startsWith('fieldemitters:')
    ? await model(own.parent.slice('fieldemitters:'.length)) : {};
  const merged = {...parent, ...own, textures: {...parent.textures, ...own.textures}};
  return {elements: merged.elements ?? [], textures: merged.textures ?? {}};
}

const models = {
  emitter0: await model('block/emitter_0_active'),
  emitter1: await model('block/emitter_1_active'),
  emitter2: await model('block/emitter_2_active'),
  emitter3: await model('block/emitter_3_active'),
  emitter4: await model('block/emitter_4_active'),
  rail: await model('block/field_rail_active'),
  tuner: await model('item/field_tuner'),
};
const animations = {};
for (const texture of new Set(Object.values(models).flatMap(m => Object.values(m.textures)))) {
  const relative = texture.replace('fieldemitters:', '') + '.png';
  const bytes = await copy(path.join(assets, 'textures', relative), path.join(output, 'textures', relative));
  const width = bytes.readUInt32BE(16);
  const height = bytes.readUInt32BE(20);
  const meta = path.join(assets, 'textures', relative + '.mcmeta');
  if (await access(meta).then(() => true, () => false)) {
    const animation = JSON.parse(await readFile(meta)).animation ?? {};
    records.push({source: path.relative(repo, meta), sha256: sha256(await readFile(meta))});
    animations[texture] = {frames: height / width, frametime: animation.frametime ?? 1};
  }
}

// Native captures from the 1.0.0 release build. Bytes are copied unchanged; nothing is retouched.
const screenshots = [
  ['post-closeup', 'gallery-post-closeup.png', 'Two linked emitter posts and the energy cube feeding them by day.'],
  ['post-night', 'gallery-post-night.png', 'The same two posts after dark with the cyan field lit.'],
  ['perimeter-day', 'gallery-perimeter-day.png', 'Four posts on a stepped hillside; the field follows the stairs.'],
  ['perimeter-night', 'gallery-perimeter-night-close.png', 'The hillside perimeter at night in violet.'],
  ['impact-night', 'gallery-impact-night-1.png', 'A zombie held inside the lit perimeter at midnight.'],
  ['rails-doorway', 'gallery-rails-doorway.png', 'Rails sealing a doorway, a shaft and a walkway, with a redstone lamp on the detection output.'],
  ['gui-blocking', 'gui-blocking.png', 'The Blocking tab of an emitter.'],
  ['gui-detection', 'gui-detection.png', 'The Detection tab of an emitter.'],
  ['gui-manager', 'gui-manager.png', 'The Field Manager opened from the tuner.'],
  ['craft-emitter', 'craft-emitter.png', 'The Field Emitter recipe in a crafting table.'],
];
for (const [name, file, caption] of screenshots) {
  const source = path.join(repo, 'docs/publishing/gallery', file);
  const bytes = await readFile(source);
  if (!bytes.subarray(0, 8).equals(Buffer.from([137, 80, 78, 71, 13, 10, 26, 10]))) throw new Error(`Expected a native PNG capture: ${source}`);
  if (bytes.readUInt32BE(16) !== 1708 || bytes.readUInt32BE(20) !== 960) throw new Error(`Unexpected native capture dimensions: ${source}`);
  await copy(source, path.join(output, `screenshots/${name}.png`));
  Object.assign(records[records.length - 1], {width: 1708, height: 960, caption, stage: name});
}
await writeFile(path.join(project, 'src/generated/models.json'), JSON.stringify({models, animations}));
await writeFile(path.join(repo, 'docs/media/sources.json'), JSON.stringify({
  publicName: 'Field Emitters',
  captureBuildSha256: '029ba534a36c51c5cbe4d08769b4409fb156efba530ec3c46be149a6b9ff5fd9',
  captureDates: '2026-09-11',
  presentation: 'Gameplay screenshots are unedited captures of the Field Emitters 1.0.0 release build at 1708 × 960. The video and presentation images place those captures inside animated framing and typography, alongside Three.js studio turntables of the shipped Minecraft model JSON and textures. Studio lighting is illustrative; the in-game field renderer and interface appear only in the captures.',
  audio: 'The mod ships no sound assets, so the showcase is silent.',
  records: [...new Map(records.map(record => [record.source, record])).values()],
}, null, 2) + '\n');
console.log('Prepared the current model assets and ten unchanged gameplay captures.');
