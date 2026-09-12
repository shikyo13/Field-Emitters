import React from 'react';
import {Backdrop, Capture, Eyebrow, Reveal, palette} from '../Design';

export const Detection: React.FC = () => <Backdrop section="THE OUTPUT" number="06">
  <Reveal style={{position: 'absolute', left: 104, top: 252, width: 580}}>
    <Eyebrow>DETECTION OUTPUT</Eyebrow>
    <div style={{fontSize: 108, fontWeight: 600, lineHeight: 0.98}}>Know what<br />crossed.</div>
    <div style={{fontSize: 38, lineHeight: 1.32, color: '#b9c8d4', marginTop: 40}}>One redstone pulse per crossing,<br />or a steady signal while touched.<br />Count drops per stack or per item.</div>
    <div style={{marginTop: 60, display: 'flex', gap: 22, alignItems: 'center', fontSize: 33}}><span style={{color: palette.accent}}>Pulse</span><span style={{color: '#526279'}}> / </span><span style={{color: '#b9c8d4'}}>Steady</span></div>
  </Reveal>
  <Capture name="gui-detection" x={720} y={230} width={1080} height={626} crop={[40, 28, 1628, 916]} />
</Backdrop>;
