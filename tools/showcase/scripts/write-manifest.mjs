import {execFileSync} from 'node:child_process';
import {createHash} from 'node:crypto';
import {readFile, writeFile, readdir} from 'node:fs/promises';
import {fileURLToPath} from 'node:url';
import path from 'node:path';

const project = path.resolve(path.dirname(fileURLToPath(import.meta.url)), '..');
const media = path.resolve(project, '../../docs/media');
const video = path.join(media, 'field-emitters-showcase.mp4');
const metadata = JSON.parse(execFileSync(process.env.FFPROBE || 'ffprobe', [
  '-v', 'error', '-show_entries',
  'format=duration,size:stream=codec_type,codec_name,width,height,r_frame_rate,pix_fmt,nb_frames,duration',
  '-of', 'json', video,
], {encoding: 'utf8'}));
const videoStream = metadata.streams.find(stream => stream.codec_type === 'video');
if (videoStream?.codec_name !== 'h264' || videoStream.width !== 1920 || videoStream.height !== 1080 || videoStream.r_frame_rate !== '30/1' || Number(videoStream.nb_frames) !== 900 || Math.abs(Number(videoStream.duration) - 30) > 0.001 || metadata.streams.some(stream => stream.codec_type === 'audio')) {
  throw new Error('The exported file does not match the documented silent 1080p30 H.264 showcase.');
}
const exports = [];
for (const filename of (await readdir(media)).filter(name => /\.(png|mp4)$/.test(name)).sort()) {
  const bytes = await readFile(path.join(media, filename));
  const record = {file: filename, bytes: bytes.length, sha256: createHash('sha256').update(bytes).digest('hex')};
  if (filename.endsWith('.png')) {
    record.width = bytes.readUInt32BE(16);
    record.height = bytes.readUInt32BE(20);
    if (record.width !== 1920 || record.height !== 1080) throw new Error(`Unexpected image dimensions: ${filename}`);
  }
  exports.push(record);
}
const source = await readFile(path.join(media, 'sources.json'));
await writeFile(path.join(media, 'render-manifest.json'), JSON.stringify({
  remotion: '4.0.523',
  composition: 'FieldEmitters',
  sourcesSha256: createHash('sha256').update(source).digest('hex'),
  video: metadata,
  exports,
  verification: 'Export metadata and image dimensions match. Capture and model input hashes are recorded separately in sources.json.',
}, null, 2) + '\n');
console.log('Recorded the 30-second H.264 video and the 1080p presentation images.');
