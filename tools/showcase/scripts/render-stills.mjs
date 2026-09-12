import {execFileSync} from 'node:child_process';
import {mkdir} from 'node:fs/promises';
import path from 'node:path';
import {fileURLToPath} from 'node:url';

const project = path.resolve(path.dirname(fileURLToPath(import.meta.url)), '..');
const cli = path.join(project, 'node_modules/@remotion/cli/remotion-cli.js');
const output = path.resolve(project, '../../docs/media');
await mkdir(output, {recursive: true});
for (const [composition, name, frame] of [
  ['Hero', 'showcase-poster', 60],
  ['Hardware', 'hardware', 70],
  ['Perimeter', 'perimeter-day', 45],
  ['Perimeter', 'perimeter-night', 135],
  ['Rails', 'rails', 60],
  ['Filters', 'blocking-filters', 60],
  ['Detection', 'detection-output', 60],
]) {
  execFileSync(process.execPath, [cli, 'still', 'src/index.ts', composition, path.join(output, `${name}.png`), `--frame=${frame}`, '--image-format=png'], {cwd: project, stdio: 'inherit'});
}
