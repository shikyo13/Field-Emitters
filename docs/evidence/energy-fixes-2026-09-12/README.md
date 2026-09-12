# Energy fix verification

The two energy defects found in the 1.0.0 review are fixed in the working tree for 1.0.1. This does not change the already-published 1.0.0 artifact.

- A network activates based on having a valid field span, independently of a zero configured cost. An isolated emitter stays idle.
- Network demand, available energy and remaining withdrawal use long totals. Individual storage calls remain bounded to the integer API limit.
- Showcase script lint now recognizes its Node globals. The roadmap correctly lists connected rail impacts as released functionality.

`build` and `npx eslint src scripts` passed. The isolated Minecraft client then ran the existing post verifier (result 1) and rail verifier (result 5), followed by the two previously failing configurations. See [runtime.json](runtime.json).

With demo power disabled and cost zero, the connected rail field reported `Demand:0`, `Energy:0`, `Powered:1b`; the isolated post reported `Powered:0b`. With cost 3, capacity and transfer rate set to one billion, all three loaded post buffers remained powered, and each ten-block outgoing span demanded 135 FE/t. These are direct server-side state observations, not a calculation-only test.

The test client was stopped and its original server configuration restored. The user’s CurseForge instance was not modified. The release matrix is in [release-matrix.md](../../publishing/release-matrix.md); the four additional loader/version ports are not yet built or published.
