import React from 'react';
import {AbsoluteFill} from 'remotion';
import {ModelStage} from './ModelStage';
import {display, mono, palette} from './Design';

// Square counterpart to the showcase hero, using the same type and model assets.
export const ProjectIcon: React.FC = () => <AbsoluteFill style={{background: palette.glow, color: palette.ink, fontFamily: display, overflow: 'hidden'}}>
  <div style={{position: 'absolute', top: 34, left: 38, right: 38, height: 1, background: palette.rule}} />
  <div style={{position: 'absolute', bottom: 34, left: 38, right: 38, height: 1, background: palette.rule}} />
  <div style={{position: 'absolute', top: 53, left: 40, fontFamily: mono, fontSize: 16, letterSpacing: 2.5, color: '#b8c3d6'}}>FE / FIELD EMITTERS</div>
  <div style={{position: 'absolute', left: 640, top: 86}}>
    <ModelStage kind="post" width={360} height={880} zoom={158} angle={-0.345} turn={0} pixelRatio={2} />
  </div>
  <div style={{position: 'absolute', left: 372, top: 596}}>
    <ModelStage kind="tuner" width={400} height={380} zoom={250} angle={0.406} turn={0} pixelRatio={2} />
  </div>
  <div style={{position: 'absolute', left: 54, top: 207, fontFamily: mono, fontSize: 17, letterSpacing: 2.4, color: palette.violet}}>POSTS LINK. THE FIELD FOLLOWS.</div>
  <div style={{position: 'absolute', left: 51, top: 273, fontSize: 177, fontWeight: 700, lineHeight: 0.88, letterSpacing: -1}}>FIELD<br /><span style={{color: palette.accent, fontSize: 138}}>EMITTERS</span></div>
  <div style={{position: 'absolute', left: 54, top: 629, fontSize: 34, fontWeight: 600, lineHeight: 1.22, color: '#bccbd6'}}>Forcefields that<br />follow the ground.</div>
  <div style={{position: 'absolute', left: 40, bottom: 55, fontFamily: mono, fontSize: 16, letterSpacing: 2, color: palette.dim}}>TERRAIN-FOLLOWING FORCEFIELDS</div>
  <div style={{position: 'absolute', right: 40, bottom: 57, width: 55, height: 5, background: palette.accent}} />
</AbsoluteFill>;
