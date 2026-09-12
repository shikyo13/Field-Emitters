import React from 'react';
import {Backdrop, Capture, Eyebrow, Reveal, palette} from '../Design';

export const Rails: React.FC = () => <Backdrop section="RAILS" number="04">
  <Reveal style={{position: 'absolute', left: 104, top: 250, width: 580}}>
    <Eyebrow>FIELD RAIL</Eyebrow>
    <div style={{fontSize: 108, fontWeight: 600, lineHeight: 0.96}}>Seal the<br />openings.</div>
    <div style={{fontSize: 39, lineHeight: 1.3, color: '#b9c8d4', marginTop: 38}}>Mount a rail on any block face.<br />Face another up to 20 blocks away.<br />Add rails side by side to widen it.</div>
    <div style={{fontSize: 31, color: palette.accent, marginTop: 50}}>Doorways, pits and shafts.</div>
  </Reveal>
  <Capture name="rails-doorway" x={720} y={230} width={1080} height={626} crop={[500, 150, 1120, 649]} zoom />
</Backdrop>;
