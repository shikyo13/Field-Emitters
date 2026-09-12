import React from 'react';
import {interpolate, useCurrentFrame} from 'remotion';
import {Backdrop, Capture, Eyebrow, Reveal, mono, palette} from '../Design';

export const Perimeter: React.FC = () => {
  const frame = useCurrentFrame();
  return <Backdrop section="IN THE WORLD" number="03">
    <Reveal style={{position: 'absolute', left: 104, top: 196, width: 590}}>
      <Eyebrow>FIELD EMITTER</Eyebrow>
      <div style={{fontSize: 100, fontWeight: 600, lineHeight: 0.96}}>Posts link.<br />The field<br />follows.</div>
      <div style={{fontSize: 37, lineHeight: 1.28, color: '#b9c8d4', marginTop: 30}}>Place posts up to 20 blocks apart.<br />The field rises five blocks<br />and climbs the terrain between them.</div>
      <div style={{fontSize: 88, fontWeight: 600, color: palette.accent, marginTop: 30}}>20 <span style={{fontSize: 38}}>BLOCKS</span></div>
      <div style={{fontFamily: mono, fontSize: 18, color: '#9aabc2', marginTop: 6}}>BETWEEN LINKED POSTS</div>
    </Reveal>
    <Capture name="perimeter-day" x={720} y={230} width={1080} height={607} zoom />
    <div style={{position: 'absolute', inset: 0, opacity: interpolate(frame, [90, 114], [0, 1], {extrapolateLeft: 'clamp', extrapolateRight: 'clamp'})}}>
      <Capture name="perimeter-night" x={720} y={230} width={1080} height={607} zoom />
    </div>
    <div style={{position: 'absolute', left: 720, top: 871, fontFamily: mono, fontSize: 19, color: '#9aabc2'}}>{frame < 102 ? 'FOUR POSTS ON A HILLSIDE / DAY' : 'THE SAME PERIMETER / NIGHT'}</div>
  </Backdrop>;
};
