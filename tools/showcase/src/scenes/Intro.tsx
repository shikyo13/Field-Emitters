import React from 'react';
import {Backdrop, Eyebrow, Reveal, palette} from '../Design';
import {ModelStage} from '../ModelStage';

export const Intro: React.FC = () => <Backdrop section="TERRAIN-FOLLOWING FORCEFIELDS" number="01">
  <div style={{position: 'absolute', left: 1180, top: 110, width: 560, height: 820}}><ModelStage kind="post" width={560} height={820} zoom={152} angle={-0.45} turn={0.0015} /></div>
  <div style={{position: 'absolute', left: 900, top: 470, width: 560, height: 500}}><ModelStage kind="tuner" width={560} height={500} zoom={330} angle={0.28} turn={0.0018} /></div>
  <Reveal style={{position: 'absolute', left: 104, top: 230}}>
    <Eyebrow>POSTS LINK. THE FIELD FOLLOWS.</Eyebrow>
    <div style={{fontSize: 190, fontWeight: 700, lineHeight: 0.86, letterSpacing: -2}}>FIELD<br /><span style={{color: palette.accent, fontSize: 200}}>EMITTERS</span></div>
    <div style={{fontSize: 46, lineHeight: 1.2, marginTop: 38, color: '#bccbd6'}}>Forcefields that follow the ground.<br />You decide what gets through.</div>
  </Reveal>
  <div style={{position: 'absolute', right: 146, top: 858, fontSize: 25, color: palette.violet, letterSpacing: 3}}>EMITTER + TUNER</div>
</Backdrop>;
