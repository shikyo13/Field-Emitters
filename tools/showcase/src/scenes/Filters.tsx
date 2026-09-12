import React from 'react';
import {Backdrop, Capture, Eyebrow, Reveal, palette} from '../Design';

export const Filters: React.FC = () => <Backdrop section="THE FILTERS" number="05">
  <Reveal style={{position: 'absolute', left: 104, top: 230, width: 580}}>
    <Eyebrow>BLOCKING FILTERS</Eyebrow>
    <div style={{fontSize: 103, fontWeight: 600, lineHeight: 0.98}}>Decide what<br />gets through.</div>
    <div style={{fontSize: 38, lineHeight: 1.3, color: '#b9c8d4', marginTop: 38}}>Hostile, passive, players, drops or nonliving.<br />Narrow by age, type, item, UUID or tag.<br />Per direction. Owner exempt.</div>
    <div style={{fontSize: 31, color: palette.accent, marginTop: 50}}>Each link keeps its own rules.</div>
  </Reveal>
  <Capture name="gui-blocking" x={720} y={230} width={1080} height={626} crop={[40, 28, 1628, 916]} />
</Backdrop>;
