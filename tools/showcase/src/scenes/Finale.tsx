import React from 'react';
import {Backdrop, Eyebrow, Reveal, palette} from '../Design';
import {ModelStage} from '../ModelStage';

export const Finale: React.FC = () => <Backdrop section="FIELD EMITTERS" number="07">
  <div style={{position: 'absolute', left: 1140, top: 120, width: 600, height: 810}}><ModelStage kind="post" width={600} height={810} zoom={150} angle={0.5} turn={-0.002} /></div>
  <Reveal style={{position: 'absolute', left: 104, top: 267}}>
    <Eyebrow>BUILD THE PERIMETER</Eyebrow>
    <div style={{fontSize: 168, fontWeight: 700, lineHeight: 0.9}}>FIELD<br /><span style={{color: palette.accent}}>EMITTERS</span></div>
    <div style={{fontSize: 48, lineHeight: 1.2, color: '#bfced8', marginTop: 48}}>Terrain-following fields.<br />Filters you control.</div>
  </Reveal>
</Backdrop>;
