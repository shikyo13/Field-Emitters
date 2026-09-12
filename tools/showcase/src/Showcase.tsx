import React from 'react';
import {TransitionSeries, linearTiming} from '@remotion/transitions';
import {fade} from '@remotion/transitions/fade';
import {Intro} from './scenes/Intro';
import {Hardware} from './scenes/Hardware';
import {Perimeter} from './scenes/Perimeter';
import {Rails} from './scenes/Rails';
import {Filters} from './scenes/Filters';
import {Detection} from './scenes/Detection';
import {Finale} from './scenes/Finale';

// Seven scenes, 972 frames of content, six twelve-frame crossfades: 900 frames at 30 fps.
// The mod ships no sound assets, so the showcase is silent.
export const Showcase: React.FC = () => <TransitionSeries>
  <TransitionSeries.Sequence durationInFrames={140} name="Field Emitters"><Intro /></TransitionSeries.Sequence>
  <TransitionSeries.Transition presentation={fade()} timing={linearTiming({durationInFrames: 12})} />
  <TransitionSeries.Sequence durationInFrames={150} name="The hardware"><Hardware /></TransitionSeries.Sequence>
  <TransitionSeries.Transition presentation={fade()} timing={linearTiming({durationInFrames: 12})} />
  <TransitionSeries.Sequence durationInFrames={170} name="Terrain-following perimeter"><Perimeter /></TransitionSeries.Sequence>
  <TransitionSeries.Transition presentation={fade()} timing={linearTiming({durationInFrames: 12})} />
  <TransitionSeries.Sequence durationInFrames={130} name="Rails"><Rails /></TransitionSeries.Sequence>
  <TransitionSeries.Transition presentation={fade()} timing={linearTiming({durationInFrames: 12})} />
  <TransitionSeries.Sequence durationInFrames={150} name="Blocking filters"><Filters /></TransitionSeries.Sequence>
  <TransitionSeries.Transition presentation={fade()} timing={linearTiming({durationInFrames: 12})} />
  <TransitionSeries.Sequence durationInFrames={132} name="Detection output"><Detection /></TransitionSeries.Sequence>
  <TransitionSeries.Transition presentation={fade()} timing={linearTiming({durationInFrames: 12})} />
  <TransitionSeries.Sequence durationInFrames={100} name="Build the perimeter"><Finale /></TransitionSeries.Sequence>
</TransitionSeries>;
